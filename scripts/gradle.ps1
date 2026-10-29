param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GradleArguments
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$localJdk = Get-ChildItem -LiteralPath "$projectRoot/.tools/java" -Directory -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -like 'jdk-17*' } | Select-Object -First 1
$previousJava = $env:JAVA_HOME
try {
    if ($localJdk) { $env:JAVA_HOME = $localJdk.FullName }
    Push-Location $projectRoot
    try {
        & "$projectRoot/gradlew.bat" @GradleArguments
        $result = $LASTEXITCODE
    } finally { Pop-Location }
} finally { $env:JAVA_HOME = $previousJava }
exit $result
