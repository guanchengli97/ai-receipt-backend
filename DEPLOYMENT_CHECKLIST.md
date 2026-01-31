# Oracle 服务器配置检查清单

## 📋 第一次部署前的检查

### ✅ 本地环境检查

- [ ] 已安装 Ansible (`ansible --version`)
- [ ] 已安装 Git (`git --version`)
- [ ] 已配置 SSH 密钥连接到 Oracle 服务器
- [ ] SSH 密钥可以无密码登录 Oracle 服务器

```bash
# 测试 SSH 连接
ssh -i ~/.ssh/oracle_key ubuntu@<ORACLE_IP> "echo 'SSH connection OK'"
```

### ✅ Ansible 环境检查

- [ ] 编辑了 `ansible/hosts.ini`，添加了 Oracle 服务器信息
- [ ] 创建了 `ansible/vars/vault.yml` 并设置了密码
- [ ] 可以 ping 通目标服务器

```bash
# 测试 Ansible 连接
cd ansible
ansible -i hosts.ini -m ping webservers
```

### ✅ Ansible 部署 MySQL 检查

- [ ] 执行了 `ansible-playbook deploy.yml -i hosts.ini -t mysql --ask-vault-pass`
- [ ] 部署完成无错误
- [ ] 可以登录 MySQL 数据库

```bash
# 在 Oracle 服务器上验证
ssh ubuntu@<ORACLE_IP>
sudo mysql -u receipt_user -p ai_receipt_db
# 输入 Ansible vault 中设置的密码
mysql> SELECT 1;  # 应该返回 1
mysql> EXIT;
```

### ✅ Oracle 服务器基础检查

- [ ] 已安装 Docker (`docker --version`)
- [ ] 已安装 MySQL (`mysql --version`)
- [ ] MySQL 服务正在运行

```bash
ssh ubuntu@<ORACLE_IP>

# 检查 Docker
docker --version
docker ps

# 检查 MySQL
sudo systemctl status mysql

# 检查防火墙（如果有）
sudo iptables -L | grep 3306
sudo iptables -L | grep 7008
# 或对于 firewalld
sudo firewall-cmd --list-all
```

### ✅ MySQL 配置检查

- [ ] MySQL 允许本地连接（已由 Ansible 配置）
- [ ] 数据库用户和密码正确
- [ ] 数据库已创建

```bash
ssh ubuntu@<ORACLE_IP>

# 检查用户和权限
sudo mysql -u root -e "SELECT User, Host FROM mysql.user WHERE User='receipt_user';"

# 检查数据库
sudo mysql -u root -e "SHOW DATABASES LIKE 'ai_receipt%';"

# 测试用户连接
mysql -u receipt_user -p ai_receipt_db -e "SELECT 1;"
```

---

## 📋 GitHub 配置检查

### ✅ GitHub Secrets 设置检查

在仓库的 **Settings → Secrets and variables → Actions** 中，确保以下 Secrets 已创建：

- [ ] `DOCKER_USERNAME` - Docker Hub 用户名
  ```bash
  # 从 Docker Hub 获取
  echo $DOCKER_USERNAME
  ```

- [ ] `DOCKER_PASSWORD` - Docker Hub 密码/token
  ```bash
  # Docker Hub → Account Settings → Security
  ```

- [ ] `ORACLE_HOST` - Oracle 服务器 IP 或域名
  ```bash
  # 例如：140.238.xxx.xxx
  ```

- [ ] `ORACLE_USERNAME` - SSH 用户名
  ```bash
  # 通常是：ubuntu
  ```

- [ ] `SSH_PRIVATE_KEY` - SSH 私钥内容
  ```bash
  # 完整的私钥内容（包括 -----BEGIN RSA PRIVATE KEY----- 等）
  cat ~/.ssh/oracle_key
  ```

- [ ] `SSH_PORT` - SSH 端口
  ```bash
  # 通常是：22
  ```

- [ ] `DB_PASSWORD` - MySQL 用户密码
  ```bash
  # 必须与 Ansible vault.yml 中的 vault_db_password 一致！
  ```

- [ ] `JWT_SECRET` - JWT 密钥
  ```bash
  # 必须与 Ansible vault.yml 中的 vault_jwt_secret 一致！
  ```

### ✅ GitHub Workflow 文件检查

- [ ] `.github/workflows/deploy.yml` 文件存在
- [ ] 包含以下 envs 配置：
  ```yaml
  envs: DOCKER_USERNAME,DOCKER_PASSWORD,DB_PASSWORD,JWT_SECRET
  ```
- [ ] 包含 Docker run 命令的环境变量注入：
  ```yaml
  -e SPRING_DATASOURCE_PASSWORD="${DB_PASSWORD}"
  -e JWT_SECRET="${JWT_SECRET}"
  ```

---

## 📋 首次部署测试检查

### ✅ 代码推送到 master 分支

```bash
# 确保所有改动已提交
git status

# 推送到 master 分支
git push origin master

# GitHub Actions 会自动触发
```

### ✅ GitHub Actions 运行检查

在 GitHub 仓库中：

- [ ] 进入 **Actions** 标签页
- [ ] 查看最新的 workflow run
- [ ] **Build job** 应该成功完成（3-5 分钟）
  - [ ] Checkout code
  - [ ] Set up JDK 17
  - [ ] Build with Maven
  - [ ] Log in to Docker Hub
  - [ ] Build and push Docker image
- [ ] **Deploy job** 应该成功完成（1-2 分钟）
  - [ ] Deploy to Oracle Host

如果有失败，检查错误信息。

### ✅ Docker 容器运行检查

在 Oracle 服务器上验证容器：

```bash
ssh ubuntu@<ORACLE_IP>

# 检查容器是否在运行
docker ps | grep ai-receipt-app

# 检查容器日志
docker logs ai-receipt-app

# 应该看到类似的输出：
# Started AiReceiptBackendApplication in X.XXX seconds
```

### ✅ 应用健康检查

```bash
# 从 Oracle 服务器本地测试
ssh ubuntu@<ORACLE_IP>
curl http://localhost:7008/api/health

# 应该返回 200 OK
```

### ✅ 数据库连接检查

```bash
# 从容器内测试数据库连接
ssh ubuntu@<ORACLE_IP>
docker exec ai-receipt-app mysql -h127.0.0.1 -ureceipt_user -p<DB_PASSWORD> ai_receipt_db -e "SELECT 1;"

# 应该返回：
# 1
# 1
```

---

## 📋 故障排查检查清单

### 如果 GitHub Actions 构建失败

- [ ] 检查 Secrets 是否正确（特别是用户名密码）
- [ ] 查看 workflow 日志中的详细错误
- [ ] 检查 Java 版本是否正确（应该是 17）
- [ ] 检查 pom.xml 依赖是否正确

### 如果 Docker 镜像推送失败

- [ ] 验证 Docker Hub 用户名和密码正确
- [ ] 确保账户有权限推送镜像
- [ ] 查看 Docker Hub 账户是否被锁定

### 如果 SSH 部署失败

- [ ] 验证 `SSH_PRIVATE_KEY` 完整且正确格式
- [ ] 确保 `ORACLE_HOST` 和 `ORACLE_USERNAME` 正确
- [ ] 检查防火墙是否允许 SSH 连接
- [ ] SSH 密钥是否有正确的权限（600）

### 如果容器启动失败

- [ ] 检查 `DB_PASSWORD` 是否与 MySQL 用户密码一致
- [ ] 查看 `docker logs -f ai-receipt-app` 中的错误信息
- [ ] 检查 MySQL 服务是否正在运行：`sudo systemctl status mysql`
- [ ] 检查主机 IP 是否被正确获取：`hostname -I`

### 如果无法连接数据库

- [ ] 验证 MySQL 服务在运行：`sudo systemctl status mysql`
- [ ] 测试数据库连接：`mysql -h127.0.0.1 -u receipt_user -p`
- [ ] 检查防火墙是否允许 3306 端口
- [ ] 验证 GitHub Secrets 中的 DB_PASSWORD 与 vault.yml 一致

---

## 📋 定期维护检查

### ✅ 每周检查

- [ ] 应用容器正在运行：`docker ps | grep ai-receipt-app`
- [ ] 容器自动重启工作正常
- [ ] MySQL 数据库正常运行
- [ ] 无错误日志：`docker logs ai-receipt-app | grep -i error`

### ✅ 每月检查

- [ ] 检查磁盘空间：`df -h`
- [ ] 检查 Docker 镜像大小：`docker images`
- [ ] 清理旧的 Docker 镜像：`docker image prune`
- [ ] 检查数据库大小：`du -sh /var/lib/mysql`
- [ ] 备份数据库：`mysqldump -u root -p ai_receipt_db > backup.sql`

### ✅ 每季度检查

- [ ] 更新 Linux 系统：`sudo apt update && sudo apt upgrade`
- [ ] 更新 Docker：`sudo apt upgrade docker.io`
- [ ] 更新 MySQL：`sudo apt upgrade mysql-server`
- [ ] 审查 GitHub Secrets 安全性
- [ ] 轮换 SSH 密钥

---

## 🎯 快速诊断脚本

将以下脚本保存为 `diagnose.sh` 在 Oracle 服务器上运行：

```bash
#!/bin/bash

echo "=== Oracle 服务器诊断 ==="
echo ""

echo "1. Docker 状态"
docker ps -a

echo ""
echo "2. MySQL 状态"
sudo systemctl status mysql --no-pager

echo ""
echo "3. 容器日志（最后 20 行）"
docker logs --tail 20 ai-receipt-app

echo ""
echo "4. 应用健康检查"
curl -s http://localhost:7008/api/health || echo "应用不可达"

echo ""
echo "5. 数据库连接检查"
mysql -u receipt_user -p ai_receipt_db -e "SELECT NOW();" 2>/dev/null || echo "数据库连接失败"

echo ""
echo "6. 磁盘使用情况"
df -h | grep -E "^/dev"

echo ""
echo "=== 诊断完成 ==="
```

使用方式：
```bash
ssh ubuntu@<ORACLE_IP> 'bash -s' < diagnose.sh
```

---

## ✅ 部署成功标志

部署成功时应该看到：

```
✓ GitHub Actions 所有 job 成功完成
✓ Docker 容器运行中：docker ps 显示 ai-receipt-app
✓ 应用日志显示：Started AiReceiptBackendApplication
✓ 健康检查返回 200：curl http://localhost:7008/api/health
✓ 数据库连接正常：容器内可以查询数据库
✓ 无错误日志：docker logs 中无异常
```

---

**现在开始部署吧！** 🚀
