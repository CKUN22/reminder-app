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
$apksigner = Join-Path $env:ANDROID_HOME 'build-tools\35.0.0\apksigner.bat'
$certs = (& $apksigner verify --print-certs 'app\build\outputs\apk\debug\app-debug.apk') -join "`n"
if ($certs -notmatch '85a1257848aae3ebbd307e46071e5cb0fee4dd061a4181064e7dcc16de131135') { throw 'APK is not signed with the historical upgrade certificate.' }
Write-Output 'PASS: historical Android signing certificate'
if ($DeviceTests) {
    & $gradleCommand connectedDebugAndroidTest --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Device tests failed' }
}
