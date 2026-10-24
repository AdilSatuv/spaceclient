#requires -Version 7.0
param(
    [Parameter(Mandatory=$true)][ValidateRange(1,2147483647)][int]$TargetProcessId,
    [Parameter(Mandatory=$true)][ValidateSet('vanilla','space-hud-off','space-hud-on')][string]$Variant,
    [Parameter(Mandatory=$true)][ValidateRange(1,3)][int]$Repetition,
    [Parameter(Mandatory=$true)][string]$ConditionsFile,
    [ValidateRange(5,300)][int]$DurationSeconds = 60,
    [ValidateRange(0,300)][int]$WarmupSeconds = 30
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$binary = Join-Path $repo '.tools/presentmon/PresentMon-2.3.1-x64.exe'
$expectedHash = '364e5d98d4d134bd54dd25c22ed2ca2f4883f8bc3ed6502bee0c151e3436d30c'
if (!(Test-Path -LiteralPath $binary) -or (Get-FileHash -LiteralPath $binary).Hash.ToLowerInvariant() -ne $expectedHash) {
    throw 'Run scripts/benchmark/setup.ps1; a verified PresentMon binary is required.'
}
$conditionsPath = (Resolve-Path -LiteralPath $ConditionsFile).Path
# Validate the same schema used by the analyzer before starting any capture.
& node -e 'require(process.argv[1]).validateConditions(JSON.parse(require("node:fs").readFileSync(process.argv[2],"utf8")))' (Join-Path $PSScriptRoot 'analyze.cjs') $conditionsPath
if ($LASTEXITCODE -ne 0) { throw 'Invalid benchmark conditions.' }
$conditions = Get-Content -Raw -LiteralPath $conditionsPath | ConvertFrom-Json
$target = Get-Process -Id $TargetProcessId
if ($target.ProcessName -notin @('java','javaw')) { throw 'Select the exact Minecraft Java process ID.' }
$targetStarted = $target.StartTime.ToUniversalTime()
$commit = (& git -C $repo rev-parse HEAD).Trim()
$gameTree = (& git -C $repo rev-parse HEAD:game).Trim()
if ((& git -C $repo status --porcelain -- game)) { throw 'Commit game sources before recording a benchmark.' }
$runId = (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + $Variant + '-' + $Repetition + '-' + [guid]::NewGuid().ToString('N').Substring(0,8)
$directory = Join-Path $repo ('.reports/benchmark/' + $runId)
New-Item -ItemType Directory -Path $directory | Out-Null
$hardware = [ordered]@{
    cpu = @((Get-CimInstance Win32_Processor | Select-Object -ExpandProperty Name))
    gpu = @(Get-CimInstance Win32_VideoController | ForEach-Object { [ordered]@{ name=$_.Name; driverVersion=$_.DriverVersion } })
    ramBytes = [double](Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory
    osBuild = [Environment]::OSVersion.Version.ToString()
}
$metadata = [ordered]@{
    schemaVersion = 1; status = 'failed'; variant = $Variant; repetition = $Repetition
    processId = $TargetProcessId; processStartedUtc = $targetStarted.ToString('o')
    durationSeconds = $DurationSeconds; warmupSeconds = $WarmupSeconds; startupMs = $null
    presentMon = [ordered]@{ version='2.3.1'; sha256=$expectedHash }
    conditions = $conditions; hardware = $hardware
    source = [ordered]@{ commit=$commit; gameSourceTree=$(if ($Variant -eq 'vanilla') { $null } else { $gameTree }) }
    failure = 'Capture did not finish.'
}
$samples = [System.Collections.Generic.List[object]]::new()
$capture = $null
try {
    Write-Output "Warmup: $WarmupSeconds seconds; run $runId"
    if ($WarmupSeconds -gt 0) { Start-Sleep -Seconds $WarmupSeconds }
    $target.Refresh()
    if ($target.HasExited -or $target.StartTime.ToUniversalTime() -ne $targetStarted) { throw 'Minecraft exited during warmup.' }
    $arguments = @('--process_id', "$TargetProcessId", '--v1_metrics', '--timed', "$DurationSeconds",
        '--terminate_after_timed', '--no_console_stats', '--no_track_input', '--session_name', ('SpaceClient-' + [guid]::NewGuid().ToString('N')),
        '--output_file', (Join-Path $directory 'frames.csv'))
    # No shell expansion, global session termination or automatic elevation.
    if ($arguments | Where-Object { $_ -match '["\r\n]' }) { throw 'Unsupported quote in capture path.' }
    $quotedArguments = @($arguments | ForEach-Object { '"' + $_ + '"' })
    $timer = [Diagnostics.Stopwatch]::StartNew()
    $capture = Start-Process -FilePath $binary -ArgumentList $quotedArguments -WorkingDirectory $repo -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $directory 'presentmon.stdout.txt') -RedirectStandardError (Join-Path $directory 'presentmon.stderr.txt')
    do {
        $target.Refresh()
        if ($target.HasExited -or $target.StartTime.ToUniversalTime() -ne $targetStarted) { throw 'Minecraft exited during capture.' }
        $samples.Add([ordered]@{ elapsedMs=$timer.Elapsed.TotalMilliseconds; workingSetBytes=[double]$target.WorkingSet64; privateBytes=[double]$target.PrivateMemorySize64 })
        if ($timer.Elapsed.TotalSeconds -gt $DurationSeconds + 15) { throw 'Capture timed out.' }
        Start-Sleep -Milliseconds 500
        $capture.Refresh()
    } while (!$capture.HasExited)
    $capture.WaitForExit()
    $target.Refresh()
    $samples.Add([ordered]@{ elapsedMs=$timer.Elapsed.TotalMilliseconds; workingSetBytes=[double]$target.WorkingSet64; privateBytes=[double]$target.PrivateMemorySize64 })
    $stderr = Get-Content -Raw -LiteralPath (Join-Path $directory 'presentmon.stderr.txt')
    if ($capture.ExitCode -ne 0 -or $stderr -match '(?im)^error:' -or !(Test-Path -LiteralPath (Join-Path $directory 'frames.csv'))) {
        throw 'PresentMon could not capture frames. See local stderr; ETW may require additional rights.'
    }
    if ($timer.Elapsed.TotalSeconds -lt $DurationSeconds * 0.9) { throw 'Capture ended before the requested interval.' }
    $metadata.status = 'complete'; $metadata.failure = $null
} catch {
    $metadata.failure = $_.Exception.Message
} finally {
    if ($null -ne $capture -and !$capture.HasExited) { Stop-Process -Id $capture.Id -ErrorAction SilentlyContinue }
    $samples.ToArray() | ConvertTo-Json -Depth 5 -AsArray | Set-Content -LiteralPath (Join-Path $directory 'memory.json') -Encoding utf8
    $metadata | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $directory 'capture.json') -Encoding utf8
}
if ($metadata.status -ne 'complete') {
    Write-Output ('Capture failed: ' + $metadata.failure)
    Write-Output ('Evidence: .reports/benchmark/' + $runId)
    exit 1
}
& node (Join-Path $PSScriptRoot 'analyze.cjs') $directory --out (Join-Path $directory 'summary.json')
if ($LASTEXITCODE -ne 0) {
    $metadata.status='failed'; $metadata.failure='Raw capture failed statistical validation.'
    $metadata | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $directory 'capture.json') -Encoding utf8
    exit 1
}
Write-Output ('Validated capture: .reports/benchmark/' + $runId)
