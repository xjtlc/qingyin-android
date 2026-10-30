$ErrorActionPreference = 'Stop'
$projectDirectory = $PSScriptRoot
$javaDirectory = $env:JAVA_HOME
if (-not $javaDirectory) { throw 'Set JAVA_HOME to an installed JDK 17 directory.' }
if (-not (Test-Path (Join-Path $javaDirectory 'bin\java.exe'))) { throw 'JAVA_HOME does not contain bin\java.exe.' }

$sdkDirectory = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { $env:ANDROID_HOME }
if (-not $sdkDirectory -or -not (Test-Path $sdkDirectory)) { throw 'Set ANDROID_SDK_ROOT to the Android SDK directory.' }

$wrapper = Join-Path $projectDirectory 'gradlew.bat'
if (Test-Path $wrapper) {
    $gradleExecutable = $wrapper
} elseif ($env:GRADLE_HOME -and (Test-Path (Join-Path $env:GRADLE_HOME 'bin\gradle.bat'))) {
    $gradleExecutable = Join-Path $env:GRADLE_HOME 'bin\gradle.bat'
} else {
    $gradleCommand = Get-Command gradle.bat -ErrorAction SilentlyContinue
    if (-not $gradleCommand) { throw 'Install Gradle 8.7 and set GRADLE_HOME or add gradle.bat to PATH.' }
    $gradleExecutable = $gradleCommand.Source
}

$env:JAVA_HOME = $javaDirectory
$env:ANDROID_HOME = $sdkDirectory
$env:ANDROID_SDK_ROOT = $sdkDirectory
$env:PATH = (Join-Path $javaDirectory 'bin') + ';' + $env:PATH

Push-Location -LiteralPath $projectDirectory
try {
    & $gradleExecutable :app:assembleDebug --offline --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
    Write-Output (Join-Path $projectDirectory 'app\build\outputs\apk\debug\app-debug.apk')
} finally {
    Pop-Location
}
