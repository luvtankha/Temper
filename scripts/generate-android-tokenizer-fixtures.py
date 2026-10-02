"""Generate public dummy references with the independent Hugging Face tokenizer."""
import argparse
import hashlib
import json
from pathlib import Path
from tokenizers import Tokenizer

parser = argparse.ArgumentParser()
parser.add_argument('--tokenizer', type=Path, required=True)
arguments = parser.parse_args()
if hashlib.sha256(arguments.tokenizer.read_bytes()).hexdigest() != '63735ef382776e869c0ee50f8e999ab19111bb794f8a451559e611077dfe7f25':
    raise ValueError('Supply the pinned GoEmotions tokenizer')
root = Path(__file__).resolve().parent.parent
tokenizer = Tokenizer.from_file(str(arguments.tokenizer))
tokenizer.enable_truncation(max_length=128)
tokenizer.no_padding()
styles = json.loads((root / 'backend/src/test/resources/overlay-chat-styles.json').read_text(encoding='utf-8-sig'))
texts = []
def collect(value):
    if isinstance(value, dict):
        for key, item in value.items():
            if key == 'text' and isinstance(item, str):
                texts.append(item)
            else:
                collect(item)
    elif isinstance(value, list):
        for item in value:
            collect(item)
collect(styles)
texts += ["Can't wait! WE'RE READY 😄", '  Mixed\twhitespace\n\nworks.  ', 'नमस्ते, kaise ho?',
          '<s>real text</s> <mask> <unk> <pad>', "I'M HAPPY but I'm not sure.", 'hello ' + 'very ' * 180,
          'emoji 👨‍👩‍👧‍👦 and café résumé', 'ok', 'I got the job!', 'I am furious with you!']
texts += ['a' * 1000, 'abcde' * 200, '🤯' * 200, 'न' * 900, 'é' * 1000, ('a <mask> ' * 100), ' ' * 500 + 'x', 'x' + '\t' * 998 + 'y']
fixtures = [dict(text=text, ids=tokenizer.encode(text).ids) for text in dict.fromkeys(texts)]
for target in ('android/app/src/test/resources/roberta-reference.json', 'android/app/src/androidTest/assets/roberta-reference.json'):
    output = root / target
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(fixtures, ensure_ascii=False, indent=2), encoding='utf-8')
print(f'Generated {len(fixtures)} fictional tokenizer cases')
