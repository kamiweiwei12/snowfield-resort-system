# Create/verify the integration-test-only database `snowfield_test`.
#
# Why: docs/ARCHITECTURE.md §7 requires the test database to be isolated from the
# development database `snowfield`; tests must not pollute dev data.
# This script only creates idempotently; it never deletes or resets anything.
#
# Usage (from repository root, after `docker compose up -d db`):
#   powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/test-db-init.ps1

$ErrorActionPreference = 'Stop'

$container = 'snowfield-db'
$dbUser = 'snowfield'
$testDb = 'snowfield_test'

$state = docker inspect -f '{{.State.Health.Status}}' $container 2>$null
if ($LASTEXITCODE -ne 0) {
    throw "Container $container not found. Run 'docker compose up -d db' in the repo root first."
}
if ($state -ne 'healthy') {
    throw "Container $container health is '$state', expected 'healthy'. Refusing to continue."
}

# Probe by attempting the idempotent CREATE; PostgreSQL reports an existing
# database with error code 42P04, which we treat as success.
# (Avoids depending on capturing `docker exec` stdout, which is unreliable
# under Windows PowerShell 5.1.)
$create = 'CREATE DATABASE snowfield_test'
# Native commands writing to stderr would trigger $ErrorActionPreference='Stop';
# scope the relaxation to this single call so real failures are still handled below.
$prevEap = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
$out = & docker exec $container psql -U $dbUser -d postgres -v ON_ERROR_STOP=1 -c $create 2>&1
$code = $LASTEXITCODE
$ErrorActionPreference = $prevEap
$text = ($out | Out-String)

if ($code -eq 0) {
    Write-Output "OK: created test database $testDb."
}
elseif ($text -match '42P04|already exists') {
    Write-Output "OK: test database $testDb already exists; nothing changed."
}
else {
    throw "Failed to create test database $testDb (exit $code): $text"
}
