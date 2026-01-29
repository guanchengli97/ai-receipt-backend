# Ansible 部署 AI Receipt Backend 快速开始

## 📋 文件结构

```
ansible/
├── deploy.yml              # 主部署脚本
├── deploy.sh              # Linux/Mac 部署脚本
├── deploy.ps1             # Windows 部署脚本
├── hosts.ini              # 主机配置
├── DEPLOY_GUIDE.md        # 详细部署指南
├── vars/
│   ├── main.yml          # 公开变量
│   └── vault.yml         # 敏感信息（加密）
└── roles/
    ├── mysql/            # MySQL 安装角色
    │   ├── tasks/main.yml
    │   └── templates/
    └── java-app/         # Java 应用部署角色
        ├── tasks/main.yml
        └── templates/
```

## ⚡ 5 分钟快速开始

### 第 1 步：安装 Ansible

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install ansible -y

# CentOS/RHEL
sudo yum install ansible -y

# macOS
brew install ansible
```

### 第 2 步：配置目标服务器

编辑 `ansible/hosts.ini`：

```ini
[webservers]
# 本地部署（开发环境）
localhost ansible_connection=local

# 或远程服务器
# prod-server ansible_host=192.168.1.100 ansible_user=ubuntu ansible_ssh_private_key_file=~/.ssh/id_rsa
```

### 第 3 步：加密敏感变量

```bash
cd ansible
ansible-vault create vars/vault.yml
```

输入以下内容（**修改所有密码**）：

```yaml
vault_mysql_root_password: "your-secure-password"
vault_db_password: "your-db-password"
vault_jwt_secret: "your-jwt-secret-key"
```

### 第 4 步：执行部署

```bash
# Linux/Mac 使用脚本
bash deploy.sh all

# 或手动执行
ansible-playbook deploy.yml \
    -i hosts.ini \
    --ask-vault-pass \
    --ask-become-pass

# Windows PowerShell
.\deploy.ps1 -DeploymentType "all"
```

### 第 5 步：验证部署

```bash
# 检查应用健康状态
curl http://localhost:8080/api/health

# 检查数据库
mysql -u receipt_user -p -h localhost ai_receipt_db
```

## 🔑 密钥功能

### 部署选项

```bash
# 仅部署 MySQL
bash deploy.sh mysql

# 仅部署应用
bash deploy.sh java-app

# 全部部署
bash deploy.sh all
```

### 验证命令

```bash
# 检查 MySQL 服务
sudo systemctl status mysql

# 检查应用服务
sudo systemctl status ai-receipt-backend

# 查看应用日志
tail -f /opt/ai-receipt-backend/logs/app.log

# 数据库连接测试
mysql -u receipt_user -p -h localhost ai_receipt_db
```

## 📝 主要变量配置

编辑 `ansible/vars/main.yml`：

| 变量 | 默认值 | 说明 |
|-----|-------|------|
| `db_host` | localhost | 数据库主机 |
| `db_port` | 3306 | 数据库端口 |
| `db_name` | ai_receipt_db | 数据库名称 |
| `app_home` | /opt/ai-receipt-backend | 应用安装目录 |
| `java_heap_min` | 512m | Java 最小堆 |
| `java_heap_max` | 1024m | Java 最大堆 |
| `server_port` | 8080 | 应用服务端口 |

## 🐛 常见问题排查

### 问题 1：Vault 密码错误

```bash
# 重置密码
ansible-vault rekey ansible/vars/vault.yml
```

### 问题 2：SSH 连接失败

```bash
# 测试连接
ansible -i hosts.ini webservers -m ping -v
```

### 问题 3：应用启动失败

```bash
# 查看详细日志
journalctl -u ai-receipt-backend -f

# 或
tail -f /opt/ai-receipt-backend/logs/app.log
```

### 问题 4：MySQL 连接错误

```bash
# 检查 MySQL 状态
sudo systemctl status mysql

# 检查日志
sudo tail -f /var/log/mysql/error.log
```

## 🔒 安全最佳实践

1. **更改所有默认密码** - vault.yml 中的所有密码
2. **使用 SSH 密钥认证** - 不要使用密码登录
3. **限制防火墙规则** - 仅开放必要端口
4. **定期备份数据库** - 在部署前备份

```bash
# MySQL 备份
mysqldump -u receipt_user -p ai_receipt_db > backup.sql

# 恢复
mysql -u receipt_user -p ai_receipt_db < backup.sql
```

## 📊 支持的操作系统

- ✅ Ubuntu 18.04, 20.04, 22.04
- ✅ Debian 10, 11, 12
- ✅ CentOS 7, 8, 9
- ✅ RHEL 7, 8, 9
- ✅ Amazon Linux 2

## 🚀 高级用法

### 部署到多个服务器

```ini
# hosts.ini
[webservers]
server1 ansible_host=192.168.1.100
server2 ansible_host=192.168.1.101
server3 ansible_host=192.168.1.102
```

```bash
# 并行部署（10 个并发）
ansible-playbook deploy.yml -i hosts.ini -f 10 --ask-vault-pass
```

### 仅在特定主机部署

```bash
ansible-playbook deploy.yml -i hosts.ini -l server1 --ask-vault-pass
```

### 干运行（检查但不执行）

```bash
ansible-playbook deploy.yml -i hosts.ini --check
```

## 📚 详细文档

查看 [DEPLOY_GUIDE.md](./DEPLOY_GUIDE.md) 获取完整的部署指南。

## 🆘 获取帮助

```bash
# 查看所有可用的 tags
ansible-playbook deploy.yml --list-tags

# 增加详细日志输出
ansible-playbook deploy.yml -i hosts.ini -vvv --ask-vault-pass
```

## ✅ 部署清单

- [ ] 安装 Ansible
- [ ] 修改 hosts.ini 配置
- [ ] 创建并加密 vault.yml
- [ ] 修改应用配置（如需要）
- [ ] 执行部署脚本
- [ ] 验证 MySQL 连接
- [ ] 验证应用运行状态
- [ ] 测试应用功能
