param(
  [string]$OutputDirectory = ""
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Version = (Get-Content -Raw -Encoding UTF8 (Join-Path $Root "VERSION")).Trim()
$OutputRoot = if ($OutputDirectory) { $OutputDirectory } else { Join-Path $Root "dist" }
$Archive = Join-Path $OutputRoot "PTAgent-$Version-release.zip"
$Staging = Join-Path ([IO.Path]::GetTempPath()) "ptagent-package-$PID"
$TempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$ResolvedStaging = [IO.Path]::GetFullPath($Staging)

if (-not $ResolvedStaging.StartsWith($TempRoot, [StringComparison]::OrdinalIgnoreCase)) {
  throw "Staging directory escaped the system temporary directory."
}

try {
  & (Join-Path $PSScriptRoot "test.ps1")
  if (Test-Path -LiteralPath $Staging) {
    Remove-Item -LiteralPath $Staging -Recurse -Force
  }
  New-Item -ItemType Directory -Force -Path $Staging, $OutputRoot | Out-Null

  New-Item -ItemType Directory -Force -Path (Join-Path $Staging "build") | Out-Null
  Copy-Item -LiteralPath (Join-Path $Root "build\classes") -Destination (Join-Path $Staging "build") -Recurse
  Copy-Item -LiteralPath (Join-Path $Root "public") -Destination $Staging -Recurse
  Copy-Item -LiteralPath (Join-Path $Root "docs") -Destination $Staging -Recurse
  Copy-Item -LiteralPath (Join-Path $Root "database") -Destination $Staging -Recurse
  New-Item -ItemType Directory -Force -Path (Join-Path $Staging "config") | Out-Null
  Copy-Item -LiteralPath (Join-Path $Root "config\application.env.example") -Destination (Join-Path $Staging "config")
  New-Item -ItemType Directory -Force -Path (Join-Path $Staging "scripts") | Out-Null
  Copy-Item -LiteralPath (Join-Path $Root "scripts\run-release.ps1") -Destination (Join-Path $Staging "scripts")
  Copy-Item -LiteralPath (Join-Path $Root "scripts\init-database.ps1") -Destination (Join-Path $Staging "scripts")
  Copy-Item -LiteralPath (Join-Path $Root "README.md") -Destination $Staging
  Copy-Item -LiteralPath (Join-Path $Root "VERSION") -Destination $Staging
  Copy-Item -LiteralPath (Join-Path $Root "example.xlsx") -Destination $Staging

  if (Test-Path -LiteralPath $Archive) {
    Remove-Item -LiteralPath $Archive -Force
  }
  Compress-Archive -Path (Join-Path $Staging "*") -DestinationPath $Archive -CompressionLevel Optimal
  Write-Host "Pre-integration package created: $Archive"
} finally {
  if (Test-Path -LiteralPath $Staging) {
    Remove-Item -LiteralPath $Staging -Recurse -Force
  }
}
