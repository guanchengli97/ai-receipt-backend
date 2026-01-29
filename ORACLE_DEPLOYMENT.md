# Oracle 云服务器 - Ansible + Docker 整合部署指南

## 🏗️ 架构说明

你的部署架构是：**Ansible 部署 MySQL + GitHub Workflows 部署 Docker 应用**

```
Oracle 云服务器
├── Host OS (Linux)
│   ├── MySQL Server (by Ansible)
│   │   ├── Port: 3306
│   │   ├── Database: ai_receipt_db
│   │   └── User: receipt_user
│   │
│   └── Docker Engine
│       └── Docker Container (by GitHub Workflows)
│           ├── Java Application (Spring Boot)
│           ├── Port: 8080 (container) → 7008 (host)
│           └── Connects to Host MySQL via HOST_IP:3306
```

## 📋 部署步骤

### 第 1 步：在 Oracle 服务器上部署 MySQL（使用 Ansible）

在你的本地机器或控制机上执行：

```bash
# 1. 编辑主机配置
cd ansible
nano hosts.ini

# 添加你的 Oracle 服务器信息：
# [webservers]
# oracle-prod ansible_host=<YOUR_ORACLE_IP> ansible_user=ubuntu ansible_ssh_private_key_file=~/.ssh/oracle_key
```

```bash
# 2. 创建加密的敏感信息
ansible-vault create vars/vault.yml

# 输入以下内容（修改所有密码）：
# vault_mysql_root_password: "your-secure-password"
# vault_db_password: "receipt_user_password"  # 重要！记住这个密码
# vault_jwt_secret: "your-jwt-secret-key"
```

```bash
# 3. 执行 Ansible 部署 MySQL
ansible-playbook deploy.yml \
    -i hosts.ini \
    --ask-vault-pass \
    --ask-become-pass
```

**验证 MySQL 部署成功：**

```bash
# SSH 登录到 Oracle 服务器
ssh -i ~/.ssh/oracle_key ubuntu@<YOUR_ORACLE_IP>

# 检查 MySQL 服务
sudo systemctl status mysql

# 测试数据库连接
mysql -u receipt_user -p ai_receipt_db
```

### 第 2 步：在 GitHub 中配置密钥和密码

在 GitHub 仓库的 **Settings → Secrets and variables → Actions** 中添加以下 Secrets：

| Secret 名称 | 值 | 说明 |
|-----------|-----|------|
| `DOCKER_USERNAME` | 你的 Docker Hub 用户名 | 用于推送镜像 |
| `DOCKER_PASSWORD` | 你的 Docker Hub 密码 | 用于推送镜像 |
| `ORACLE_HOST` | Oracle 服务器 IP/域名 | 例如：140.238.xxx.xxx |
| `ORACLE_USERNAME` | Ubuntu | 默认用户 |
| `SSH_PRIVATE_KEY` | 你的 SSH 私钥内容 | 连接 Oracle 的密钥 |
| `SSH_PORT` | 22（或自定义端口） | SSH 端口 |
| `DB_PASSWORD` | 与 Ansible vault.yml 相同 | **重要！必须一致** |
| `JWT_SECRET` | 与 Ansible vault.yml 相同 | **重要！必须一致** |

⚠️ **关键点**：
- `DB_PASSWORD` 和 `JWT_SECRET` 必须与 Ansible 部署时的密码完全一致
- 这样 Docker 容器才能正确连接到主机上的 MySQL

### 第 3 步：检查 GitHub Workflows 配置

你的 GitHub Workflows 已经自动配置为：

1. **构建阶段**：在 GitHub 临时虚拟机上编译 Java 项目
2. **镜像阶段**：打包成 Docker 镜像并推送到 Docker Hub
3. **部署阶段**：SSH 登录到 Oracle 服务器，启动 Docker 容器

容器自动配置为：
- 连接到主机上的 MySQL（通过主机 IP）
- 映射端口 7008:8080（外部访问 7008，容器内 8080）
- 自动注入数据库凭证

### 第 4 步：测试部署

#### 方式 1：手动测试

```bash
# 推送代码到 master 分支触发自动部署
git add .
git commit -m "Deploy to Oracle"
git push origin master

# 在 GitHub 仓库中查看 Actions 标签页面，监控部署进度
```

#### 方式 2：SSH 登录验证

```bash
# SSH 登录到 Oracle 服务器
ssh -i ~/.ssh/oracle_key ubuntu@<YOUR_ORACLE_IP>

# 检查 Docker 容器运行状态
docker ps

# 查看容器日志
docker logs -f ai-receipt-app

# 检查容器内的数据库连接
curl http://localhost:7008/api/health

# 测试应用
curl -X GET http://localhost:7008/api/health
```

#### 方式 3：访问应用

```bash
# 从你的本地浏览器访问
http://<YOUR_ORACLE_IP>:7008/api/health
```

## 🔧 配置详解

### application.yml 中的变量替换

在 Docker 容器中，环境变量会自动注入到 application.yml 中：

```yaml
# 原配置文件 (application.yml)
spring:
  datasource:
    url: jdbc:mysql://{{ db_host }}:{{ db_port }}/{{ db_name }}
    username: {{ db_user }}
    password: {{ db_password }}
  
# Docker 启动时注入的环境变量：
# -e SPRING_DATASOURCE_URL="jdbc:mysql://<HOST_IP>:3306/ai_receipt_db..."
# -e SPRING_DATASOURCE_USERNAME="receipt_user"
# -e SPRING_DATASOURCE_PASSWORD="<DB_PASSWORD>"
```

**Docker 容器启动命令解析：**

```bash
docker run -d \
  --name ai-receipt-app \                    # 容器名称
  --restart unless-stopped \                 # 自动重启（除非手动停止）
  -p 7008:8080 \                             # 端口映射：主机 7008 → 容器 8080
  -e SPRING_DATASOURCE_URL="..." \           # 数据库连接地址
  -e SPRING_DATASOURCE_USERNAME="..." \      # 数据库用户名
  -e SPRING_DATASOURCE_PASSWORD="..." \      # 数据库密码
  -e JWT_SECRET="..." \                      # JWT 密钥
  "docker.io/<username>/ai-receipt-backend:latest"
```

## 🔐 关键配置项

### 1. MySQL 配置（Ansible）

**文件：** `ansible/vars/vault.yml`

```yaml
vault_mysql_root_password: "strong-root-password"
vault_db_password: "strong-db-user-password"    # ⭐ 记住这个
vault_jwt_secret: "long-random-jwt-secret"       # ⭐ 记住这个
```

### 2. GitHub Secrets

**位置：** GitHub → Settings → Secrets and variables → Actions

```bash
# 必须添加的 Secrets
DB_PASSWORD=<与 Ansible vault.yml 中相同>
JWT_SECRET=<与 Ansible vault.yml 中相同>
```

### 3. Docker 连接配置

**自动配置项：**
```bash
HOST_IP=$(hostname -I | awk '{print $1}')  # 自动获取主机 IP
```

这样容器就能通过主机 IP 访问 MySQL。

## 📝 常用命令

```bash
# 在 Oracle 服务器上执行

# 1. 查看 Docker 容器状态
docker ps -a

# 2. 查看容器日志
docker logs ai-receipt-app
docker logs -f ai-receipt-app  # 实时跟踪

# 3. 进入容器
docker exec -it ai-receipt-app bash

# 4. 检查应用健康状态
curl http://localhost:7008/api/health

# 5. 重启容器
docker restart ai-receipt-app

# 6. 停止容器
docker stop ai-receipt-app

# 7. 查看 MySQL 日志
sudo tail -f /var/log/mysql/error.log

# 8. 检查 MySQL 连接
mysql -u receipt_user -p ai_receipt_db -e "SELECT 1;"

# 9. 查看 MySQL 进程
sudo systemctl status mysql

# 10. 查看服务器资源使用
docker stats
```

## 🐛 故障排查

### 问题 1：Docker 容器启动失败

```bash
# 查看错误日志
docker logs ai-receipt-app

# 可能原因：
# 1. MySQL 密码错误 → 检查 DB_PASSWORD 和 vault.yml 是否一致
# 2. 主机 IP 获取失败 → 检查 hostname -I 是否返回正确 IP
# 3. 端口已占用 → 检查 7008 端口是否已被使用
```

### 问题 2：容器无法连接到 MySQL

```bash
# 从容器内测试 MySQL 连接
docker exec -it ai-receipt-app bash

# 在容器内执行
mysql -h <HOST_IP> -u receipt_user -p ai_receipt_db

# 如果连接失败，检查：
# 1. MySQL 是否在运行：sudo systemctl status mysql
# 2. MySQL 是否允许远程连接
# 3. 防火墙是否允许 3306 端口
```

### 问题 3：应用日志显示数据库连接错误

```bash
docker logs -f ai-receipt-app | grep -i "database\|connection\|error"

# 检查 application.yml 中的数据库配置是否被正确注入
docker exec ai-receipt-app env | grep SPRING_DATASOURCE
```

### 问题 4：MySQL 密码验证失败

**场景：** Ansible 部署时设置的密码与 GitHub Secrets 中的密码不匹配

**解决方案：**
```bash
# 1. 在 Oracle 服务器重置 MySQL 用户密码
sudo mysql -u root

# 2. 执行 SQL 命令
ALTER USER 'receipt_user'@'localhost' IDENTIFIED BY 'new-password';
FLUSH PRIVILEGES;

# 3. 更新 GitHub Secrets 中的 DB_PASSWORD
# GitHub → Settings → Secrets → 修改 DB_PASSWORD 值

# 4. 重新部署
git push origin master
```

## 🔄 更新和重新部署

### 方案 1：自动部署（推荐）

```bash
# 本地修改代码并提交
git add .
git commit -m "Update feature"
git push origin master

# GitHub Actions 自动构建、推送、部署
# 监控进度：GitHub → Actions
```

### 方案 2：手动重启容器

```bash
ssh -i ~/.ssh/oracle_key ubuntu@<YOUR_ORACLE_IP>

# 拉取最新镜像并重启
docker pull docker.io/<username>/ai-receipt-backend:latest
docker stop ai-receipt-app
docker rm ai-receipt-app

# 重新运行容器（参考上面的 docker run 命令）
# 或等待下次代码推送自动部署
```

## ✅ 完整部署清单

- [ ] Ansible 部署 MySQL 成功
- [ ] 验证 MySQL 数据库已创建
- [ ] GitHub Secrets 已配置正确
- [ ] DB_PASSWORD 和 JWT_SECRET 与 Ansible vault.yml 一致
- [ ] Docker Hub 镜像已上传
- [ ] GitHub Workflows 部署成功
- [ ] Docker 容器正在运行
- [ ] 应用可通过 http://IP:7008/api/health 访问
- [ ] 数据库连接正常

## 📊 部署时间线

| 步骤 | 执行位置 | 耗时 |
|------|--------|------|
| MySQL 部署 | Ansible（本地→Oracle） | 3-5 分钟 |
| 代码推送 | Git | 1 分钟 |
| Java 编译 | GitHub Actions | 3-5 分钟 |
| 镜像构建 | GitHub Actions | 2-3 分钟 |
| 镜像推送 | GitHub Actions | 1-2 分钟 |
| 容器启动 | Oracle 服务器 | 1-2 分钟 |
| **总计** | - | **12-18 分钟** |

## 🔑 Secrets 清单

必须在 GitHub 中配置的 Secrets：

```
DOCKER_USERNAME        ← Docker Hub 用户名
DOCKER_PASSWORD        ← Docker Hub 密码
ORACLE_HOST            ← Oracle 服务器 IP
ORACLE_USERNAME        ← 通常是 ubuntu
SSH_PRIVATE_KEY        ← SSH 私钥（完整内容）
SSH_PORT               ← SSH 端口（默认 22）
DB_PASSWORD            ← MySQL 用户密码（与 Ansible 一致）
JWT_SECRET             ← JWT 密钥（与 Ansible 一致）
```

## 🎯 架构优势

✅ **自动化完整**: 代码推送即自动部署  
✅ **安全性高**: 敏感信息加密存储  
✅ **易于管理**: Docker 容器易扩展  
✅ **数据持久**: MySQL 运行在主机，数据持久化  
✅ **灵活扩展**: 可轻松添加多个应用容器或数据库实例

---

**现在你可以开始部署了！** 🚀
