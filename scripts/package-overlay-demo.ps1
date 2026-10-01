$ErrorActionPreference = 'Stop'
$temperRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$temperApk = Join-Path $temperRoot 'android\app\build\outputs\apk\debug\app-debug.apk'
$temperJar = Join-Path $temperRoot 'backend\target\cerebro-0.0.1-SNAPSHOT.jar'
if (!(Test-Path -LiteralPath $temperApk) -or !(Test-Path -LiteralPath $temperJar)) { throw 'Build and verify Android and backend first.' }
$temperDist = Join-Path $temperRoot 'dist'
New-Item -ItemType Directory -Path $temperDist -Force | Out-Null
$temperStage = Join-Path $temperRoot ('temp\package-' + [Guid]::NewGuid().ToString('N') + '\TEMPER-demo-0.39.0')
New-Item -ItemType Directory -Path $temperStage -Force | Out-Null
function Copy-TemperFile([string]$Source,[string]$Relative) {
    $temperTarget = Join-Path $temperStage $Relative
    New-Item -ItemType Directory -Path (Split-Path -Parent $temperTarget) -Force | Out-Null
    Copy-Item -LiteralPath $Source -Destination $temperTarget
}
Copy-TemperFile $temperApk 'TEMPER.apk'
Copy-TemperFile $temperJar 'backend\target\cerebro-0.0.1-SNAPSHOT.jar'
Copy-TemperFile (Join-Path $temperRoot 'docs\demo\ANDROID-USB-DEMO.md') 'README.md'
foreach ($temperScript in @('start-overlay-backend.ps1','configure-overlay-usb.ps1','evaluate-overlay-spectrum.ps1','download-foundation.ps1','download-sentiment.ps1','download-emotion.ps1','download-sarcasm.ps1','download-toxicity.ps1','export-sentiment.py','export-sarcasm.py','export-toxicity.py','model-export-requirements.txt')) {
    Copy-TemperFile (Join-Path $PSScriptRoot $temperScript) ('scripts\' + $temperScript)
}
foreach ($temperDoc in @('android-privacy.md','live-overlay-pipeline.md','whatsapp-adapter.md','android-character.md','adapter-extension.md')) {
    Copy-TemperFile (Join-Path $temperRoot ('docs\architecture\' + $temperDoc)) ('docs\architecture\' + $temperDoc)
}
foreach ($temperCard in @('foundation.md','sentiment.md','emotion.md','sarcasm.md','toxicity.md')) {
    Copy-TemperFile (Join-Path $temperRoot ('docs\model-cards\' + $temperCard)) ('docs\model-cards\' + $temperCard)
}
Copy-TemperFile (Join-Path $temperRoot 'models\sentiment\ATTRIBUTION.md') 'models\sentiment\ATTRIBUTION.md'
Copy-TemperFile (Join-Path $temperRoot 'models\foundation\manifest.json') 'models\foundation\manifest.json'
foreach ($temperSpec in @('sentiment.json','emotion.json','sarcasm.json','toxicity.json')) {
    $temperRelative = 'backend\src\main\resources\models\' + $temperSpec
    Copy-TemperFile (Join-Path $temperRoot $temperRelative) $temperRelative
}
foreach ($temperPhase in @(38,39,40)) {
    $temperRelative = 'docs\phases\PHASE-' + $temperPhase + '-HANDOFF.md'
    Copy-TemperFile (Join-Path $temperRoot $temperRelative) $temperRelative
}
Copy-TemperFile (Join-Path $temperRoot 'backend\src\test\resources\overlay-chat-styles.json') 'backend\src\test\resources\overlay-chat-styles.json'
Copy-TemperFile (Join-Path $temperRoot 'docs\demo\EMOTIONAL-SPECTRUM-CHECK.md') 'docs\demo\EMOTIONAL-SPECTRUM-CHECK.md'
$temperManifest = foreach ($temperFile in Get-ChildItem -LiteralPath $temperStage -File -Recurse) {
    '{0}  {1}' -f (Get-FileHash -LiteralPath $temperFile.FullName -Algorithm SHA256).Hash.ToLowerInvariant(),([IO.Path]::GetRelativePath($temperStage,$temperFile.FullName).Replace('\','/'))
}
$temperManifest | Set-Content -LiteralPath (Join-Path $temperStage 'SHA256SUMS.txt')
$temperZip = Join-Path $temperDist 'TEMPER-demo-0.39.0.zip'
Compress-Archive -LiteralPath $temperStage -DestinationPath $temperZip -Force
$temperStandaloneApk = Join-Path $temperDist 'TEMPER-0.39.0.apk'
Copy-Item -LiteralPath $temperApk -Destination $temperStandaloneApk -Force
$temperChecksums = foreach ($temperArtifact in @($temperStandaloneApk,$temperZip)) {
    '{0}  {1}' -f (Get-FileHash -LiteralPath $temperArtifact -Algorithm SHA256).Hash.ToLowerInvariant(),([IO.Path]::GetFileName($temperArtifact))
}
$temperChecksums | Set-Content -LiteralPath (Join-Path $temperDist 'SHA256SUMS.txt')
Write-Output "Packaged $temperStandaloneApk and $temperZip. Models and private runtime data excluded."
