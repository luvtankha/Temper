"""Compile the pinned public RoBERTa tables; no model semantics or chat data change."""
from pathlib import Path
import hashlib
import json
import struct

asset = Path(__file__).resolve().parents[1]/'android/app/src/main/assets/emotion'
vocab_bytes, merge_bytes = (asset/'vocab.json').read_bytes(), (asset/'merges.txt').read_bytes()
assert hashlib.sha256(vocab_bytes).hexdigest() == 'ed19656ea1707df69134c4af35c8ceda2cc9860bf2c3495026153a133670ab5e'
assert hashlib.sha256(merge_bytes).hexdigest() == '1ce1664773c50f3e0cc8842619a93edc4624525b728b188a9e0be33b7726adc5'
vocab = json.loads(vocab_bytes)
rows = []
for line in merge_bytes.decode().splitlines():
    if line.startswith('#') or not line.strip():
        continue
    left, right = line.split(' ')
    rows.append(((vocab[left] << 32) | vocab[right], len(rows), vocab[left+right]))
rows.sort()
assert len({row[0] for row in rows}) == len(rows)
data = bytearray(struct.pack('>ii', 0x54425031, len(rows)))
extra = 0
for i in range(256):
    visible = 33 <= i <= 126 or 161 <= i <= 172 or i >= 174
    char = chr(i if visible else 256+extra)
    if not visible:
        extra += 1
    data.extend(struct.pack('>i', vocab[char]))
for token in ['<s>', '</s>', '<unk>', '<pad>', '<mask>']:
    data.extend(struct.pack('>i', vocab[token]))
for key, rank, token in rows:
    data.extend(struct.pack('>qii', key, rank, token))
(asset/'tokenizer.bin').write_bytes(data)
print(json.dumps({'bytes': len(data), 'merges': len(rows), 'sha256': hashlib.sha256(data).hexdigest()}))
