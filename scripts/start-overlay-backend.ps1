param(
    [string]$JavaHome = "$env:USERPROFILE\.codex\cache\temper-tools\jdk-21.0.12.1+1",
    [string]$ModelsRoot = "$env:USERPROFILE\.codex\cache\temper-models"
)
$ErrorActionPreference = 'Stop'
$temperRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$temperJar = Join-Path $temperRoot 'backend\target\cerebro-0.0.1-SNAPSHOT.jar'
$temperJava = Join-Path $JavaHome 'bin\java.exe'
if (!(Test-Path -LiteralPath $temperJar) -or !(Test-Path -LiteralPath $temperJava)) { throw 'Build the backend and provide a Java21 installation first.' }
if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) { throw 'Port8080 is already in use. Check the existing server before starting another.' }
$temperTemp = Join-Path $temperRoot 'temp'
New-Item -ItemType Directory -Path $temperTemp -Force | Out-Null
$temperConnection = Join-Path $temperTemp 'overlay-connection.json'
if (!(Test-Path -LiteralPath $temperConnection)) {
    $temperBytes = New-Object byte[] 32
    [Security.Cryptography.RandomNumberGenerator]::Fill($temperBytes)
    @{ token = [Convert]::ToHexString($temperBytes).ToLowerInvariant() } | ConvertTo-Json -Compress | Set-Content -LiteralPath $temperConnection -NoNewline
}
$temperToken = (Get-Content -Raw -LiteralPath $temperConnection | ConvertFrom-Json).token
if ($temperToken -notmatch '^[a-f0-9]{64}$') { throw 'Invalid private overlay connection configuration.' }
$env:TEMPER_OVERLAY_TOKEN = $temperToken
$env:SERVER_ADDRESS = '127.0.0.1'
$env:SERVER_PORT = '8080'
$env:TEMPER_FOUNDATION_MODEL_DIR = Join-Path $ModelsRoot 'foundation-distilbert'
$env:TEMPER_SENTIMENT_MODEL_DIR = Join-Path $ModelsRoot 'sentiment-roberta'
$env:TEMPER_EMOTION_MODEL_DIR = Join-Path $ModelsRoot 'emotion-goemotions'
$env:TEMPER_SARCASM_MODEL_DIR = Join-Path $ModelsRoot 'sarcasm-bert'
$env:TEMPER_TOXICITY_MODEL_DIR = Join-Path $ModelsRoot 'toxicity-bert'
foreach ($temperModel in @($env:TEMPER_FOUNDATION_MODEL_DIR,$env:TEMPER_SENTIMENT_MODEL_DIR,$env:TEMPER_EMOTION_MODEL_DIR,$env:TEMPER_SARCASM_MODEL_DIR,$env:TEMPER_TOXICITY_MODEL_DIR)) {
    if (!(Test-Path -LiteralPath (Join-Path $temperModel 'model.onnx'))) { throw 'A required local model is missing. See the model download scripts.' }
}
$temperProcess = Start-Process -FilePath $temperJava -ArgumentList @('-jar',('"{0}"' -f $temperJar)) -WorkingDirectory $temperRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $temperTemp 'overlay-backend.log') -RedirectStandardError (Join-Path $temperTemp 'overlay-backend-error.log')
$temperProcess.Id | Set-Content -LiteralPath (Join-Path $temperTemp 'overlay-backend.pid')
Write-Output "Local TEMPER backend starting on127.0.0.1:8080 (PID $($temperProcess.Id)). Private connection token retained."
