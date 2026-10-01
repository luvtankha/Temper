param([Parameter(Mandatory=$true)][string]$Destination,[Parameter(Mandatory=$true)][string]$Python)
$ErrorActionPreference='Stop'
$modelDirectory=[System.IO.Path]::GetFullPath($Destination)
New-Item -ItemType Directory -Force -Path $modelDirectory | Out-Null
$revision='3216a57f2a0d9c45a2e6c20157c20c49fb4bf9c7'
foreach ($fileName in @('config.json','vocab.json','merges.txt','README.md','pytorch_model.bin')) {
    $target=Join-Path $modelDirectory $fileName
    if (!(Test-Path -LiteralPath $target)) { Invoke-WebRequest -Uri ('https://huggingface.co/cardiffnlp/twitter-roberta-base-sentiment-latest/resolve/'+$revision+'/'+$fileName) -OutFile $target }
}
if ((Get-FileHash -LiteralPath (Join-Path $modelDirectory 'pytorch_model.bin')).Hash.ToLowerInvariant() -ne '4d24a3e32a88ed1c4e5b789fc6644e2e767500554e954b27dccf52a8e762cbae') {throw 'Upstream checkpoint checksum mismatch'}
$metadataHashes=@{
    'config.json'='d2fba19997da698157196ba16f5fcb30a97a7551cef6845a0f3d743ee19c6129'
    'vocab.json'='ed19656ea1707df69134c4af35c8ceda2cc9860bf2c3495026153a133670ab5e'
    'merges.txt'='1ce1664773c50f3e0cc8842619a93edc4624525b728b188a9e0be33b7726adc5'
}
foreach ($entry in $metadataHashes.GetEnumerator()) {if ((Get-FileHash -LiteralPath (Join-Path $modelDirectory $entry.Key)).Hash.ToLowerInvariant() -ne $entry.Value) {throw ('Metadata checksum mismatch: '+$entry.Key)}}
$spec=Get-Content -LiteralPath (Join-Path $PSScriptRoot '../backend/src/main/resources/models/sentiment.json') -Raw | ConvertFrom-Json
$modelPath=Join-Path $modelDirectory 'model.onnx'
$tokenizerPath=Join-Path $modelDirectory 'tokenizer.json'
$valid=(Test-Path -LiteralPath $modelPath) -and (Test-Path -LiteralPath $tokenizerPath)
if ($valid) {$valid=((Get-FileHash -LiteralPath $modelPath).Hash.ToLowerInvariant() -eq $spec.modelSha256) -and ((Get-FileHash -LiteralPath $tokenizerPath).Hash.ToLowerInvariant() -eq $spec.tokenizerSha256)}
if (!$valid) {
    & $Python (Join-Path $PSScriptRoot 'export-sentiment.py') $modelDirectory
    if ($LASTEXITCODE -ne 0) {throw 'Sentiment export failed'}
}
if ((Get-FileHash -LiteralPath $modelPath).Hash.ToLowerInvariant() -ne $spec.modelSha256 -or (Get-FileHash -LiteralPath $tokenizerPath).Hash.ToLowerInvariant() -ne $spec.tokenizerSha256) {throw 'Export checksum differs from the verified runtime specification; verify parity before updating metadata'}
Copy-Item -LiteralPath (Join-Path $PSScriptRoot '../models/sentiment/ATTRIBUTION.md') -Destination (Join-Path $modelDirectory 'ATTRIBUTION.md')
Write-Output ('Verified local sentiment artifacts: '+$modelDirectory)
