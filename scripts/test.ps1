$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$testOutput = Join-Path $projectRoot '.tools\rule-tests'
New-Item -ItemType Directory -Force $testOutput | Out-Null
$javaBin = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin' } else { 'C:\Program Files\Java\jdk-17\bin' }
& (Join-Path $javaBin 'javac.exe') -encoding UTF-8 -d $testOutput (Join-Path $projectRoot 'app\src\main\java\com\ckun\reminder\Task.java') (Join-Path $projectRoot 'app\src\main\java\com\ckun\reminder\ReminderRules.java') (Join-Path $projectRoot 'tests\ReminderRulesTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
& (Join-Path $javaBin 'java.exe') -cp $testOutput com.ckun.reminder.ReminderRulesTest
if ($LASTEXITCODE -ne 0) { throw 'Rule tests failed' }
& (Join-Path $javaBin 'javac.exe') -encoding UTF-8 -cp $testOutput -d $testOutput (Join-Path $projectRoot 'app\src\main\java\com\ckun\reminder\Statistics.java') (Join-Path $projectRoot 'tests\StatisticsTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Statistics test compilation failed' }
& (Join-Path $javaBin 'java.exe') -cp $testOutput com.ckun.reminder.StatisticsTest
if ($LASTEXITCODE -ne 0) { throw 'Statistics tests failed' }
[xml]$manifest = Get-Content -Raw -Encoding UTF8 (Join-Path $projectRoot 'app\src\main\AndroidManifest.xml')
$androidNamespace = 'http://schemas.android.com/apk/res/android'
$permissions = @($manifest.manifest.'uses-permission' | ForEach-Object { $_.GetAttribute('name', $androidNamespace) })
if ($permissions -contains 'android.permission.INTERNET') { throw 'Offline app must not request INTERNET permission' }
if ($manifest.manifest.application.GetAttribute('allowBackup', $androidNamespace) -ne 'false') { throw 'Cloud backup must be disabled' }
Write-Output 'PASS: offline manifest checks'
