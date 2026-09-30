param([Parameter(Mandatory=$true)][string]$Destination)
$ErrorActionPreference = 'Stop'
$manifestPath = Join-Path $PSScriptRoot '../models/foundation/manifest.json'
$manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
$modelDirectory = [System.IO.Path]::GetFullPath($Destination)
New-Item -ItemType Directory -Path $modelDirectory -Force | Out-Null
foreach ($file in $manifest.files.PSObject.Properties) {
    $target = Join-Path $modelDirectory $file.Name
    if ((Test-Path -LiteralPath $target) -and ((Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant() -eq $file.Value)) { continue }
    $uri = 'https://huggingface.co/' + $manifest.modelId + '/resolve/' + $manifest.revision + '/onnx/' + $file.Name
    Invoke-WebRequest -Uri $uri -OutFile $target
    if ((Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant() -ne $file.Value) { throw ('Checksum mismatch: ' + $file.Name) }
}
Write-Output ('Verified foundation artifacts: ' + $modelDirectory)
