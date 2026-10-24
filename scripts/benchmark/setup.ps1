$ErrorActionPreference = 'Stop'
$repo = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$directory = Join-Path $repo '.tools/presentmon'
$binary = Join-Path $directory 'PresentMon-2.3.1-x64.exe'
$expected = '364e5d98d4d134bd54dd25c22ed2ca2f4883f8bc3ed6502bee0c151e3436d30c'
New-Item -ItemType Directory -Path $directory -Force | Out-Null
if (!(Test-Path -LiteralPath $binary)) {
    $download = Join-Path $directory ('download-' + [guid]::NewGuid().ToString('N') + '.tmp')
    try {
        Invoke-WebRequest -Uri 'https://github.com/GameTechDev/PresentMon/releases/download/v2.3.1/PresentMon-2.3.1-x64.exe' -OutFile $download
        if ((Get-FileHash -LiteralPath $download -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expected) { throw 'PresentMon download hash mismatch.' }
        Move-Item -LiteralPath $download -Destination $binary
    } finally { if (Test-Path -LiteralPath $download) { Remove-Item -LiteralPath $download } }
}
if ((Get-FileHash -LiteralPath $binary -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expected) { throw 'PresentMon binary hash mismatch.' }
Write-Output 'PresentMon 2.3.1 x64 verified. No service, driver or system settings installed.'
