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
$temperR8Review = Join-Path $temperRoot ('dist\TEMPER-' + $temperVersion + '-R8-device-review.apk')
if (!(Test-Path -LiteralPath $temperR8Review -PathType Leaf)) { throw 'Create, signature-check and device-review the minified APK before packaging.' }

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
Copy-TemperCandidateFile $temperR8Review ('TEMPER-' + $temperVersion + '-R8-device-review.apk')
Copy-TemperCandidateFile $temperJar 'backend\cerebro-0.0.1-SNAPSHOT.jar'
foreach ($temperRelative in @(
    'README.md', 'docs\release\evidence\phase48.json',
    'deployment\store\Dockerfile', 'deployment\store\store.env.example', '.dockerignore',
    'scripts\build-consumer-release.ps1', 'scripts\check-android-native-alignment.py',
    'android\app\src\main\assets\emotion\NOTICE.txt', 'models\sentiment\ATTRIBUTION.md',
    'deployment\feedback\Dockerfile', 'deployment\feedback\feedback.env.example',
    'scripts\train-feedback-model.py', 'scripts\approve-feedback-model.py',
    'scripts\test-feedback-training.py', 'scripts\model-export-requirements.txt',
    'scripts\generate-studio-avatars.py', 'scripts\figma-build-payload.py'
)) {
    Copy-TemperCandidateFile (Join-Path $temperRoot $temperRelative) $temperRelative
}
foreach ($temperDesignRelative in $temperTracked | Where-Object { $_ -like 'design/phase47/*' }) {
    Copy-TemperCandidateFile (Join-Path $temperRoot $temperDesignRelative) $temperDesignRelative
}
foreach ($temperDeploymentRelative in $temperTracked | Where-Object { $_ -like 'deployment/feedback/*' -and $_ -notin @('deployment/feedback/Dockerfile','deployment/feedback/feedback.env.example') }) {
    Copy-TemperCandidateFile (Join-Path $temperRoot $temperDeploymentRelative) $temperDeploymentRelative
}
foreach ($temperEvidence in @(
    @('temp\consumer-avatar-gallery.png', 'evidence\avatar-gallery.png'),
    @('temp\consumer-dummy-spectrum.json', 'evidence\dummy-spectrum.json'),
    @('temp\learning-training-smoke-42\smoke-report.json', 'evidence\synthetic-training-smoke.json'),
    @('models\context\evaluation.json', 'evidence\context-pilot-evaluation.json'),
    @('models\context\evaluation-v1-rejected.json', 'evidence\context-pilot-rejected-v1.json'),
    @('models\context\phone-performance.json', 'evidence\phone-performance.json'),
    @('models\context\phone-context-results.json', 'evidence\phone-context-results.json'),
    @('design\phase47\previews\carousel-astra-off.png', 'evidence\home-off.png'),
    @('design\phase47\previews\carousel-astra-on.png', 'evidence\home-on.png'),
    @('screenshots\test\phase41-floating-dummy.png', 'evidence\floating-dummy.png'),
    @('screenshots\test\phase41-floating-popup.png', 'evidence\floating-popup.png'),
    @('temp\phase48\release-home.png', 'evidence\phase48-home-off.png'),
    @('temp\phase48\release-ready-on.png', 'evidence\phase48-home-on.png'),
    @('temp\phase48\release-settings.png', 'evidence\phase48-settings.png'),
    @('temp\phase48\release-setup.png', 'evidence\phase48-setup.png'),
    @('temp\phase48\release-fictional-preview.png', 'evidence\phase48-selected-fictional-preview.png'),
    @('temp\phase48\release-privacy-bottom.png', 'evidence\phase48-privacy-model.png'),
    @('temp\phase48\release-feedback-bottom.png', 'evidence\phase48-feedback-unconfigured.png'),
    @('temp\phase48-clean-build.log', 'evidence\phase48-clean-build.log'),
    @('temp\phase48-final-copy-build.log', 'evidence\phase48-final-build.log'),
    @('temp\phase48-freshOnly.log', 'evidence\phase48-fresh-install.log'),
    @('temp\phase48-modelRecoveryOnly.log', 'evidence\phase48-model-recovery.log'),
    @('temp\phase48-qa-preview-foundation.log', 'evidence\phase48-selected-preview-regression.log')
)) {
    Copy-TemperCandidateFile (Join-Path $temperRoot $temperEvidence[0]) $temperEvidence[1]
}
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
$temperChecksums = foreach ($temperArtifact in @($temperStandaloneApk, $temperStandaloneAab, $temperR8Review, $temperZip)) {
    '{0}  {1}' -f (Get-FileHash -LiteralPath $temperArtifact -Algorithm SHA256).Hash.ToLowerInvariant(), [IO.Path]::GetFileName($temperArtifact)
}
$temperChecksums | Set-Content -LiteralPath (Join-Path $temperDist ('SHA256SUMS-consumer-' + $temperVersion + '.txt')) -Encoding ascii
Write-Output ('Packaged tested candidate from ' + $temperRevision + ' in ' + $temperDist + '. Production sales remain gated by owner configuration and Play tests.')
