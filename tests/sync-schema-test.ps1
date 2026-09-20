$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$path = Join-Path $root 'supabase\migrations\202609200001_sync_core.sql'
$sql = Get-Content -Raw -Encoding UTF8 $path
foreach ($table in 'tasks','goals','courses') {
    if ($sql -notmatch "create table if not exists public\.$table") { throw "Missing $table table" }
    if ($sql -notmatch "alter table public\.$table enable row level security") { throw "RLS missing for $table" }
    if ($sql -notmatch "${table}_owner_select") { throw "Select policy missing for $table" }
    if ($sql -notmatch "${table}_owner_insert") { throw "Insert policy missing for $table" }
    if ($sql -notmatch "${table}_owner_update") { throw "Update policy missing for $table" }
    if ($sql -notmatch "${table}_user_sync_idx") { throw "Sync index missing for $table" }
}
foreach ($required in 'auth.uid()','sync_version','sync_seq','deleted_at','prepare_sync_row','pull_changes','security invoker','to authenticated') {
    if (-not $sql.Contains($required)) { throw "Sync schema missing: $required" }
}
if ($sql -match 'service_role') { throw 'Client schema must not grant service_role access' }
Write-Output 'PASS: Supabase sync schema contract'
