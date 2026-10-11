# 活动管家后端启动脚本
# 作用：指定 JDK 21 + 注入 Jasypt 配置加密主密钥，然后启动 Spring Boot 应用
# 用法：在 PowerShell 中执行  .\start.ps1

# 1. 指定 JDK 21（避免终端缓存旧 JDK）
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# 2. 注入 Jasypt 主密钥（用于解密 application-dev.yml 中的 ENC(...) 密文）
#    该密钥仅用于本地开发/演示，正式部署应通过安全的环境变量或密钥管理服务注入
$env:JASYPT_ENCRYPTOR_PASSWORD = "activity-assistant-2026-secure"

Write-Host "JAVA_HOME = $env:JAVA_HOME" -ForegroundColor Cyan
Write-Host "Starting activity-assistant backend..." -ForegroundColor Green

# 3. 启动应用
mvn spring-boot:run
