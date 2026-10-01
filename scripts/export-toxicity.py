"""Export the pinned Apache-2.0 toxicity checkpoint with its matching BERT base tokenizer."""
import argparse, hashlib, json, os
from pathlib import Path
os.environ['HF_HUB_DISABLE_TELEMETRY']='1'
import numpy as np
import torch, onnx, onnxruntime as ort
from transformers import AutoModelForSequenceClassification, AutoTokenizer
parser=argparse.ArgumentParser();parser.add_argument('directory',type=Path);args=parser.parse_args()
directory=args.directory.resolve()
assert hashlib.sha256((directory/'model.safetensors').read_bytes()).hexdigest()=='2c272885d24138df70bff1b3cd944a999bd6b41dad33209730aa8ba074f6ad09'
torch.set_num_threads(2)
tokenizer=AutoTokenizer.from_pretrained(directory,local_files_only=True)
model=AutoModelForSequenceClassification.from_pretrained(directory,local_files_only=True,attn_implementation='eager').eval()
assert model.config.id2label==dict(enumerate(['toxic','severe_toxic','obscene','threat','insult','identity_hate']))
assert model.config.vocab_size==len(tokenizer)==30522
class Export(torch.nn.Module):
    def __init__(self,model): super().__init__();self.model=model
    def forward(self,input_ids,attention_mask):
        return self.model(input_ids=input_ids,attention_mask=attention_mask,token_type_ids=torch.zeros_like(input_ids)).logits
sample=tokenizer('You are an idiot.',return_tensors='pt')
with torch.no_grad():
    torch.onnx.export(Export(model).eval(),(sample['input_ids'],sample['attention_mask']),str(directory/'model.onnx'),
        input_names=['input_ids','attention_mask'],output_names=['logits'],opset_version=17,dynamo=False,
        dynamic_axes={'input_ids':{0:'batch',1:'sequence'},'attention_mask':{0:'batch',1:'sequence'},'logits':{0:'batch'}})
onnx.checker.check_model(str(directory/'model.onnx'))
session=ort.InferenceSession(str(directory/'model.onnx'),providers=['CPUExecutionProvider'])
for text in ['Thank you for helping me.','You are an idiot.','I will hurt you.']:
    encoded=tokenizer(text,return_tensors='pt')
    with torch.no_grad(): expected=model(**encoded).logits.numpy()
    actual=session.run(None,{key:encoded[key].numpy() for key in ['input_ids','attention_mask']})[0]
    np.testing.assert_allclose(actual,expected,atol=1e-4,rtol=1e-4)
    print('Parity:',text,torch.sigmoid(torch.from_numpy(actual)).tolist(),flush=True)
spec={'modelId':'unitary/toxic-bert','modelSha256':hashlib.sha256((directory/'model.onnx').read_bytes()).hexdigest(),
      'tokenizerSha256':hashlib.sha256((directory/'tokenizer.json').read_bytes()).hexdigest(),'labels':['toxic','severe_toxic','obscene','threat','insult','identity_hate'],'maxTokens':512}
(directory/'temper-spec.json').write_text(json.dumps(spec,indent=2),encoding='utf-8')
print(json.dumps(spec),flush=True)
