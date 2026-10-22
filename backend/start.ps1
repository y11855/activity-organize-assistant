# Activity Assistant backend startup script
# Sets JDK 21, loads local secrets (Jasypt master key + DashScope API key), then starts Spring Boot.
# Usage: in PowerShell run  .\start.ps1
#
# Local secrets are read from start.local.ps1 (git-ignored).
# Copy start.local.ps1.example to start.local.ps1 and fill in your own values.

# 1. Use JDK 21 (avoids stale JAVA_HOME cached in terminal)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# 2. Make sure Maven is callable - some terminals (e.g. fresh admin PowerShell) lack it on PATH
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    $mavenBin = $null
    if ($env:MAVEN_HOME -and (Test-Path (Join-Path $env:MAVEN_HOME "bin\mvn.cmd"))) {
        $mavenBin = Join-Path $env:MAVEN_HOME "bin"
    }
    if (-not $mavenBin) {
        foreach ($root in @("C:\", "D:\", "E:\")) {
            if (-not (Test-Path $root)) { continue }
            foreach ($dir in Get-ChildItem -Path $root -Directory -Filter "apache-maven*" -ErrorAction SilentlyContinue) {
                if (Test-Path (Join-Path $dir.FullName "bin\mvn.cmd")) {
                    $mavenBin = Join-Path $dir.FullName "bin"
                    break
                }
                foreach ($sub in Get-ChildItem -Path $dir.FullName -Directory -Filter "apache-maven*" -ErrorAction SilentlyContinue) {
                    if (Test-Path (Join-Path $sub.FullName "bin\mvn.cmd")) {
                        $mavenBin = Join-Path $sub.FullName "bin"
                        break
                    }
                }
                if ($mavenBin) { break }
            }
            if ($mavenBin) { break }
        }
    }
    if ($mavenBin) {
        $env:PATH = "$mavenBin;$env:PATH"
        Write-Host "[Config] Maven found at $mavenBin" -ForegroundColor Green
    }
}
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host "[ERROR] Maven not found. Install Maven and add its bin directory to PATH." -ForegroundColor Red
    Write-Host '        Example: setx MAVEN_HOME "D:\apache-maven-3.9.16"  then reopen the terminal.' -ForegroundColor Yellow
    exit 1
}

# 3. Load local secrets - never hardcode real keys in this file
$localSecrets = Join-Path $PSScriptRoot "start.local.ps1"
if (Test-Path $localSecrets) {
    . $localSecrets
    Write-Host "[Config] Local secrets loaded from start.local.ps1" -ForegroundColor Green
} else {
    Write-Host "[WARN] start.local.ps1 not found. Copy start.local.ps1.example -> start.local.ps1 and fill in keys." -ForegroundColor Yellow
}

if ([string]::IsNullOrWhiteSpace($env:DASHSCOPE_API_KEY) -or $env:DASHSCOPE_API_KEY -like "sk-xxxx*") {
    Write-Host "[WARN] No valid DashScope API key found. AI planning will use fallback." -ForegroundColor Yellow
} else {
    Write-Host "[AI] DashScope API key loaded." -ForegroundColor Green
}

Write-Host "JAVA_HOME = $env:JAVA_HOME" -ForegroundColor Cyan
Write-Host "Starting activity-assistant backend..." -ForegroundColor Green

# 4. Start the app
mvn spring-boot:run
