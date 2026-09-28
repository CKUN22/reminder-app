$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$workflow = Get-Content -Raw -Encoding UTF8 (Join-Path $root '.github/workflows/supabase-keepalive.yml')
$migration = Get-Content -Raw -Encoding UTF8 (Join-Path $root 'supabase/migrations/202609280001_keepalive.sql')
if ($workflow -notmatch "cron: '23 3 \* \* \*'" -or $workflow -notmatch 'workflow_dispatch:') { throw 'Keepalive must run daily and allow manual checks' }
if ($workflow -notmatch 'rest/v1/project_keepalive' -or $workflow -notmatch 'secrets.SUPABASE_ANON_KEY') { throw 'Keepalive query or credential missing' }
if ($workflow -match 'service_role') { throw 'Admin credential must not be used' }
if ($migration -notmatch 'enable row level security' -or $migration -notmatch 'for select to anon' -or $migration -notmatch 'grant select') { throw 'Keepalive read policy missing' }
Write-Output 'PASS: Supabase keepalive contract'
