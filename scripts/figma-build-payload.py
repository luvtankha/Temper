"""Produce UTF-8 use_figma payloads from the checked-in design sources.

Library mode is for a fresh file. Screens mode requires the recorded component
IDs in that same file. These scripts are authoring recipes, not idempotent sync.
Use the Figma tools to execute the resulting JavaScript; this does not access Figma.
"""
from pathlib import Path
import argparse
import json

parser=argparse.ArgumentParser()
parser.add_argument('mode',choices=['library','ui','prototype'])
parser.add_argument('output',type=Path)
args=parser.parse_args()
base=Path(__file__).resolve().parents[1]/'design/phase47'
tokens=json.loads((base/'tokens.json').read_text(encoding='utf-8'))
code='const TOKENS='+json.dumps(tokens,ensure_ascii=False)+';\n'
if args.mode=='library':
    assets={p.stem:p.read_text(encoding='utf-8') for p in (base/'avatars').glob('*.svg')}
    code+='const ASSETS='+json.dumps(assets,ensure_ascii=False)+';\n'
    code+=(base/'build-components.js').read_text(encoding='utf-8')
else:
    ledger=json.loads((base/'component-ledger.json').read_text(encoding='utf-8'))
    code+='const LEDGER='+json.dumps(ledger,ensure_ascii=False)+';\n'
    code+="const MODE="+json.dumps(args.mode)+";const PAGE_ID="+json.dumps('0:1' if args.mode=='ui' else '3:5')+';\n'
    code+='const PAGE_NAME='+json.dumps('01 – Temper UI' if args.mode=='ui' else '03 – Prototype',ensure_ascii=False)+';\n'
    code+=(base/'build-screens.js').read_text(encoding='utf-8')
args.output.parent.mkdir(parents=True,exist_ok=True)
args.output.write_text(code,encoding='utf-8')
