param(
    [Parameter(Mandatory)][ValidatePattern('^[A-Za-z0-9_-]+$')][string]$Serial,
    [string]$AdbPath = "$env:USERPROFILE\.codex\cache\temper-tools\android-sdk\platform-tools\adb.exe"
)
$ErrorActionPreference = 'Stop'
$temperRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$temperConfig = Join-Path $temperRoot 'temp\overlay-connection.json'
if (!(Test-Path -LiteralPath $AdbPath)) { throw 'Provide the installed Android platform-tools adb.exe path.' }
if (!(Test-Path -LiteralPath $temperConfig)) { throw 'Start the local overlay backend first to create its private connection configuration.' }
$temperJson = Get-Content -Raw -LiteralPath $temperConfig
if (($temperJson | ConvertFrom-Json).token -notmatch '^[a-f0-9]{64}$') { throw 'Invalid private USB configuration.' }
try {
    $temperState = & $AdbPath -s $Serial get-state
    if ($LASTEXITCODE -ne 0 -or $temperState -ne 'device') { throw 'Connect and authorize the selected USB phone first.' }
    # Stdin goes straight into our debug app's private file; no public temporary token file.
    $temperJson | & $AdbPath -s $Serial shell -T "run-as dev.temper.android sh -c 'cat > files/overlay-connection.json'"
    if ($LASTEXITCODE -ne 0) { throw 'Install the TEMPER debug APK before configuring its private USB connection.' }
    & $AdbPath -s $Serial shell run-as dev.temper.android test -s files/overlay-connection.json
    if ($LASTEXITCODE -ne 0) { throw 'Private connection configuration was not saved.' }
    & $AdbPath -s $Serial reverse tcp:8080 tcp:8080 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'USB port forwarding failed.' }
    Write-Output 'TEMPER private USB connection configured. Consent and capture state were not changed.'
} finally { $temperJson = $null }
