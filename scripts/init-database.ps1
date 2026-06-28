param(
  [string]$Server = "127.0.0.1",
  [int]$Port = 3306,
  [string]$Username = "root",
  [string]$Password = "",
  [string]$Database = "ptagent"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$MigrationDirectory = Join-Path $Root "database\migrations"
$Migrations = Get-ChildItem -LiteralPath $MigrationDirectory -Filter "*.sql" | Sort-Object Name
$Mysql = Get-Command mysql -ErrorAction SilentlyContinue

if (-not $Mysql) {
  throw "mysql client was not found in PATH. Install MySQL 8 client first."
}

$env:MYSQL_PWD = $Password
try {
  & $Mysql.Source --host=$Server --port=$Port --user=$Username --execute="CREATE DATABASE IF NOT EXISTS $Database CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
  if ($LASTEXITCODE -ne 0) {
    throw "Failed to create database $Database."
  }

  foreach ($Migration in $Migrations) {
    Get-Content -Raw -Encoding utf8 $Migration.FullName | & $Mysql.Source --host=$Server --port=$Port --user=$Username --database=$Database
    if ($LASTEXITCODE -ne 0) {
      throw "Failed to apply migration $($Migration.FullName)."
    }
    Write-Host "Applied migration: $($Migration.Name)"
  }
} finally {
  Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}

Write-Host "Database initialized: $Database@$Server`:$Port"
