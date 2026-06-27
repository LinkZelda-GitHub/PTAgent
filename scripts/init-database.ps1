param(
  [string]$Server = "127.0.0.1",
  [int]$Port = 3306,
  [string]$Username = "root",
  [string]$Password = "",
  [string]$Database = "ptagent"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Migration = Join-Path $Root "database\migrations\V1__init.sql"
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

  Get-Content -Raw -Encoding utf8 $Migration | & $Mysql.Source --host=$Server --port=$Port --user=$Username --database=$Database
  if ($LASTEXITCODE -ne 0) {
    throw "Failed to apply migration $Migration."
  }
} finally {
  Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}

Write-Host "Database initialized: $Database@$Server`:$Port"
