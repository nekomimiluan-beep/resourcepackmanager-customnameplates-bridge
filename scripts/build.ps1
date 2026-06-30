param(
    [string]$ServerDir = "H:\mcziyou-1.21.1-Server",
    [string]$Version = "1.0.1"
)

$ErrorActionPreference = "Stop"

$ProjectDir = Split-Path -Parent $PSScriptRoot
$JavaHome = Join-Path $ServerDir "jdk-21"
$Javac = Join-Path $JavaHome "bin\javac.exe"
$Jar = Join-Path $JavaHome "bin\jar.exe"
$BukkitApi = Join-Path $ServerDir "libraries\com\mohistmc\installation\data\paper-remap.jar"
$ResourcePackManagerJar = Get-ChildItem -LiteralPath (Join-Path $ServerDir "plugins") -Filter "ResourcePackManager*.jar" |
    Where-Object { $_.Name -notlike "ResourcePackManagerCustomNameplatesBridge*" } |
    Select-Object -First 1 -ExpandProperty FullName
$ClassesDir = Join-Path $ProjectDir "build\classes"
$JarDir = Join-Path $ProjectDir "build\jar"
$OutputJar = Join-Path $JarDir "ResourcePackManagerCustomNameplatesBridge-$Version.jar"

foreach ($RequiredPath in @($Javac, $Jar, $BukkitApi, $ResourcePackManagerJar)) {
    if (-not (Test-Path -LiteralPath $RequiredPath)) {
        throw "缺少构建依赖：$RequiredPath"
    }
}

if (Test-Path -LiteralPath $ClassesDir) {
    Remove-Item -LiteralPath $ClassesDir -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $ClassesDir, $JarDir | Out-Null

$Sources = @(Get-ChildItem -LiteralPath (Join-Path $ProjectDir "src") -Filter "*.java" -Recurse |
    ForEach-Object { $_.FullName })

$JavacArgs = @(
    "-encoding", "UTF-8",
    "-source", "21",
    "-target", "21",
    "-cp", "$BukkitApi;$ResourcePackManagerJar",
    "-d", $ClassesDir
) + $Sources

& $Javac @JavacArgs
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

& $Jar --create --file $OutputJar `
    -C $ClassesDir . `
    -C (Join-Path $ProjectDir "resources") .
if ($LASTEXITCODE -ne 0) {
    throw "jar failed with exit code $LASTEXITCODE"
}

Write-Host "构建完成：$OutputJar"
