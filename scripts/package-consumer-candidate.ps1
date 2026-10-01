$ErrorActionPreference = 'Stop'
$temperRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$temperApk = Join-Path $temperRoot 'android\app\build\outputs\apk\debug\app-debug.apk'
$temperAab = Join-Path $temperRoot 'android\app\build\outputs\bundle\release\app-release.aab'
$temperJar = Join-Path $temperRoot 'backend\target\cerebro-0.0.1-SNAPSHOT.jar'
foreach ($temperArtifact in @($temperApk, $temperAab, $temperJar)) {
    if (!(Test-Path -LiteralPath $temperArtifact -PathType Leaf)) { throw 'Build and verify Android and backend before packaging.' }
}
$temperMetadata = Get-Content -LiteralPath (Join-Path (Split-Path $temperApk) 'output-metadata.json') -Raw | ConvertFrom-Json
$temperVersion=$temperMetadata.elements[0].versionName
if ($temperMetadata.applicationId -ne 'dev.temper.android' -or $temperVersion -notmatch '^0\.\d+\.\d+$') { throw 'Unexpected testing APK version/package.' }

# A test-configured signed AAB must never be mistaken for a production upload.
Add-Type -AssemblyName System.IO.Compression.FileSystem
$temperBundle = [IO.Compression.ZipFile]::OpenRead($temperAab)
try {
    if ($temperBundle.Entries.FullName -match '^META-INF/[^/]+\.(RSA|DSA|EC|SF)$') { throw 'Rebuild an unsigned/unconfigured review AAB before candidate packaging.' }
} finally { $temperBundle.Dispose() }

Push-Location $temperRoot
try {
    $temperStatus = & git status --porcelain
    if ($LASTEXITCODE -ne 0 -or $temperStatus) { throw 'Commit the verified candidate first so the source archive matches a clean revision.' }
    $temperRevision = & git rev-parse HEAD
    if ($LASTEXITCODE -ne 0) { throw 'Could not identify source revision.' }
    $temperTracked = & git ls-files
    if ($LASTEXITCODE -ne 0) { throw 'Could not inspect source paths.' }
    if ($temperTracked -match '(^|/)(secrets|private-conversations|temp|tmp)/|\.(pem|key|jks|keystore)$') { throw 'Private/runtime material must not be tracked in the candidate.' }
} finally { Pop-Location }

$temperDist = Join-Path $temperRoot 'dist'
New-Item -ItemType Directory -Path $temperDist -Force | Out-Null
$temperStage = Join-Path $temperRoot ('temp\consumer-package-' + [Guid]::NewGuid().ToString('N') + '\TEMPER-consumer-' + $temperVersion)
New-Item -ItemType Directory -Path $temperStage -Force | Out-Null
function Copy-TemperCandidateFile([string]$Source, [string]$Relative) {
    $temperTarget = Join-Path $temperStage $Relative
    New-Item -ItemType Directory -Path (Split-Path -Parent $temperTarget) -Force | Out-Null
    Copy-Item -LiteralPath $Source -Destination $temperTarget
}
Copy-TemperCandidateFile $temperApk ('TEMPER-' + $temperVersion + '-testing.apk')
Copy-TemperCandidateFile $temperAab ('TEMPER-' + $temperVersion + '-UNSIGNED-review.aab')
Copy-TemperCandidateFile $temperJar 'backend\cerebro-0.0.1-SNAPSHOT.jar'
foreach ($temperDoc in Get-ChildItem -LiteralPath (Join-Path $temperRoot 'docs\release') -File) {
    Copy-TemperCandidateFile $temperDoc.FullName ('docs\release\' + $temperDoc.Name)
}
foreach ($temperRelative in @('docs\phases\PHASE-41-CONTEXT.md', 'docs\phases\PHASE-41-HANDOFF.md', 'deployment\store\Dockerfile', 'deployment\store\store.env.example', '.dockerignore', 'scripts\build-consumer-release.ps1', 'scripts\check-android-native-alignment.py', 'android\app\src\main\assets\emotion\NOTICE.txt')) {
    Copy-TemperCandidateFile (Join-Path $temperRoot $temperRelative) $temperRelative
}
foreach ($temperRelative in @('docs\phases\PHASE-42-CONTEXT.md','docs\phases\PHASE-42-HANDOFF.md','deployment\feedback\Dockerfile','deployment\feedback\feedback.env.example','scripts\train-feedback-model.py','scripts\approve-feedback-model.py','scripts\test-feedback-training.py','scripts\model-export-requirements.txt')) {
    Copy-TemperCandidateFile (Join-Path $temperRoot $temperRelative) $temperRelative
}
foreach ($temperRelative in @('docs\phases\PHASE-43-CONTEXT.md','docs\phases\PHASE-43-HANDOFF.md')) {
    Copy-TemperCandidateFile (Join-Path $temperRoot $temperRelative) $temperRelative
}
foreach ($temperDeploymentRelative in $temperTracked | Where-Object { $_ -like 'deployment/feedback/*' -and $_ -notin @('deployment/feedback/Dockerfile','deployment/feedback/feedback.env.example') }) {
    Copy-TemperCandidateFile (Join-Path $temperRoot $temperDeploymentRelative) $temperDeploymentRelative
}
foreach ($temperEvidence in @(
    @('temp\consumer-avatar-gallery.png', 'evidence\avatar-gallery.png'),
    @('temp\consumer-dummy-spectrum.json', 'evidence\dummy-spectrum.json'),
    @('temp\learning-training-smoke-42\smoke-report.json', 'evidence\synthetic-training-smoke.json'),
    @('screenshots\test\phase41-floating-dummy.png', 'evidence\floating-dummy.png'),
    @('screenshots\test\phase41-floating-popup.png', 'evidence\floating-popup.png')
)) {
    Copy-TemperCandidateFile (Join-Path $temperRoot $temperEvidence[0]) $temperEvidence[1]
}
$temperReadme = @'
# TEMPER consumer candidate

Start with docs/release/CONSUMER-RELEASE.md and PLAY-CONSOLE-SETUP.md.
The testing APK is installed on the test phone and has no real sale configuration.
The UNSIGNED-review AAB cannot be uploaded as a signed production release.
Premium purchases are disabled until real Play products and HTTPS verification exist.
Automatic analysis currently supports the verified WhatsApp layout only.
Read the model limits and outstanding checks in ON-DEVICE-CHECK.md and Phase 43 handoff.
ANALYSIS-FEEDBACK.md documents optional rated-session sharing and the evaluated model upgrade path.
No real feedback service is configured, no user data was collected, and the current model is unchanged.

The source ZIP contains the committed repository. Extract it before building or
deploying deployment/store/Dockerfile from its repository root. This is an untested
cloud deployment recipe; complete the release gates before selling or publishing.
The feedback directory contains a separate persistent-volume HTTPS deployment kit.
Weights, credentials, test signing keys and private runtime data are excluded.
Evidence contains only generated fixture results and TEMPER's own fictional screen.
Avatar/spectrum/floating screenshots were captured in Phase 41 with the unchanged model/assets.
The Phase 42 training smoke is synthetic and cannot be promoted as a customer model.
'@
$temperReadme | Set-Content -LiteralPath (Join-Path $temperStage 'README.md') -Encoding utf8
$temperRevision | Set-Content -LiteralPath (Join-Path $temperStage 'SOURCE-REVISION.txt') -Encoding ascii
$temperSource = Join-Path $temperStage ('TEMPER-source-' + $temperVersion + '.zip')
Push-Location $temperRoot
try {
    & git archive --format=zip ('--output=' + $temperSource) HEAD
    if ($LASTEXITCODE -ne 0) { throw 'Source archive failed.' }
} finally { Pop-Location }
$temperManifest = foreach ($temperFile in Get-ChildItem -LiteralPath $temperStage -File -Recurse) {
    '{0}  {1}' -f (Get-FileHash -LiteralPath $temperFile.FullName -Algorithm SHA256).Hash.ToLowerInvariant(), [IO.Path]::GetRelativePath($temperStage, $temperFile.FullName).Replace('\', '/')
}
$temperManifest | Set-Content -LiteralPath (Join-Path $temperStage 'SHA256SUMS.txt') -Encoding ascii
$temperZip = Join-Path $temperDist ('TEMPER-consumer-' + $temperVersion + '.zip')
Compress-Archive -LiteralPath $temperStage -DestinationPath $temperZip -Force
$temperStandaloneApk = Join-Path $temperDist ('TEMPER-' + $temperVersion + '-testing.apk')
$temperStandaloneAab = Join-Path $temperDist ('TEMPER-' + $temperVersion + '-UNSIGNED-review.aab')
Copy-Item -LiteralPath $temperApk -Destination $temperStandaloneApk -Force
Copy-Item -LiteralPath $temperAab -Destination $temperStandaloneAab -Force
$temperChecksums = foreach ($temperArtifact in @($temperStandaloneApk, $temperStandaloneAab, $temperZip)) {
    '{0}  {1}' -f (Get-FileHash -LiteralPath $temperArtifact -Algorithm SHA256).Hash.ToLowerInvariant(), [IO.Path]::GetFileName($temperArtifact)
}
$temperChecksums | Set-Content -LiteralPath (Join-Path $temperDist ('SHA256SUMS-consumer-' + $temperVersion + '.txt')) -Encoding ascii
Write-Output ('Packaged tested candidate from ' + $temperRevision + ' in ' + $temperDist + '. Production sales remain gated by owner configuration and Play tests.')
