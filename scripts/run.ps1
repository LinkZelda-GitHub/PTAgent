param(
  [int]$Port = 8080
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$BuildScript = Join-Path $PSScriptRoot "build.ps1"
$BuildDir = Join-Path $Root "build\classes"

& $BuildScript
Set-Location $Root
java -cp $BuildDir com.ptagent.App $Port
