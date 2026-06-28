param(
  [int]$Port = 8080,
  [string]$DataDirectory = ""
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$BuildDir = Join-Path $Root "build\classes"

if (-not (Test-Path -LiteralPath (Join-Path $BuildDir "com\ptagent\App.class"))) {
  throw "Compiled application was not found in $BuildDir."
}
if ($DataDirectory) {
  $env:PTAGENT_DATA_DIR = $DataDirectory
}

Set-Location $Root
java -cp $BuildDir com.ptagent.App $Port
