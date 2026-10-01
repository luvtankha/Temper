"""Export the pinned Apache-2.0 sarcasm checkpoint with its documented BERT base tokenizer."""
import argparse, hashlib, json, os
from pathlib import Path
os.environ['HF_HUB_DISABLE_TELEMETRY']='1'
import numpy as np
import torch, onnx, onnxruntime as ort
from transformers import AutoModelForSequenceClassification, AutoTokenizer
parser=argparse.ArgumentParser();parser.add_argument('directory',type=Path);args=parser.parse_args()
directory=args.directory.resolve()
assert hashlib.sha256((directory/'model.safetensors').read_bytes()).hexdigest()=='628b5e74d77d869363eb63aa6cf8c7bfc864d0d80da096959da0b298109d135f'
torch.set_num_threads(2)
tokenizer=AutoTokenizer.from_pretrained(directory,local_files_only=True)
model=AutoModelForSequenceClassification.from_pretrained(directory,local_files_only=True,attn_implementation='eager').eval()
assert model.config.id2label=={0:'No sarcasm',1:'Sarcasm'}
assert model.config.vocab_size==len(tokenizer)==30522
class Export(torch.nn.Module):
    def __init__(self,model): super().__init__();self.model=model
    def forward(self,input_ids,attention_mask):
        return self.model(input_ids=input_ids,attention_mask=attention_mask,token_type_ids=torch.zeros_like(input_ids)).logits
sample=tokenizer('Oh great, another traffic jam. Just what I needed!',return_tensors='pt')
with torch.no_grad():
    torch.onnx.export(Export(model).eval(),(sample['input_ids'],sample['attention_mask']),str(directory/'model.onnx'),
        input_names=['input_ids','attention_mask'],output_names=['logits'],opset_version=17,dynamo=False,
        dynamic_axes={'input_ids':{0:'batch',1:'sequence'},'attention_mask':{0:'batch',1:'sequence'},'logits':{0:'batch'}})
onnx.checker.check_model(str(directory/'model.onnx'))
session=ort.InferenceSession(str(directory/'model.onnx'),providers=['CPUExecutionProvider'])
for text in ['Oh great, another traffic jam. Just what I needed!','The train arrives at six o clock.','I love waiting for hours in a queue. It is my favorite activity.','I am upset because the train is late.']:
    encoded=tokenizer(text,return_tensors='pt')
    with torch.no_grad(): expected=model(**encoded).logits.numpy()
    actual=session.run(None,{key:encoded[key].numpy() for key in ['input_ids','attention_mask']})[0]
    np.testing.assert_allclose(actual,expected,atol=1e-4,rtol=1e-4)
    print('Parity:',text,torch.softmax(torch.from_numpy(actual),dim=-1).tolist(),flush=True)
spec={'modelId':'dima806/sarcasm-detection-distilbert','modelSha256':hashlib.sha256((directory/'model.onnx').read_bytes()).hexdigest(),
      'tokenizerSha256':hashlib.sha256((directory/'tokenizer.json').read_bytes()).hexdigest(),'labels':['notSarcastic','sarcasm'],'maxTokens':512}
(directory/'temper-spec.json').write_text(json.dumps(spec,indent=2),encoding='utf-8')
print(json.dumps(spec),flush=True)
