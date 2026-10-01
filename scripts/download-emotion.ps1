param([Parameter(Mandatory=$true)][string]$Destination)
$ErrorActionPreference='Stop'
$modelDirectory=[System.IO.Path]::GetFullPath($Destination)
New-Item -ItemType Directory -Path $modelDirectory -Force | Out-Null
$hashes=@{
    'model.onnx'='3bf605adfa0e59ae36723f1136d37865acb0ec4ded7831f7f7f5231eb032d60a'
    'tokenizer.json'='63735ef382776e869c0ee50f8e999ab19111bb794f8a451559e611077dfe7f25'
    'config.json'='60db0b3d640dedb02f9a033578ddecb78fa1e72ac67dd9d391606cba0e6cbcf1'
    'tokenizer_config.json'='25022bd6d2bd8913bf238a14d4a97eb8a819f4b5bf50596bd31b8c1a43dd5656'
    'special_tokens_map.json'='06e405a36dfe4b9604f484f6a1e619af1a7f7d09e34a8555eb0b77b66318067f'
    'vocab.json'='ed19656ea1707df69134c4af35c8ceda2cc9860bf2c3495026153a133670ab5e'
    'merges.txt'='1ce1664773c50f3e0cc8842619a93edc4624525b728b188a9e0be33b7726adc5'
}
foreach($entry in $hashes.GetEnumerator()) {
    $target=Join-Path $modelDirectory $entry.Key
    if ((Test-Path -LiteralPath $target) -and (Get-FileHash -LiteralPath $target).Hash.ToLowerInvariant() -eq $entry.Value) {continue}
    Invoke-WebRequest -Uri ('https://huggingface.co/SamLowe/roberta-base-go_emotions-onnx/resolve/90ee0c1c4796d370e68968687b8ba51fc11224f4/onnx/'+$entry.Key) -OutFile $target
    if ((Get-FileHash -LiteralPath $target).Hash.ToLowerInvariant() -ne $entry.Value) {throw ('Checksum mismatch: '+$entry.Key)}
}
$notice=Join-Path $modelDirectory 'UPSTREAM-README.md'
if (!(Test-Path -LiteralPath $notice)) {Invoke-WebRequest -Uri 'https://huggingface.co/SamLowe/roberta-base-go_emotions-onnx/resolve/90ee0c1c4796d370e68968687b8ba51fc11224f4/README.md' -OutFile $notice}
Write-Output ('Verified local emotion artifacts: '+$modelDirectory)
