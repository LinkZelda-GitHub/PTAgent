param()

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$BuildDir = Join-Path $Root "build\classes"
$SourceDir = Join-Path $Root "backend\src\main\java"

New-Item -ItemType Directory -Force -Path $BuildDir | Out-Null
$Files = Get-ChildItem -Path $SourceDir -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }

if (-not $Files) {
  throw "No Java source files found."
}

javac -encoding UTF-8 -d $BuildDir $Files
Write-Host "Build completed: $BuildDir"
