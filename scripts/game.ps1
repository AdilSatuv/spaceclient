param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Tasks = @('build'))
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$jdk = Join-Path $repo '.tools/jdk-21'
if (!(Test-Path (Join-Path $jdk 'bin/javac.exe'))) { throw 'Run scripts/bootstrap.ps1 first.' }
$previousJava = $env:JAVA_HOME
$previousGradle = $env:GRADLE_USER_HOME
try {
    $env:JAVA_HOME = $jdk
    $env:GRADLE_USER_HOME = Join-Path $repo '.tools/gradle-cache'
    Push-Location (Join-Path $repo 'game')
    try { & ./gradlew.bat @Tasks --console=plain; $code = $LASTEXITCODE }
    finally { Pop-Location }
} finally {
    $env:JAVA_HOME = $previousJava
    $env:GRADLE_USER_HOME = $previousGradle
}
exit $code
