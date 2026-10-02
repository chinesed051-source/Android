$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$Tools = Join-Path $ProjectRoot '.tools'
New-Item -ItemType Directory -Force -Path $Tools | Out-Null

function Find-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) { return $env:JAVA_HOME }
    $candidates = @(
        "$env:ProgramFiles\Android\Android Studio\jbr",
        "$env:ProgramFiles\Android\Android Studio\jre",
        "$env:LOCALAPPDATA\Programs\Android Studio\jbr"
    )
    foreach ($p in $candidates) { if (Test-Path (Join-Path $p 'bin\java.exe')) { return $p } }
    return $null
}

$JavaHome = Find-JavaHome
if (-not $JavaHome) {
    Write-Host '未找到 Java。正在下载 Temurin JDK 17...' -ForegroundColor Yellow
    $jdkZip = Join-Path $Tools 'jdk17.zip'
    Invoke-WebRequest -Uri 'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse' -OutFile $jdkZip -UseBasicParsing
    $jdkDir = Join-Path $Tools 'jdk17'
    if (Test-Path $jdkDir) { Remove-Item $jdkDir -Recurse -Force }
    New-Item -ItemType Directory -Force -Path $jdkDir | Out-Null
    Expand-Archive -Path $jdkZip -DestinationPath $jdkDir -Force
    $sub = Get-ChildItem $jdkDir -Directory | Select-Object -First 1
    if (-not $sub) { throw 'JDK 解压失败' }
    $JavaHome = $sub.FullName
}
$env:JAVA_HOME = $JavaHome
$env:Path = "$JavaHome\bin;$env:Path"
Write-Host "JAVA_HOME = $JavaHome" -ForegroundColor Cyan

$SdkRoot = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } elseif ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
$env:ANDROID_SDK_ROOT = $SdkRoot
$env:ANDROID_HOME = $SdkRoot
New-Item -ItemType Directory -Force -Path $SdkRoot | Out-Null

$SdkManager = Join-Path $SdkRoot 'cmdline-tools\latest\bin\sdkmanager.bat'
if (-not (Test-Path $SdkManager)) {
    Write-Host '正在安装 Android Command-line Tools...' -ForegroundColor Yellow
    $cmdZip = Join-Path $Tools 'cmdline-tools.zip'
    Invoke-WebRequest -Uri 'https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip' -OutFile $cmdZip -UseBasicParsing
    $temp = Join-Path $Tools 'cmdline-unpack'
    if (Test-Path $temp) { Remove-Item $temp -Recurse -Force }
    Expand-Archive -Path $cmdZip -DestinationPath $temp -Force
    $latest = Join-Path $SdkRoot 'cmdline-tools\latest'
    New-Item -ItemType Directory -Force -Path $latest | Out-Null
    Copy-Item (Join-Path $temp 'cmdline-tools\*') $latest -Recurse -Force
}

Write-Host '正在准备 Android SDK 35...' -ForegroundColor Yellow
1..200 | ForEach-Object { 'y' } | & $SdkManager --licenses | Out-Null
& $SdkManager 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0'
if ($LASTEXITCODE -ne 0) { throw 'Android SDK 安装失败' }

$GradleDir = Join-Path $Tools 'gradle-8.9'
$GradleExe = Join-Path $GradleDir 'bin\gradle.bat'
if (-not (Test-Path $GradleExe)) {
    Write-Host '正在下载 Gradle 8.9...' -ForegroundColor Yellow
    $gradleZip = Join-Path $Tools 'gradle-8.9-bin.zip'
    Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.9-bin.zip' -OutFile $gradleZip -UseBasicParsing
    Expand-Archive -Path $gradleZip -DestinationPath $Tools -Force
}

Write-Host '开始编译净屏 APK...' -ForegroundColor Green
Push-Location $ProjectRoot
try {
    & $GradleExe --no-daemon --stacktrace :app:assembleDebug
    if ($LASTEXITCODE -ne 0) { throw "Gradle 编译失败，退出码 $LASTEXITCODE" }
} finally {
    Pop-Location
}

$Apk = Join-Path $ProjectRoot 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path $Apk)) { throw '编译完成但没有找到 APK' }
$Out = Join-Path $ProjectRoot 'JingPing-v0.2-debug.apk'
Copy-Item $Apk $Out -Force
Write-Host "`n编译成功：$Out" -ForegroundColor Green
Start-Process explorer.exe "/select,`"$Out`""
