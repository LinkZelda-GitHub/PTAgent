param()

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$MainBuildDir = Join-Path $Root "build\classes"
$TestBuildDir = Join-Path $Root "build\test-classes"
$TestSourceDir = Join-Path $Root "backend\src\test\java"

& (Join-Path $PSScriptRoot "build.ps1")

New-Item -ItemType Directory -Force -Path $TestBuildDir | Out-Null
$Files = Get-ChildItem -Path $TestSourceDir -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }

if (-not $Files) {
  throw "No Java test source files found."
}

javac -encoding UTF-8 -cp $MainBuildDir -d $TestBuildDir $Files
if ($LASTEXITCODE -ne 0) {
  throw "Test compilation failed."
}

java -cp "$MainBuildDir;$TestBuildDir" com.ptagent.ServiceTests
if ($LASTEXITCODE -ne 0) {
  throw "Service tests failed."
}
