$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$tools = Join-Path $repo '.tools'
$jdk = Join-Path $tools 'jdk-21'
if (Test-Path (Join-Path $jdk 'bin/javac.exe')) { & (Join-Path $jdk 'bin/java.exe') -version; exit $LASTEXITCODE }
New-Item -ItemType Directory -Force $tools | Out-Null
$uri = 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jdk_x64_windows_hotspot_21.0.12.1_1.zip'
$expected = 'f9d6e191ab098c0d416e7d588a24420a8621cd2f4720dab2459b8b7b2d2d8b4e'
$archive = Join-Path $tools 'jdk.zip'
Invoke-WebRequest $uri -OutFile $archive
if ((Get-FileHash $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expected) { throw 'JDK SHA-256 mismatch. Installation stopped.' }
$unpack = Join-Path $tools 'jdk-unpack'
if (Test-Path $unpack) { throw 'Incomplete installation exists in .tools/jdk-unpack; inspect it before retrying.' }
Expand-Archive -LiteralPath $archive -DestinationPath $unpack
$folders = @(Get-ChildItem $unpack -Directory)
if ($folders.Count -ne 1) { throw 'Unexpected JDK archive structure' }
Move-Item -LiteralPath $folders[0].FullName -Destination $jdk
& (Join-Path $jdk 'bin/javac.exe') -version
if ($LASTEXITCODE -ne 0) { throw 'JDK validation failed' }
