# Oracle 云部署 - 快速参考卡片

## 🚀 3 步快速开始

### 第 1 步：Ansible 部署 MySQL
```bash
cd ansible
ansible-vault create vars/vault.yml
# 输入密码信息

ansible-playbook deploy.yml -i hosts.ini --ask-vault-pass
```

### 第 2 步：GitHub 配置 Secrets
在 GitHub Settings → Secrets 中添加：
```
DB_PASSWORD         = <与 Ansible 相同>
JWT_SECRET          = <与 Ansible 相同>
ORACLE_HOST         = 你的 Oracle IP
ORACLE_USERNAME     = ubuntu
SSH_PRIVATE_KEY     = 你的 SSH 密钥
DOCKER_USERNAME     = Docker Hub 用户名
DOCKER_PASSWORD     = Docker Hub 密码
```

### 第 3 步：推送代码自动部署
```bash
git push origin master
# GitHub Actions 自动构建、推送镜像、启动容器
```

## 🔑 关键配置对应关系

```
Ansible vault.yml
├── vault_db_password ─────────────→ GitHub Secret: DB_PASSWORD
└── vault_jwt_secret ──────────────→ GitHub Secret: JWT_SECRET

GitHub Secrets → Docker 环境变量
├── DB_PASSWORD ─────→ -e SPRING_DATASOURCE_PASSWORD
└── JWT_SECRET ──────→ -e JWT_SECRET
```

⚠️ **三个地方必须保持一致！**

## 📡 数据流向

```
你的代码
    ↓
GitHub (git push)
    ↓
GitHub Actions (编译 + 打包 Docker 镜像)
    ↓
Docker Hub (推送镜像)
    ↓
Oracle 服务器 (SSH 拉取镜像 + 启动容器)
    ↓
Docker 容器 (Java 应用)
    ↓
主机 MySQL (3306)
```

## 🐳 Docker 容器配置

### 端口映射
```
外部访问: http://IP:7008
容器内部: http://localhost:8080
```

### 数据库连接
```
容器内: jdbc:mysql://<HOST_IP>:3306/ai_receipt_db
用户名: receipt_user
密码: <DB_PASSWORD>
```

## 📊 服务器上的架构

```
Oracle 服务器
│
├─ MySQL (systemd 服务)
│  ├─ 端口: 3306
│  ├─ 数据库: ai_receipt_db
│  └─ 用户: receipt_user
│
└─ Docker 容器 (ai-receipt-app)
   ├─ 端口映射: 7008:8080
   ├─ 连接到: HOST_IP:3306
   └─ 自动重启: yes
```

## 🔍 验证命令

```bash
# 检查 MySQL
sudo systemctl status mysql
mysql -u receipt_user -p ai_receipt_db

# 检查 Docker
docker ps
docker logs -f ai-receipt-app

# 测试应用
curl http://localhost:7008/api/health
```

## ❌ 常见错误排查

| 错误 | 原因 | 解决方案 |
|------|------|--------|
| 容器启动失败 | MySQL 密码错误 | 确保 DB_PASSWORD 与 Ansible 一致 |
| 无法连接数据库 | 获取主机 IP 失败 | 检查 `hostname -I` 输出 |
| 端口冲突 | 7008 已被占用 | 修改端口映射或停止占用进程 |
| 镜像拉取失败 | Docker 凭证错误 | 检查 DOCKER_USERNAME 和 PASSWORD |

## 📝 GitHub Workflow 执行时间

```
准备阶段 ─┐
          ├─ 编译 (3-5 分钟)
构建阶段 ─┤
          ├─ 打包 Docker (2-3 分钟)
          └─ 推送镜像 (1-2 分钟)
          
部署阶段 ──── SSH 启动容器 (1-2 分钟)

总耗时: 7-12 分钟
```

## 🎯 目录结构参考

```
ai-receipt-backend/
├── .github/workflows/deploy.yml    ← GitHub CI/CD 配置
├── ansible/
│   ├── deploy.yml                  ← Ansible 主文件
│   ├── hosts.ini                   ← 目标服务器配置
│   ├── vars/vault.yml              ← 加密密码（需创建）
│   └── roles/                       ← MySQL 部署脚本
├── src/
│   └── main/resources/
│       └── application.yml         ← Spring Boot 配置
└── ORACLE_DEPLOYMENT.md            ← 完整指南（本文件）
```

## 💡 快速操作

### 修改应用并重新部署
```bash
# 本地修改代码
nano src/main/...

# 提交并推送（自动部署）
git add .
git commit -m "fix: xxxx"
git push origin master

# 查看部署进度
# GitHub Actions 页面中监控
```

### 手动重启应用
```bash
ssh ubuntu@<ORACLE_IP>
docker restart ai-receipt-app
```

### 查看实时日志
```bash
ssh ubuntu@<ORACLE_IP>
docker logs -f ai-receipt-app
```

### 紧急回滚
```bash
ssh ubuntu@<ORACLE_IP>
docker pull <previous_image_tag>
docker stop ai-receipt-app
docker rm ai-receipt-app
docker run ... # 运行旧镜像
```

## 📞 需要帮助？

查看详细文档：
- [ORACLE_DEPLOYMENT.md](./ORACLE_DEPLOYMENT.md) - 完整部署指南
- [ANSIBLE_DEPLOYMENT.md](./ANSIBLE_DEPLOYMENT.md) - Ansible 说明
- [.github/workflows/deploy.yml](./.github/workflows/deploy.yml) - Workflow 配置

---

**记住：DB_PASSWORD 和 JWT_SECRET 必须在三个地方保持一致！** ✅
