param([Parameter(Mandatory=$true)][string]$Destination,[Parameter(Mandatory=$true)][string]$Python)
$ErrorActionPreference='Stop'
$modelDirectory=[System.IO.Path]::GetFullPath($Destination)
New-Item -ItemType Directory -Path $modelDirectory -Force | Out-Null
$sources=@{
    'model.safetensors'=@('unitary/toxic-bert','4d6c22e74ba2fdd26bc4f7238f50766b045a0d94','2c272885d24138df70bff1b3cd944a999bd6b41dad33209730aa8ba074f6ad09')
    'config.json'=@('unitary/toxic-bert','4d6c22e74ba2fdd26bc4f7238f50766b045a0d94','a45f41c1f62793e4d6468d85c40e06f1e59343c7910600c60d465b74dbb3ccfb')
    'tokenizer.json'=@('google-bert/bert-base-uncased','86b5e0934494bd15c9632b12f734a8a67f723594','ce64fce797c24f68df90b40a3f74f579b336a493db14bd583fd520ea0d8c9a98')
    'vocab.txt'=@('google-bert/bert-base-uncased','86b5e0934494bd15c9632b12f734a8a67f723594','07eced375cec144d27c900241f3e339478dec958f92fddbc551f295c992038a3')
    'tokenizer_config.json'=@('unitary/toxic-bert','4d6c22e74ba2fdd26bc4f7238f50766b045a0d94','8823984edea41294b27aac79047c15d33909ac913ecdbcbd30e139326226d29a')
}
foreach($entry in $sources.GetEnumerator()) {
    $target=Join-Path $modelDirectory $entry.Key
    if ((Test-Path -LiteralPath $target) -and (Get-FileHash -LiteralPath $target).Hash.ToLowerInvariant() -eq $entry.Value[2]) {continue}
    Invoke-WebRequest -Uri ('https://huggingface.co/'+$entry.Value[0]+'/resolve/'+$entry.Value[1]+'/'+$entry.Key) -OutFile $target
    if ((Get-FileHash -LiteralPath $target).Hash.ToLowerInvariant() -ne $entry.Value[2]) {throw ('Checksum mismatch: '+$entry.Key)}
}
$spec=Get-Content -LiteralPath (Join-Path $PSScriptRoot '../backend/src/main/resources/models/toxicity.json') -Raw | ConvertFrom-Json
$modelPath=Join-Path $modelDirectory 'model.onnx'
if (!(Test-Path -LiteralPath $modelPath) -or (Get-FileHash -LiteralPath $modelPath).Hash.ToLowerInvariant() -ne $spec.modelSha256) {
    & $Python (Join-Path $PSScriptRoot 'export-toxicity.py') $modelDirectory
    if ($LASTEXITCODE -ne 0) {throw 'Toxicity export failed'}
}
if ((Get-FileHash -LiteralPath $modelPath).Hash.ToLowerInvariant() -ne $spec.modelSha256) {throw 'Export checksum differs from verified specification'}
foreach($notice in @{'UPSTREAM-README.md'='https://huggingface.co/unitary/toxic-bert/resolve/4d6c22e74ba2fdd26bc4f7238f50766b045a0d94/README.md'; 'LICENSE-Apache-2.0.txt'='https://www.apache.org/licenses/LICENSE-2.0.txt'}.GetEnumerator()) {
    $target=Join-Path $modelDirectory $notice.Key
    if (!(Test-Path -LiteralPath $target)) {Invoke-WebRequest -Uri $notice.Value -OutFile $target}
}
Write-Output ('Verified local toxicity artifacts: '+$modelDirectory)
