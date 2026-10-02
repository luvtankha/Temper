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
foreach ($temperDoc in Get-ChildItem -LiteralPath (Join-Path $temperRoot 'docs\release') -File -Recurse) {
    Copy-TemperCandidateFile $temperDoc.FullName ([IO.Path]::GetRelativePath($temperRoot, $temperDoc.FullName))
}
foreach ($temperRelative in @('docs\phases\PHASE-41-CONTEXT.md', 'docs\phases\PHASE-41-HANDOFF.md', 'deployment\store\Dockerfile', 'deployment\store\store.env.example', '.dockerignore', 'scripts\build-consumer-release.ps1', 'scripts\check-android-native-alignment.py', 'android\app\src\main\assets\emotion\NOTICE.txt')) {
    Copy-TemperCandidateFile (Join-Path $temperRoot $temperRelative) $temperRelative
}
foreach ($temperRelative in @('docs\phases\PHASE-42-CONTEXT.md','docs\phases\PHASE-42-HANDOFF.md','deployment\feedback\Dockerfile','deployment\feedback\feedback.env.example','scripts\train-feedback-model.py','scripts\approve-feedback-model.py','scripts\test-feedback-training.py','scripts\model-export-requirements.txt')) {
    Copy-TemperCandidateFile (Join-Path $temperRoot $temperRelative) $temperRelative
}
foreach ($temperRelative in @('docs\phases\PHASE-43-CONTEXT.md','docs\phases\PHASE-43-HANDOFF.md','docs\phases\PHASE-44-CONTEXT.md','docs\phases\PHASE-44-HANDOFF.md','docs\phases\PHASE-45-CONTEXT.md','docs\phases\PHASE-45-HANDOFF.md','docs\phases\PHASE-46-CONTEXT.md','docs\phases\PHASE-46-HANDOFF.md')) {
    Copy-TemperCandidateFile (Join-Path $temperRoot $temperRelative) $temperRelative
}
foreach ($temperRelative in @('docs\phases\PHASE-47-CONTEXT.md','docs\phases\PHASE-47-HANDOFF.md','scripts\generate-studio-avatars.py','scripts\figma-build-payload.py')) {
    Copy-TemperCandidateFile (Join-Path $temperRoot $temperRelative) $temperRelative
}
foreach ($temperRelative in @('docs\phases\PHASE-48-CONTEXT.md','docs\phases\PHASE-48-HANDOFF.md')) {
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
$temperReadme = @'
# TEMPER consumer candidate

Start with docs/release/FINAL-VERIFICATION.md and SETUP-AND-DEMO.md.
The installed R8-device-review APK is minified, non-debuggable and development-signed.
The testing APK is a separate debuggable developer artifact. Neither is for publishing.
Read the report's actual acceptance result; packaging does not mark pending gates passed.
The UNSIGNED-review AAB cannot be uploaded as a signed production release.
Premium purchases are disabled until real Play products and HTTPS verification exist.
Automatic analysis currently supports the verified WhatsApp layout only.
Read the Phase 48 handoff for final QA fixes and test boundaries.
Read AVATAR-CAROUSEL.md for the approved home and Phase 47 Figma prototype.
Verified public weights are bundled in both APKs/AAB for offline first-use preparation.
Source builds fetch pinned weights or use TEMPER_MODEL_FILE; weights are not in source ZIP.
Read model limits in CONTEXT-AND-PERFORMANCE.md; classifier weights were not retrained in QA.
ANALYSIS-FEEDBACK.md documents optional rated-session sharing and the evaluated model upgrade path.
No real feedback service is configured and no user data was collected.
Emotion-bar weights are unchanged; the direction head is a fictional-data-trained pilot.
Its synthetic agreement is not a real-world accuracy score.

The source ZIP contains the committed repository. Extract it before building or
deploying deployment/store/Dockerfile from its repository root. This is an untested
cloud deployment recipe; complete the release gates before selling or publishing.
The feedback directory contains a separate persistent-volume HTTPS deployment kit.
Weights are included only inside installable artifacts. Credentials, signing keys and private runtime data are excluded.
Evidence contains only generated fixture results and TEMPER's own fictional screen.
Phase 48 screenshots show only TEMPER's own UI. They do not prove live host-message updates.
The older avatar gallery and generated home renders are from Phase 47.
The spectrum/floating screenshots are historical Phase 41 fixtures.
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
$temperChecksums = foreach ($temperArtifact in @($temperStandaloneApk, $temperStandaloneAab, $temperR8Review, $temperZip)) {
    '{0}  {1}' -f (Get-FileHash -LiteralPath $temperArtifact -Algorithm SHA256).Hash.ToLowerInvariant(), [IO.Path]::GetFileName($temperArtifact)
}
$temperChecksums | Set-Content -LiteralPath (Join-Path $temperDist ('SHA256SUMS-consumer-' + $temperVersion + '.txt')) -Encoding ascii
Write-Output ('Packaged tested candidate from ' + $temperRevision + ' in ' + $temperDist + '. Production sales remain gated by owner configuration and Play tests.')
