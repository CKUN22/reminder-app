param([switch]$DeviceTests)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot
if (-not $env:JAVA_HOME -and (Test-Path 'C:\Program Files\Java\jdk-17')) { $env:JAVA_HOME = 'C:\Program Files\Java\jdk-17' }
if (-not $env:ANDROID_HOME -and (Test-Path '.tools\android-sdk')) { $env:ANDROID_HOME = Join-Path $projectRoot '.tools\android-sdk' }
if (-not $env:ANDROID_HOME) { throw 'Set ANDROID_HOME to an Android SDK containing platform 35 and build-tools 35.0.0.' }
$env:GRADLE_USER_HOME = Join-Path $projectRoot '.tools\gradle-home'
& (Join-Path $PSScriptRoot 'test.ps1')
$gradleCommand = if (Test-Path '.tools\gradle-8.11.1\bin\gradle.bat') { '.tools\gradle-8.11.1\bin\gradle.bat' } else { '.\gradlew.bat' }
& $gradleCommand assembleDebug lintDebug testReminderRules testAgendaRules assembleDebugAndroidTest --console=plain
if ($LASTEXITCODE -ne 0) { throw 'Android build or validation failed' }
if ($DeviceTests) {
    & $gradleCommand connectedDebugAndroidTest --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Device tests failed' }
}
