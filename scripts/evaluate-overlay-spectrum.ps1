param([ValidatePattern('^[A-Za-z0-9._-]+\.json$')][string]$ReportName='overlay-spectrum-results.json')
$ErrorActionPreference='Stop'
$temperRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$temperCases=Get-Content -Raw -LiteralPath (Join-Path $temperRoot 'backend/src/test/resources/overlay-chat-styles.json') | ConvertFrom-Json
$temperToken=(Get-Content -Raw -LiteralPath (Join-Path $temperRoot 'temp/overlay-connection.json') | ConvertFrom-Json).token
if($temperToken -notmatch '^[a-f0-9]{64}$'){throw 'Start the configured local backend first.'}
$temperHeaders=@{Authorization='Bearer '+$temperToken}
$temperOrder=@('neutral','happiness','concern','confusion','sadness','frustration','anger','surprise')
$temperResults=@()
try {
    foreach($temperCase in $temperCases){
        # Fixtures are generated examples; never read device content or accept arbitrary chat input.
        $temperBody=@{turns=$temperCase.turns}|ConvertTo-Json -Depth 5 -Compress
        $temperResponse=Invoke-RestMethod -Uri 'http://127.0.0.1:8080/api/overlay/analyze' -Method Post -ContentType 'application/json; charset=utf-8' -Headers $temperHeaders -Body ([Text.Encoding]::UTF8.GetBytes($temperBody)) -TimeoutSec 120
        if(!$temperResponse.available -or $temperResponse.spectrum.Count -ne 8){throw ('Spectrum unavailable: '+$temperCase.id)}
        $temperScores=[ordered]@{}
        for($temperIndex=0;$temperIndex -lt 8;$temperIndex++){
            $temperValue=[double]$temperResponse.spectrum[$temperIndex]
            if(![double]::IsFinite($temperValue) -or $temperValue -lt 0 -or $temperValue -gt 1){throw 'Invalid spectrum value'}
            $temperScores[$temperOrder[$temperIndex]]=$temperValue
        }
        $temperDominant=($temperScores.GetEnumerator()|Sort-Object Value -Descending|Select-Object -First 1).Key
        $temperResults += [pscustomobject]@{id=$temperCase.id;expected=$temperCase.expected;dominant=$temperDominant;spectrum=$temperScores;currentState=$temperResponse.currentState;direction=$temperResponse.direction}
        Write-Output ($temperCase.id+' | dominant='+$temperDominant+' | '+(($temperResponse.spectrum|ForEach-Object {[Math]::Round($_*100,1)})-join ','))
    }
    $temperTemp=Join-Path $temperRoot 'temp'
    New-Item -ItemType Directory -Path $temperTemp -Force|Out-Null
    $temperResults|ConvertTo-Json -Depth 7|Set-Content -LiteralPath (Join-Path $temperTemp $ReportName)
    Write-Output 'Generated-example results saved. Dominant-label agreement is a smoke check, not measured real-chat accuracy.'
} finally {$temperToken=$null;$temperHeaders=$null;$temperBody=$null}
