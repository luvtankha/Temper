param([Parameter(Mandatory=$true)][string]$Destination,[Parameter(Mandatory=$true)][string]$Python)
$ErrorActionPreference='Stop'
$modelDirectory=[System.IO.Path]::GetFullPath($Destination)
New-Item -ItemType Directory -Path $modelDirectory -Force | Out-Null
$sources=@{
    'model.safetensors'=@('dima806/sarcasm-detection-distilbert','8f0e2429ce5ba0cc976ded7dd2b359ca9d1a66ab','628b5e74d77d869363eb63aa6cf8c7bfc864d0d80da096959da0b298109d135f')
    'config.json'=@('dima806/sarcasm-detection-distilbert','8f0e2429ce5ba0cc976ded7dd2b359ca9d1a66ab','2fe4c550d12b9219f30aab8c300a1652d011a7967dc0ed2950e30e2e83ea75b6')
    'tokenizer.json'=@('google-bert/bert-base-uncased','86b5e0934494bd15c9632b12f734a8a67f723594','ce64fce797c24f68df90b40a3f74f579b336a493db14bd583fd520ea0d8c9a98')
    'vocab.txt'=@('google-bert/bert-base-uncased','86b5e0934494bd15c9632b12f734a8a67f723594','07eced375cec144d27c900241f3e339478dec958f92fddbc551f295c992038a3')
    'tokenizer_config.json'=@('google-bert/bert-base-uncased','86b5e0934494bd15c9632b12f734a8a67f723594','a025160ef0431f1a392f6f050c1310f4c5d9fb6f275932dbccba73c4d214bf10')
}
foreach($entry in $sources.GetEnumerator()) {
    $target=Join-Path $modelDirectory $entry.Key
    if ((Test-Path -LiteralPath $target) -and (Get-FileHash -LiteralPath $target).Hash.ToLowerInvariant() -eq $entry.Value[2]) {continue}
    Invoke-WebRequest -Uri ('https://huggingface.co/'+$entry.Value[0]+'/resolve/'+$entry.Value[1]+'/'+$entry.Key) -OutFile $target
    if ((Get-FileHash -LiteralPath $target).Hash.ToLowerInvariant() -ne $entry.Value[2]) {throw ('Checksum mismatch: '+$entry.Key)}
}
$spec=Get-Content -LiteralPath (Join-Path $PSScriptRoot '../backend/src/main/resources/models/sarcasm.json') -Raw | ConvertFrom-Json
$modelPath=Join-Path $modelDirectory 'model.onnx'
if (!(Test-Path -LiteralPath $modelPath) -or (Get-FileHash -LiteralPath $modelPath).Hash.ToLowerInvariant() -ne $spec.modelSha256) {
    & $Python (Join-Path $PSScriptRoot 'export-sarcasm.py') $modelDirectory
    if ($LASTEXITCODE -ne 0) {throw 'Sarcasm export failed'}
}
if ((Get-FileHash -LiteralPath $modelPath).Hash.ToLowerInvariant() -ne $spec.modelSha256) {throw 'Export checksum differs from verified specification'}
foreach($notice in @{'UPSTREAM-README.md'='https://huggingface.co/dima806/sarcasm-detection-distilbert/resolve/8f0e2429ce5ba0cc976ded7dd2b359ca9d1a66ab/README.md'; 'LICENSE-Apache-2.0.txt'='https://www.apache.org/licenses/LICENSE-2.0.txt'}.GetEnumerator()) {
    $target=Join-Path $modelDirectory $notice.Key
    if (!(Test-Path -LiteralPath $target)) {Invoke-WebRequest -Uri $notice.Value -OutFile $target}
}
Write-Output ('Verified local sarcasm artifacts: '+$modelDirectory)
