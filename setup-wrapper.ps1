$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$target = Join-Path $projectRoot "gradle\wrapper\gradle-wrapper.jar"
$url = "https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle/raw/refs/heads/main/gradle/wrapper/gradle-wrapper.jar"

New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
Write-Host "Downloading official NeoForge 1.21.1 Gradle wrapper..."
Invoke-WebRequest -Uri $url -OutFile $target
Write-Host "Saved: $target"
