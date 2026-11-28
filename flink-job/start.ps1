# Flink 推荐作业启动脚本（本地迷你集群）
# 用法：
#   .\start.ps1            直接启动（首次会自动编译并生成依赖 classpath）
#   .\start.ps1 -Rebuild   重新打包后再启动（修改了 flink-job 代码时使用）
#
# 说明：本机未安装 Flink 发行版，因此不依赖 `flink run`；
# 作业以本地迷你集群方式运行，输出写入 MySQL 的 user_recommendation 表。

param([switch]$Rebuild)

$ErrorActionPreference = "Stop"

# 1. JDK 21
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# 2. 确保 mvn 可用
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    foreach ($root in @("C:\", "D:\", "E:\")) {
        if (-not (Test-Path $root)) { continue }
        foreach ($dir in Get-ChildItem -Path $root -Directory -Filter "apache-maven*" -ErrorAction SilentlyContinue) {
            foreach ($candidate in @($dir.FullName) + (Get-ChildItem -Path $dir.FullName -Directory -Filter "apache-maven*" -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName })) {
                if (Test-Path (Join-Path $candidate "bin\mvn.cmd")) {
                    $env:PATH = "$(Join-Path $candidate 'bin');$env:PATH"
                    break
                }
            }
            if (Get-Command mvn -ErrorAction SilentlyContinue) { break }
        }
        if (Get-Command mvn -ErrorAction SilentlyContinue) { break }
    }
}
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host "[ERROR] 未找到 Maven，请安装并配置 PATH" -ForegroundColor Red
    exit 1
}

# 3. 按需编译 / 打包
if ($Rebuild) {
    Write-Host "重新打包 Flink 作业..." -ForegroundColor Cyan
    mvn -B -DskipTests package
} elseif (-not (Test-Path "target\classes")) {
    Write-Host "首次运行，编译 Flink 作业..." -ForegroundColor Cyan
    mvn -B -q -DskipTests compile
}

# 4. 生成依赖 classpath（含 provided 依赖，本地运行需要）
if (-not (Test-Path "target\cp.txt")) {
    Write-Host "解析依赖 classpath..." -ForegroundColor Cyan
    mvn -B -q dependency:build-classpath "-Dmdep.outputFile=target\cp.txt" "-Dmdep.includeScope=test"
}

Write-Host "启动 Flink 推荐作业（Ctrl+C 停止）..." -ForegroundColor Green
$cp = (Get-Content "target\cp.txt" -Raw).Trim()
& "$env:JAVA_HOME\bin\java.exe" -cp "target\classes;$cp" com.aiapp.flink.UserBehaviorRecommendationJob
