param(
    [switch]$AppImageOnly
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$appName = "MyBeautifulGit-chan"
$appVersion = if ($env:APP_VERSION) { $env:APP_VERSION } else { "1.0.0" }
$packageVersion = if ($env:PACKAGE_VERSION) { $env:PACKAGE_VERSION } else { "1.0.0" }
$channel = if ($env:RELEASE_CHANNEL) { $env:RELEASE_CHANNEL } else { "chan" }
$dist = Join-Path $root "dist"
$inputDir = Join-Path $root "build\packaging\input"
$jar = Join-Path $root "build\tasks\_desktopApp_executableJarJvm\desktopApp-jvm-executable.jar"
$icon = Join-Path $root "packaging\icon.ico"

if (Test-Path $dist) { Remove-Item -Recurse -Force $dist }
New-Item -ItemType Directory -Force -Path $dist, $inputDir | Out-Null

Push-Location $root
try {
    & "$root\kotlin.bat" package -m desktopApp -f executable-jar
    if ($LASTEXITCODE -ne 0) { throw "Amper package failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}
if (-not (Test-Path $jar)) { throw "Executable jar not found: $jar" }

$python = Get-Command "python" -ErrorAction SilentlyContinue
if ($null -eq $python) { $python = Get-Command "py" -ErrorAction SilentlyContinue }
if ($null -eq $python) { throw "Python 3 is required to prepare the jpackage input" }
$pythonPrefix = if ($python.Name -eq "py.exe") { @("-3") } else { @() }
& $python.Source @pythonPrefix "$root\packaging\prepare-runtime.py" --jar $jar --output $inputDir
if ($LASTEXITCODE -ne 0) { throw "Could not prepare jpackage input (exit code $LASTEXITCODE)" }

$jpackage = if ($env:JAVA_HOME) {
    Join-Path $env:JAVA_HOME "bin\jpackage.exe"
} else {
    (Get-Command "jpackage.exe" -ErrorAction Stop).Source
}
if (-not (Test-Path $jpackage)) { throw "jpackage not found: $jpackage" }
$jpackageVersion = (& $jpackage --version).Trim()
if ([version]($jpackageVersion.Split("+")[0]) -lt [version]"25.0") {
    throw "JDK 25+ is required for packaging (found jpackage $jpackageVersion)"
}

$modules = @(
    "java.base",
    "java.desktop",
    "java.logging",
    "java.management",
    "java.naming",
    "java.net.http",
    "java.prefs",
    "java.security.jgss",
    "java.sql",
    "java.xml",
    "jdk.accessibility",
    "jdk.charsets",
    "jdk.crypto.ec",
    "jdk.localedata",
    "jdk.unsupported",
    "jdk.zipfs"
) -join ","

$common = @(
    "--name", $appName,
    "--app-version", $packageVersion,
    "--vendor", "Baconana-chan",
    "--description", "$appName - terminal-flavored Git client",
    "--copyright", "GPL-2.0-only",
    "--input", $inputDir,
    "--main-jar", "desktopApp-jvm.jar",
    "--main-class", "org.baconana.mybeautifulgit_chan.MainKt",
    "--dest", $dist,
    "--java-options", "-Dfile.encoding=UTF-8",
    "--java-options", "-Xmx2048m",
    "--add-modules", $modules
)
if (Test-Path $icon) { $common += @("--icon", $icon) }

$formats = if ($AppImageOnly) { @("app-image") } else { @("msi", "exe") }
foreach ($format in $formats) {
    $jpackageArgs = @("--type", $format) + $common
    if ($format -ne "app-image") {
        $jpackageArgs += @(
            "--win-menu",
            "--win-menu-group", $appName,
            "--win-shortcut",
            "--win-dir-chooser",
            "--win-upgrade-uuid", "2b6e0f7a-6e5c-4c1d-9a4d-0f6c7b8a9d21",
            # main.wxs override: same-version upgrades must replace the installed product.
            "--resource-dir", (Join-Path $PSScriptRoot "wix")
        )
    }
    & $jpackage @jpackageArgs
    if ($LASTEXITCODE -ne 0) { throw "jpackage --type $format failed with exit code $LASTEXITCODE" }
}

if (-not $AppImageOnly) {
    foreach ($format in @("msi", "exe")) {
        $file = Get-ChildItem -Path $dist -File -Filter "*.$format" | Select-Object -First 1
        if ($null -eq $file) { throw "jpackage did not produce a .$format file" }
        $target = Join-Path $dist "$appName-$appVersion-windows-x64.$format"
        if ($file.FullName -ne $target) {
            Move-Item -Force $file.FullName $target
        }
    }
}

$statusFile = Join-Path $dist "PACKAGE-STATUS.txt"
$statusFormats = if ($AppImageOnly) { "app-image" } else { "msi, exe" }
@(
    "app: $appName",
    "version: $appVersion",
    "package_version: $packageVersion",
    "channel: $channel",
    "platform: Windows x64",
    "formats: $statusFormats"
) | Set-Content -Encoding ascii $statusFile

$checksumFile = Join-Path $dist "SHA256SUMS"
$files = Get-ChildItem -Path $dist -File | Where-Object { $_.Name -ne "SHA256SUMS" }
$lines = foreach ($file in $files) {
    $hash = (Get-FileHash -Algorithm SHA256 $file.FullName).Hash.ToLowerInvariant()
    "$hash  $($file.Name)"
}
$lines | Set-Content -Encoding ascii $checksumFile

Write-Host "Built $appName $appVersion ($channel) in $dist"
Get-ChildItem $dist -File | Select-Object Name, Length
