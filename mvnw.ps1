# OHCP Maven bootstrap script (Windows PowerShell 5.1+ / 7)
$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$MAVEN_VERSION = '3.9.9'
$ROOT = $PSScriptRoot
$DIST = Join-Path $ROOT '.mvn-dist'
$MAVEN_HOME = Join-Path $DIST "apache-maven-$MAVEN_VERSION"
$MVN = Join-Path $MAVEN_HOME 'bin\mvn.cmd'

if (-not (Test-Path $MVN)) {
    Write-Host "[OHCP] First run: downloading Apache Maven $MAVEN_VERSION ..."
    New-Item -ItemType Directory -Force -Path $DIST | Out-Null
    $ZIP = Join-Path $DIST "apache-maven-$MAVEN_VERSION-bin.zip"
    if (-not (Test-Path $ZIP)) {
        Invoke-WebRequest -UseBasicParsing -Uri "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.zip" -OutFile $ZIP
    }
    if (-not (Test-Path $MAVEN_HOME)) {
        Expand-Archive -Path $ZIP -DestinationPath $DIST -Force
    }
}

& $MVN @args
exit $LASTEXITCODE