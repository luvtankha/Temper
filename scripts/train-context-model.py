"""Reproducible fictional context-head pilot. Never reads feedback/user data."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import time

import numpy as np
import onnxruntime as ort
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, f1_score, recall_score
from transformers import RobertaTokenizerFast

ROOT = Path(__file__).resolve().parents[1]
QUOTES = re.compile(r'"[^"\n]*"|(?<!\w)\x27[^\x27\n]+\x27(?!\w)')


def normalize(text):
    text = QUOTES.sub(' ', text.lower().replace('\u2019', "'").replace('\u201c', '"').replace('\u201d', '"'))
    return ' ' + re.sub(r'[^a-z0-9]+', ' ', text).strip() + ' '


def cues(text, groups):
    text = normalize(text)
    return np.array([float(any(' ' + phrase + ' ' in text for phrase in phrases)) for phrases in groups.values()])


def features(turns, current, previous, groups):
    remote = [i for i, t in enumerate(turns) if t['role'] == 'REMOTE']
    last = remote[-1]
    prior = remote[-2] if len(remote) > 1 else -1
    intervening = [t['text'] for t in turns[prior + 1:last] if t['role'] == 'LOCAL']
    older = [cues(turns[i]['text'], groups) for i in remote[:-2]]
    now = cues(turns[last]['text'], groups)
    before = cues(turns[prior]['text'], groups) if prior >= 0 else np.zeros(len(groups))
    local = np.maximum.reduce([cues(t, groups) for t in intervening]) if intervening else np.zeros(len(groups))
    history = np.mean(older, axis=0) if older else np.zeros(len(groups))
    return np.concatenate([now, before, local, history, now-before, current, previous])


def baseline(current, prior):
    now, before = max(current[2], current[5], current[6]), max(prior[2], prior[5], prior[6])
    if now >= .65 and now-before >= .15:
        return 'TENSION_RISING'
    if before >= .5 and before-now >= .2:
        return 'DEESCALATING'
    if now < .25 and before < .25:
        return 'STABLE'
    if now >= .8 and before >= .6:
        return 'TENSION_RISING'
    return 'UNCERTAIN'


def evaluate(rows, labels, predictions):
    truth = [r['direction'] for r in rows]
    return {'count': len(rows), 'accuracy': accuracy_score(truth, predictions),
            'macroF1': f1_score(truth, predictions, labels=labels, average='macro', zero_division=0),
            'recall': dict(zip(labels, recall_score(truth, predictions, labels=labels, average=None, zero_division=0).tolist())),
            'abstentions': sum(p == 'UNCERTAIN' for p in predictions)}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--model', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--install', action='store_true')
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    data_path = ROOT/'models/context/scenarios.txt'
    spec_path = ROOT/'models/context/features.json'
    spec = json.loads(spec_path.read_text(encoding='utf-8'))
    groups = spec['groups']
    rows = []
    holdout_path = ROOT/'models/context/holdout-v2.txt'
    original = [line for line in data_path.read_text(encoding='utf-8-sig').splitlines() if '|test|' not in line]
    for line in original + holdout_path.read_text(encoding='utf-8-sig').splitlines():
        if not line.strip():
            continue
        name, split, language, direction, *texts = line.split('|')
        assert 3 <= len(texts) <= 8 and all(0 < len(t) <= 1000 for t in texts)
        rows.append({'id': name, 'split': split, 'language': language, 'direction': direction,
                     'turns': [{'role': 'REMOTE' if i % 2 == 0 else 'LOCAL', 'text': t} for i, t in enumerate(texts)]})
    assert len({r['id'] for r in rows}) == len(rows)
    # Paired counterfactual scenarios and every exact conversation stay within one split.
    families, duplicates = {}, {}
    for row in rows:
        family = re.sub(r'-[ab](?=-v2$|$)', '', row['id'])
        assert families.setdefault(family, row['split']) == row['split']
        digest = hashlib.sha256(json.dumps(row['turns']).encode()).hexdigest()
        assert duplicates.setdefault(digest, row['split']) == row['split']
    asset = ROOT/'android/app/src/main/assets/emotion'
    tokenizer = RobertaTokenizerFast(vocab_file=str(asset/'vocab.json'), merges_file=str(asset/'merges.txt'), model_max_length=128)
    options = ort.SessionOptions()
    options.intra_op_num_threads, options.inter_op_num_threads = 2, 1
    session = ort.InferenceSession(str(args.model), sess_options=options, providers=['CPUExecutionProvider'])
    cache = {}

    def spectrum(text):
        text = re.sub(r'https?://\S+', 'http', text, flags=re.I)
        text = re.sub(r'(?<!\w)@\w+', '@user', text)
        if text not in cache:
            encoded = tokenizer(text, truncation=True, max_length=128, return_tensors='np')
            logits = session.run(None, {name: encoded[name].astype(np.int64) for name in ('input_ids', 'attention_mask')})[0][0]
            p = 1/(1+np.exp(-logits))
            cache[text] = p[[27,17,14,6,25,3,2,26]].copy()
            cache[text][2] = max(p[14], p[19])
        return cache[text]

    start = time.perf_counter()
    for row in rows:
        remote = [t['text'] for t in row['turns'] if t['role'] == 'REMOTE']
        current, prior = spectrum(remote[-1]), spectrum(remote[-2])
        row['current'], row['previous'] = current.tolist(), prior.tolist()
        row['features'] = features(row['turns'], current, prior, groups).tolist()
        row['baseline'] = baseline(current, prior)
    train = [r for r in rows if r['split'] == 'train']
    validation = [r for r in rows if r['split'] == 'validation']
    test = [r for r in rows if r['split'] == 'test']
    labels = sorted({r['direction'] for r in train})
    assert len(test) >= 28
    minimum, margin = .45, .12
    def deployed_predictions(model, data):
        predictions = []
        for probabilities in model.predict_proba([r['features'] for r in data]):
            order = np.argsort(probabilities)
            predictions.append(str(model.classes_[order[-1]]) if probabilities[order[-1]] >= minimum and probabilities[order[-1]]-probabilities[order[-2]] >= margin else 'UNCERTAIN')
        return predictions
    candidates = []
    for regularization in [.25, 1., 4.]:
        model = LogisticRegression(C=regularization, max_iter=2000, class_weight='balanced', random_state=46)
        model.fit([r['features'] for r in train], [r['direction'] for r in train])
        score = f1_score([r['direction'] for r in validation], deployed_predictions(model, validation), labels=labels, average='macro', zero_division=0)
        candidates.append((score, -regularization, model))
    score, _, model = max(candidates, key=lambda item: item[:2])
    # Fixed abstention policy, selected before looking at held-out results. These
    # softmax values are selection scores, not calibrated emotional confidence.
    minimum, margin = .45, .12
    for row in rows:
        probabilities = model.predict_proba([row['features']])[0]
        order = np.argsort(probabilities)
        row['candidate'] = str(model.classes_[order[-1]]) if probabilities[order[-1]] >= minimum and probabilities[order[-1]]-probabilities[order[-2]] >= margin else 'UNCERTAIN'
        row['probabilities'] = probabilities.tolist()
    old = evaluate(test, labels, [r['baseline'] for r in test])
    new = evaluate(test, labels, [r['candidate'] for r in test])
    by_language = {}
    for language in ['en', 'hi']:
        subset = [r for r in test if r['language'] == language]
        by_language[language] = {key: evaluate(subset, labels, [r[key] for r in subset]) for key in ['baseline', 'candidate']}
    # Pilot shipping gate only. This does NOT approve a customer-trained model or
    # bypass the independent human-reviewed promotion gates in train-feedback-model.
    approved = (new['macroF1'] >= old['macroF1'] + .1
                and all(new['recall'][label] >= old['recall'][label] for label in labels)
                and all(v['candidate']['macroF1'] >= v['baseline']['macroF1'] for v in by_language.values()))
    candidate = {'schema': 1, 'kind': 'fictional-context-pilot', 'approvedPilot': bool(approved),
                 'groups': list(groups.values()), 'groupNames': list(groups), 'labels': model.classes_.tolist(),
                 'weights': model.coef_.tolist(), 'bias': model.intercept_.tolist(), 'minimum': minimum, 'margin': margin,
                 'datasetSha256': hashlib.sha256(data_path.read_bytes()+holdout_path.read_bytes()).hexdigest(),
                 'featureSpecSha256': hashlib.sha256(spec_path.read_bytes()).hexdigest(),
                 'baseModelSha256': hashlib.sha256(args.model.read_bytes()).hexdigest()}
    report = {'scope': 'Authored fictional scenarios only; NOT real-world accuracy or independent human evaluation.',
              'evaluationRevision': 2, 'priorFailure': 'v1 selected C before abstention; settings now selected by deployed validation predictions. Original test excluded; fresh holdout-v2 used once.',
              'training': len(train), 'validation': len(validation), 'heldOut': len(test),
              'regularization': model.C, 'validationMacroF1WithAbstention': score,
              'baseline': old, 'candidate': new, 'byLanguage': by_language,
              'approvedPilot': bool(approved), 'elapsedSeconds': time.perf_counter()-start,
              'datasetSha256': candidate['datasetSha256'], 'featureSpecSha256': candidate['featureSpecSha256'],
              'limitations': ['Small synthetic sample; shared author/cue vocabulary across scenarios.',
                              'Only bounded visible context; no claims about intentions or hidden history.',
                              'Frozen English GoEmotions base; no new emotion-bar calibration or accuracy claim.',
                              'Hinglish is Roman-script only; novel idioms and indirect sarcasm may be missed.'],
              'heldOutCases': [{k: r[k] for k in ['id','language','direction','baseline','candidate']} for r in test]}
    (args.output/'context-model.json').write_text(json.dumps(candidate, separators=(',', ':'))+'\n')
    (args.output/'evaluation.json').write_text(json.dumps(report, indent=2)+'\n')
    (args.output/'parity-fixtures.json').write_text(json.dumps(rows, indent=2)+'\n')
    if args.install:
        if not approved:
            raise SystemExit('Pilot gate failed: production asset left unchanged. Inspect evaluation.json.')
        (asset/'context-model.json').write_text(json.dumps(candidate, separators=(',', ':'))+'\n')
        (ROOT/'models/context/evaluation.json').write_text(json.dumps(report, indent=2)+'\n')
        (ROOT/'android/app/src/test/resources/context-parity.json').write_text(json.dumps(rows, separators=(',', ':'))+'\n')
        (ROOT/'android/app/src/androidTest/assets/complex-context.json').write_text(json.dumps(test, separators=(',', ':'))+'\n')
    print(json.dumps({k: report[k] for k in ['training','validation','heldOut','baseline','candidate','approvedPilot','elapsedSeconds']}, indent=2))


if __name__ == '__main__':
    main()
