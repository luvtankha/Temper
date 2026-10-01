param([Parameter(Mandatory=$true)][uri]$StoreUrl,[Parameter(Mandatory=$true)][string]$EntitlementPublicKey,[uri]$FeedbackUrl,[string]$ApprovedEmotionModelSpec)
$ErrorActionPreference='Stop'
if ($StoreUrl.Scheme -ne 'https' -or !$StoreUrl.Host -or $StoreUrl.AbsolutePath -ne '/' -or $StoreUrl.Query -or $StoreUrl.Fragment -or $StoreUrl.UserInfo) {throw 'Supply the HTTPS origin of the deployed purchase-verification server.'}
[Convert]::FromBase64String($EntitlementPublicKey) | Out-Null
foreach($temperVariable in @('TEMPER_KEYSTORE_FILE','TEMPER_KEYSTORE_PASSWORD','TEMPER_KEYSTORE_ALIAS','TEMPER_KEYSTORE_KEY_PASSWORD')) {if(![Environment]::GetEnvironmentVariable($temperVariable)){throw ('Set '+$temperVariable+' securely before building.')}}
if (!(Test-Path -LiteralPath $env:TEMPER_KEYSTORE_FILE -PathType Leaf)) {throw 'The upload keystore is missing.'}
$temperRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$temperApprovedSpec=if($ApprovedEmotionModelSpec){[IO.Path]::GetFullPath($ApprovedEmotionModelSpec)}else{$null}
Push-Location (Join-Path $temperRoot 'android')
try {
    $temperArguments=@(':app:verifyStoreConfig',':app:testDebugUnitTest',':app:lintRelease',':app:assembleRelease',':app:bundleRelease',('-PtemperStoreUrl='+$StoreUrl.AbsoluteUri.TrimEnd('/')),('-PtemperEntitlementKey='+$EntitlementPublicKey))
    if($FeedbackUrl){if($FeedbackUrl.Scheme -ne 'https' -or !$FeedbackUrl.Host -or $FeedbackUrl.AbsolutePath -ne '/' -or $FeedbackUrl.Query -or $FeedbackUrl.Fragment -or $FeedbackUrl.UserInfo){throw 'Supply the HTTPS origin of the separate feedback service.'};$temperArguments+=('-PtemperLearningUrl='+$FeedbackUrl.AbsoluteUri.TrimEnd('/'))}
    if($temperApprovedSpec){$temperArguments+=('-PtemperEmotionModelSpec='+$temperApprovedSpec)}
    & .\gradlew.bat @temperArguments
    if($LASTEXITCODE -ne 0){throw 'Release checks/build failed.'}
} finally {Pop-Location}
$temperDist=Join-Path $temperRoot 'dist\consumer-release';New-Item -ItemType Directory -Path $temperDist -Force | Out-Null
$temperVersion=(Get-Content -LiteralPath (Join-Path $temperRoot 'android\app\build\outputs\apk\release\output-metadata.json') -Raw | ConvertFrom-Json).elements[0].versionName
Copy-Item -LiteralPath (Join-Path $temperRoot 'android\app\build\outputs\apk\release\app-release.apk') -Destination (Join-Path $temperDist ('TEMPER-'+$temperVersion+'.apk')) -Force
Copy-Item -LiteralPath (Join-Path $temperRoot 'android\app\build\outputs\bundle\release\app-release.aab') -Destination (Join-Path $temperDist ('TEMPER-'+$temperVersion+'.aab')) -Force
Copy-Item -LiteralPath (Join-Path $temperRoot 'android\app\build\outputs\mapping\release\mapping.txt') -Destination (Join-Path $temperDist 'mapping.txt') -Force
$temperChecksums=foreach($temperArtifact in Get-ChildItem -LiteralPath $temperDist -File | Where-Object {$_.Name -ne 'SHA256SUMS.txt'}){'{0}  {1}' -f (Get-FileHash -LiteralPath $temperArtifact.FullName).Hash.ToLowerInvariant(),$temperArtifact.Name}
$temperChecksums | Set-Content -LiteralPath (Join-Path $temperDist 'SHA256SUMS.txt')
Write-Output ('Signed candidate prepared in '+$temperDist+'. Complete real Play purchase/device tests and declarations before publishing.')
