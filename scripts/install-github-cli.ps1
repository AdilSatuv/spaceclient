$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$tools = Join-Path $repo '.tools'
$binary = Join-Path $tools 'gh/bin/gh.exe'
if (Test-Path $binary) { & $binary --version; exit $LASTEXITCODE }
New-Item -ItemType Directory -Force $tools | Out-Null
$archive = Join-Path $tools 'gh.zip'
Invoke-WebRequest 'https://github.com/cli/cli/releases/download/v2.103.0/gh_2.103.0_windows_amd64.zip' -OutFile $archive
$expected = '80c846a5b8432672ca6373b34ce9de998c9686aaa1885c845f2e1ee81f713726'
if ((Get-FileHash $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expected) { throw 'GitHub CLI checksum mismatch' }
Expand-Archive -LiteralPath $archive -DestinationPath (Join-Path $tools 'gh')
& $binary --version
