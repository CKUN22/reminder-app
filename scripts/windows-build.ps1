$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$env:DOTNET_CLI_HOME = Join-Path $root '.tools/dotnet-home'
$env:DOTNET_SKIP_FIRST_TIME_EXPERIENCE = '1'
dotnet build "$root/windows/LightTodo.Windows.Tests/LightTodo.Windows.Tests.csproj" -c Release --ignore-failed-sources
dotnet run --project "$root/windows/LightTodo.Windows.Tests/LightTodo.Windows.Tests.csproj" -c Release --no-build
dotnet publish "$root/windows/LightTodo.Windows/LightTodo.Windows.csproj" -c Release --self-contained false --no-restore -o "$root/dist/windows"
& "$PSScriptRoot/windows-smoke-test.ps1" -ExecutablePath "$root/dist/windows/LightTodo.exe"
Write-Host "Windows app: $root/dist/windows/LightTodo.exe"
