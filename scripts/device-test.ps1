param([string]$Serial = 'emulator-5554', [switch]$Reboot, [switch]$LifecycleOnly)
$ErrorActionPreference = 'Stop'
if (-not $Serial.StartsWith('emulator-')) { throw 'Use a dedicated emulator; this script modifies test data and permissions.' }
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot
$sdkPath = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $projectRoot '.tools\android-sdk' }
$adbPath = Join-Path $sdkPath 'platform-tools\adb.exe'
function Invoke-Adb {
    $result = & $adbPath -s $Serial @args 2>&1
    if ($LASTEXITCODE -ne 0) { throw "ADB failed: $result" }
    return $result
}
function Invoke-Instrumentation {
    $result = Invoke-Adb shell am instrument -w @args com.ckun.reminder.test/androidx.test.runner.AndroidJUnitRunner
    $text = $result -join "`n"
    Write-Output $text
    if ($text -notmatch 'OK \(' -or $text -match 'FAILURES|INSTRUMENTATION_FAILED') { throw 'Device tests failed' }
}
if (-not $LifecycleOnly) {
    Invoke-Adb install -r app/build/outputs/apk/debug/app-debug.apk
    Invoke-Adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
    Invoke-Adb shell pm grant com.ckun.reminder android.permission.POST_NOTIFICATIONS
    Invoke-Adb shell appops set com.ckun.reminder SCHEDULE_EXACT_ALARM allow
    Invoke-Instrumentation -e notClass com.ckun.reminder.LifecycleProbeTest
}
function Test-ProbeAlarms([string]$dump) {
    return [regex]::Matches($dump, '(?m)^\s*RTC_WAKEUP #\d+: Alarm\{[^\r\n]* com\.ckun\.reminder\}').Count -eq 2
}
if ($Reboot) {
    Invoke-Instrumentation -e class com.ckun.reminder.LifecycleProbeTest -e lifecyclePhase seed
    Invoke-Adb shell input keyevent KEYCODE_HOME
    Invoke-Adb shell am kill com.ckun.reminder
    $alarms = (Invoke-Adb shell dumpsys alarm) -join "`n"
    if (-not (Test-ProbeAlarms $alarms)) { throw 'Scheduled alarms missing after process exit' }
    Write-Output 'PASS: alarms retained after process exit'
    Invoke-Adb reboot
    $deadline = (Get-Date).AddMinutes(3)
    do {
        Start-Sleep -Seconds 3
        try { $boot = & $adbPath -s $Serial shell getprop sys.boot_completed 2>$null }
        catch { $boot = '' } # The device is briefly offline during reboot.
        if ((Get-Date) -gt $deadline) { throw 'Emulator reboot timed out' }
    } while ($boot -ne '1')
    Invoke-Adb shell input keyevent KEYCODE_WAKEUP
    Invoke-Adb shell input keyevent KEYCODE_MENU
    $deadline = (Get-Date).AddSeconds(30)
    do {
        $alarms = (Invoke-Adb shell dumpsys alarm) -join "`n"
        if ((Get-Date) -gt $deadline) { throw 'Alarms were not restored after boot' }
        if (-not (Test-ProbeAlarms $alarms)) { Start-Sleep -Seconds 1 }
    } while (-not (Test-ProbeAlarms $alarms))
    Write-Output 'PASS: future reminders restored after reboot'
    Invoke-Instrumentation -e class com.ckun.reminder.LifecycleProbeTest -e lifecyclePhase verify
}
