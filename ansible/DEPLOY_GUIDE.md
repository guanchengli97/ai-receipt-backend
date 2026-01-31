# AI Receipt Backend - Ansible 部署指南

## 项目结构

```
ansible/
├── deploy.yml                 # 主 playbook
├── hosts.ini                 # 主机清单
├── vars/
│   ├── main.yml             # 非敏感变量
│   └── vault.yml            # 敏感变量（加密）
└── roles/
    ├── mysql/
    │   ├── tasks/
    │   │   └── main.yml     # MySQL 安装和配置
    │   └── templates/
    └── java-app/
        ├── tasks/
        │   └── main.yml     # Java 应用部署
        └── templates/
            ├── application.yml.j2           # 应用配置模板
            └── ai-receipt-backend.service.j2 # systemd 服务模板
```

## 前置要求

### 1. 在本地控制机上安装 Ansible

```bash
# Ubuntu/Debian
sudo apt-get install ansible -y

# CentOS/RedHat
sudo yum install ansible -y

# macOS
brew install ansible
```

### 2. 验证 Ansible 安装

```bash
ansible --version
```

## 配置步骤

### 1. 配置目标主机 (hosts.ini)

编辑 `ansible/hosts.ini` 文件，添加你的服务器信息：

```ini
[webservers]
# 远程服务器示例：
production-server ansible_host=192.168.1.100 \
    ansible_user=ubuntu \
    ansible_ssh_private_key_file=~/.ssh/id_rsa \
    ansible_port=22

# 本地部署示例（如果在同一台机器）：
localhost ansible_connection=local
```

### 2. 生成和加密敏感变量

```bash
# 首次创建加密的 vault 文件
cd ansible
ansible-vault create vars/vault.yml

# 或编辑现有文件
ansible-vault edit vars/vault.yml
```

在 vault.yml 中设置：
- `vault_mysql_root_password` - MySQL root 密码
- `vault_db_password` - 数据库用户密码
- `vault_jwt_secret` - JWT 秘密密钥

### 3. 配置非敏感变量 (vars/main.yml)

编辑 `ansible/vars/main.yml` 根据需要调整：
- `db_host` - 数据库主机
- `db_port` - 数据库端口
- `app_home` - 应用安装目录
- `java_heap_min/max` - Java 堆内存大小

## 部署执行

### 方式 1：本地部署（开发环境）

```bash
cd ansible

# 仅安装 MySQL
ansible-playbook deploy.yml \
    -i hosts.ini \
    -t mysql \
    --ask-become-pass

# 仅部署 Java 应用
ansible-playbook deploy.yml \
    -i hosts.ini \
    -t java-app \
    --ask-vault-pass

# 全量部署
ansible-playbook deploy.yml \
    -i hosts.ini \
    --ask-become-pass \
    --ask-vault-pass
```

### 方式 2：远程服务器部署

```bash
cd ansible

# 指定特定主机部署
ansible-playbook deploy.yml \
    -i hosts.ini \
    -l production-server \
    --ask-become-pass \
    --ask-vault-pass

# 并行执行多个主机
ansible-playbook deploy.yml \
    -i hosts.ini \
    -f 10 \
    --ask-become-pass \
    --ask-vault-pass
```

## 部署参数说明

| 参数 | 说明 |
|------|------|
| `-i hosts.ini` | 指定主机清单文件 |
| `-t mysql` | 仅运行 mysql tag 的任务 |
| `-t java-app` | 仅运行 java-app tag 的任务 |
| `-l hostname` | 仅在指定主机上运行 |
| `--ask-become-pass` | 提示输入 sudo 密码 |
| `--ask-vault-pass` | 提示输入 vault 密码 |
| `-f 10` | 使用 10 个并行进程 |
| `-v` / `-vv` / `-vvv` | 增加详细程度 |

## 应用验证

部署完成后验证应用：

```bash
# 检查应用健康状态
curl http://localhost:8080/api/health

# 查看应用日志
ssh user@server "tail -f /opt/ai-receipt-backend/logs/app.log"

# 检查 MySQL 连接
mysql -h localhost -u receipt_user -p ai_receipt_db
```

## 常见问题

### 1. Vault 密码错误
```bash
# 改变 vault 密码
ansible-vault rekey ansible/vars/vault.yml
```

### 2. SSH 连接失败
```bash
# 检查 SSH 配置
ansible -i hosts.ini webservers -m ping

# 测试连接（添加 -v 查看详细信息）
ansible -i hosts.ini webservers -m ping -v
```

### 3. 权限不足错误
确保用户有 sudo 权限，或在 hosts.ini 中配置：
```ini
ansible_become_user=root
ansible_become_method=sudo
```

### 4. MySQL 连接问题
```bash
# 检查 MySQL 服务状态
sudo systemctl status mysql

# 检查端口占用
sudo netstat -tlnp | grep 3306
```

### 5. Java 应用无法启动
```bash
# 查看完整日志
tail -f /opt/ai-receipt-backend/logs/app.log

# 检查服务状态
sudo systemctl status ai-receipt-backend

# 查看 systemd 日志
journalctl -u ai-receipt-backend -f
```

## 手动回滚

如果需要恢复到之前的版本：

```bash
# 停止应用
sudo systemctl stop ai-receipt-backend

# 恢复旧 JAR 文件
sudo cp /opt/ai-receipt-backend/backup/ai-receipt-backend-old.jar \
    /opt/ai-receipt-backend/ai-receipt-backend.jar

# 启动应用
sudo systemctl start ai-receipt-backend
```

## 安全建议

1. **更改默认密码**：修改 vault.yml 中的所有密码
2. **SSH 密钥认证**：使用 SSH 密钥而不是密码
3. **防火墙配置**：只开放必要的端口
4. **定期更新**：定期更新系统和依赖包
5. **备份数据库**：在部署前备份重要数据

## 清理/卸载

```bash
# 停止应用
sudo systemctl stop ai-receipt-backend
sudo systemctl disable ai-receipt-backend

# 删除应用目录
sudo rm -rf /opt/ai-receipt-backend

# 删除数据库（谨慎！）
mysql -u root -p -e "DROP DATABASE ai_receipt_db; DROP USER 'receipt_user'@'localhost';"
```

## 支持的操作系统

- Ubuntu 18.04+ / Debian 10+
- CentOS 7+ / RHEL 7+
- Amazon Linux 2

## 更新日志

- v1.0 (2026-01-22): 初始版本，支持 MySQL 和 Java 应用部署
