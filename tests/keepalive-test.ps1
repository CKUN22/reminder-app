$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$workflow = Get-Content -Raw -Encoding UTF8 (Join-Path $root '.github/workflows/supabase-keepalive.yml')
$migration = Get-Content -Raw -Encoding UTF8 (Join-Path $root 'supabase/migrations/202609280001_keepalive.sql')
if ($workflow -notmatch "cron: '23 3 \* \* \*'" -or $workflow -notmatch 'workflow_dispatch:') { throw 'Keepalive must run daily and allow manual checks' }
if ($workflow -notmatch "cron: '37 4 1 \* \*'" -or $workflow -notmatch 'git commit --allow-empty' -or $workflow -notmatch 'contents: write' -or $workflow -notmatch 'test_repository_heartbeat') { throw 'Monthly repository activity must keep the public schedule enabled and be manually testable' }
if ($workflow -notmatch 'actions/checkout@v5') { throw 'Repository heartbeat must use Node 24 checkout action' }
if ($workflow -notmatch 'rest/v1/project_keepalive' -or $workflow -notmatch 'vars.SUPABASE_PUBLISHABLE_KEY') { throw 'Keepalive query or publishable key missing' }
if ($workflow -match 'Authorization: Bearer') { throw 'Publishable keys must only use the apikey header' }
if ($workflow -match 'service_role') { throw 'Admin credential must not be used' }
if ($migration -notmatch 'enable row level security' -or $migration -notmatch 'for select to anon' -or $migration -notmatch 'grant select') { throw 'Keepalive read policy missing' }
Write-Output 'PASS: Supabase keepalive contract'
