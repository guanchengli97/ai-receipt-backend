# Windows 快速部署脚本
# 用法: .\deploy.ps1 [-DeploymentType "all|mysql|java-app"] [-VaultPassword "password"]

param(
    [ValidateSet("mysql")]
    [string]$DeploymentType = "mysql",
    [string]$VaultPassword = ""
)

$ErrorActionPreference = "Stop"

# 颜色输出
function Write-Status {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Green
}

function Write-Error-Custom {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
    exit 1
}

function Write-Warning-Custom {
    param([string]$Message)
    Write-Host "[WARNING] $Message" -ForegroundColor Yellow
}

# 检查 Ansible 是否安装
try {
    ansible-playbook --version | Out-Null
}
catch {
    Write-Error-Custom "Ansible 未安装。请先安装 Ansible。"
}

Write-Status "开始部署类型: $DeploymentType"

# 构建命令
$cmd = @(
    "ansible-playbook", "deploy.yml",
    "-i", "hosts.ini"
)

if ($VaultPassword) {
    # 将密码保存到临时文件
    $VaultFile = New-TemporaryFile
    $VaultPassword | Set-Content $VaultFile
    $cmd += "--vault-password-file=$($VaultFile.FullName)"
}
else {
    $cmd += "--ask-vault-pass"
}

# 添加标签
switch ($DeploymentType) {
    "mysql" {
        Write-Status "部署 MySQL..."
    }
}

$cmd += "--ask-become-pass"

Write-Status "执行命令: $($cmd -join ' ')"
& $cmd

Write-Status "部署完成！"

# 清理临时文件
if ($VaultFile) {
    Remove-Item $VaultFile -Force -ErrorAction SilentlyContinue
}
