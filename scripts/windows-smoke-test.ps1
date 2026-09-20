param(
    [Parameter(Mandatory = $true)]
    [string]$ExecutablePath,
    [int]$TimeoutSeconds = 10
)

$ErrorActionPreference = 'Stop'
$resolvedExecutable = (Resolve-Path -LiteralPath $ExecutablePath).Path
$smokeDataPath = Join-Path ([IO.Path]::GetTempPath()) "lighttodo-smoke-$([Guid]::NewGuid().ToString('N')).json"
$process = Start-Process -FilePath $resolvedExecutable -Environment @{ LIGHTTODO_DATA_PATH = $smokeDataPath } -PassThru

try {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        Start-Sleep -Milliseconds 100
        $process.Refresh()
        if ($process.HasExited) {
            throw "Windows app exited before showing its main window (exit code $($process.ExitCode))."
        }
    } while ($process.MainWindowHandle -eq 0 -and (Get-Date) -lt $deadline)

    if ($process.MainWindowHandle -eq 0) {
        throw "Windows app did not show a main window within $TimeoutSeconds seconds."
    }

    Write-Host "PASS: Windows app displayed its main window."
}
finally {
    if (-not $process.HasExited) {
        Stop-Process -Id $process.Id
        $process.WaitForExit()
    }
    if (Test-Path -LiteralPath $smokeDataPath) {
        Remove-Item -LiteralPath $smokeDataPath
    }
}
