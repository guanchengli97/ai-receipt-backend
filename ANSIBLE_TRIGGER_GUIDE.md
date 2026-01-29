# Ansible 触发机制详解

## 🎯 两种部署方式

你现在有两种方式部署 Ansible MySQL：

### 方式 1：手动触发（现有）✅

**最简单，推荐首次部署使用**

```bash
# 在你的本地电脑执行
cd ansible
ansible-playbook deploy.yml -i hosts.ini -t mysql --ask-vault-pass
```

**特点：**
- ✅ 完全控制，随时部署
- ✅ 可以看到实时输出
- ✅ 失败可以立即修改重试
- ❌ 需要本地安装 Ansible
- ❌ 需要本地配置 SSH 密钥

**适用场景：**
- 首次部署 MySQL（一次性操作）
- 快速调试和测试
- 离线环境

---

### 方式 2：GitHub Actions 自动触发（新增）✨

**按需手动触发或自动触发**

#### 2A. 手动触发（按钮点击）

1. 进入 GitHub 仓库 → **Actions** 标签页
2. 左侧选择 **"Deploy Ansible MySQL"**
3. 点击 **"Run workflow"** 按钮
4. 选择部署目标（mysql / java-app / all）
5. 点击 **"Run workflow"** 确认

```
GitHub Actions UI
    ↓
按 "Run workflow" 按钮
    ↓
输入参数选择 (mysql)
    ↓
自动执行 Ansible
```

**优点：**
- ✅ 无需本地 Ansible 环境
- ✅ 无需本地配置 SSH
- ✅ 可在任何地方触发（只需 GitHub 账号）
- ✅ 自动记录执行日志
- ✅ 可以查看历史执行记录
- ✅ 可以设置定时执行或事件触发

#### 2B. 自动触发（可选，需要配置）

如果你想让 Ansible 自动运行，可以取消注释 workflow 中的 `push` 触发：

```yaml
on:
  workflow_dispatch: ...  # 保留：手动触发
  
  push:                   # 取消注释：自动触发
    branches:
      - master
      - deploy/*
    paths:
      - 'ansible/**'
      - '.github/workflows/ansible-deploy.yml'
```

这样当你 push 到 `master` 或 `deploy/*` 分支，或修改 ansible 文件时，会自动部署。

---

## 📋 GitHub Actions 方式需要的 Secrets

在 GitHub 仓库中添加以下 Secrets：

| Secret 名称 | 值 | 用途 |
|-----------|-----|------|
| `ORACLE_HOST` | 你的 Oracle IP | SSH 连接地址 |
| `ORACLE_USERNAME` | ubuntu | SSH 用户名 |
| `SSH_PRIVATE_KEY` | SSH 私钥内容 | SSH 认证 |
| `VAULT_PASSWORD` | Ansible vault 密码 | 解密 vault.yml |

**获取 SSH 私钥内容：**
```bash
# 在你的本地电脑执行
cat ~/.ssh/oracle_key
# 复制整个内容到 GitHub Secrets
```

**获取 Vault 密码：**
```bash
# 就是你创建 vault.yml 时输入的密码
# 记住你输入的密码即可
```

---

## 🔄 完整部署流程

### 首次完整部署（推荐）

```
步骤 1: 手动 Ansible 部署 MySQL
  ↓
bash ansible/deploy.sh mysql
  ↓
MySQL 部署成功
  ↓
────────────────────────
步骤 2: GitHub Actions 配置
  ↓
添加 Secrets: ORACLE_HOST, ORACLE_USERNAME, SSH_PRIVATE_KEY, VAULT_PASSWORD
  ↓
────────────────────────
步骤 3: GitHub Actions 自动部署 Docker 应用
  ↓
git push origin master
  ↓
GitHub Actions 自动执行
  ↓
Docker 容器启动，连接到 MySQL
```

### 后续更新

```
修改代码
  ↓
git push origin master
  ↓
GitHub Actions (deploy.yml) 自动触发
  ↓
构建 + 部署 Docker 应用
  ↓
完成
```

如果需要更新 MySQL 配置：
```
修改 ansible 文件
  ↓
GitHub Actions 手动触发 (ansible-deploy.yml)
  ↓
执行 Ansible 更新
  ↓
完成
```

---

## 📊 触发机制对比表

```
┌─────────────────────┬──────────────────────┬──────────────────────┐
│                     │ 手动本地 Ansible     │ GitHub Actions       │
├─────────────────────┼──────────────────────┼──────────────────────┤
│ 触发位置           │ 你的电脑              │ GitHub 网页           │
│ 执行位置           │ 你的电脑              │ GitHub 服务器         │
│ 需要本地环境       │ 需要 Ansible         │ 无需（自动安装）     │
│ 需要本地 SSH 配置  │ 需要                 │ 无需（使用 Secrets） │
│ 实时输出           │ 有                   │ 有（日志页面）       │
│ 自动化程度         │ 低（手动执行）       │ 高（可自动触发）     │
│ 适合场景           │ 首次部署、调试       │ 定期更新、自动化     │
│ 难度               │ 中等                 │ 简单                 │
│ 可靠性             │ 高                   │ 高                   │
└─────────────────────┴──────────────────────┴──────────────────────┘
```

---

## 🎬 实际操作示例

### 例子 1：首次部署（推荐）

```bash
# 在你的本地电脑
cd ai-receipt-backend

# 第 1 步：本地 Ansible 部署 MySQL
cd ansible
ansible-vault create vars/vault.yml
# 输入密码：my-mysql-password、my-jwt-secret

# 执行部署
bash deploy.sh mysql
# 等待 3-5 分钟，MySQL 部署成功

# 验证
ssh ubuntu@<ORACLE_IP>
mysql -u receipt_user -p ai_receipt_db
# 输入 my-mysql-password，应该连接成功

# 第 2 步：GitHub Actions 配置
# 在 GitHub UI 中添加 Secrets:
# ORACLE_HOST = <你的 IP>
# ORACLE_USERNAME = ubuntu
# SSH_PRIVATE_KEY = (你的私钥)
# VAULT_PASSWORD = my-mysql-password 的密码

# 第 3 步：Git 推送代码
git add .
git commit -m "Initial deployment"
git push origin master

# GitHub Actions 自动部署 Docker 应用
# 查看 Actions 标签页，监控进度
```

### 例子 2：后续维护（修改 MySQL 配置）

```bash
# 修改 Ansible 配置
cd ansible
nano vars/main.yml
# 修改 java_heap_max: "2048m"

# 方式 A: 本地 Ansible 部署
bash deploy.sh java-app

# 或方式 B: GitHub Actions 自动部署
# GitHub → Actions → "Deploy Ansible MySQL" → "Run workflow"
# 选择 "java-app"
# 点击 "Run workflow"
# 等待自动执行
```

---

## ⚙️ GitHub Actions Workflow 配置详解

### 触发条件

```yaml
on:
  # 手动触发（推荐）
  workflow_dispatch:
    inputs:
      target:
        options:
          - mysql        # 仅部署 MySQL
          - java-app     # 仅部署应用
          - all          # 全部部署

  # 自动触发（可选，取消注释启用）
  push:
    branches:
      - master         # master 分支改动时
      - deploy/*       # deploy/* 分支改动时
    paths:
      - 'ansible/**'   # 修改 ansible 文件时
```

### Workflow 执行步骤

```
1. Checkout code            ← 拉取代码
2. Install Ansible          ← 安装 Ansible
3. Setup SSH key            ← 配置 SSH（从 Secrets）
4. Create inventory         ← 生成 hosts.ini
5. Create vault password    ← 生成 vault 密码
6. Deploy MySQL/Java-app    ← 执行 Ansible
7. Cleanup                  ← 清理临时文件
```

---

## 🔒 安全性说明

### 本地 Ansible 方式

```
你的电脑
├── ansible/
│   ├── hosts.ini           ← 包含服务器 IP（本地存储）
│   ├── vars/vault.yml      ← 敏感数据加密存储
│   └── deploy.yml          ← 部署脚本
└── ~/.ssh/
    └── oracle_key          ← SSH 私钥（本地存储）
```

**安全性：** 高
- 敏感数据本地加密
- SSH 密钥本地管理

---

### GitHub Actions 方式

```
GitHub 仓库
├── .github/workflows/
│   ├── ansible-deploy.yml  ← 明文存储（安全）
│   └── deploy.yml          ← 明文存储（安全）
└── Secrets（加密存储）
    ├── SSH_PRIVATE_KEY     ← 加密
    ├── VAULT_PASSWORD      ← 加密
    └── ORACLE_HOST         ← 加密
```

**安全性：** 高
- Secrets 在 GitHub 加密存储
- Workflow 中的敏感数据被 mask
- 临时文件自动清理

---

## 🎯 建议使用方案

### 对于你的场景（Oracle 云 + MySQL + Docker）

**第 1 阶段：初始部署**
```
使用：本地 Ansible 手动部署 MySQL
原因：需要自主控制、快速调试、看到实时输出
```

**第 2 阶段：应用部署**
```
使用：GitHub Actions (deploy.yml) 自动部署 Docker
原因：可以自动触发、完整 CI/CD
```

**第 3 阶段：后续维护**
```
使用：GitHub Actions 按需手动触发 (ansible-deploy.yml)
原因：无需本地环境、方便快速、历史记录完整
```

---

## 📞 Q&A

**Q: Ansible 部署会覆盖现有的 MySQL 吗？**
A: 不会。Ansible 采用幂等性设计，重复执行相同命令不会改变现有配置（如果目标已达成）。

**Q: 我可以只部署 MySQL 不部署应用吗？**
A: 可以。使用 `-t mysql` 标签只部署 MySQL。

**Q: GitHub Actions 部署失败了怎么办？**
A: 查看 GitHub Actions 日志，或切换回本地 Ansible 手动部署。

**Q: 两种方式可以同时使用吗？**
A: 可以。本地 Ansible 和 GitHub Actions 互不影响，但不要同时运行以避免冲突。

**Q: 如何自动更新 MySQL？**
A: 在 ansible-deploy.yml 中取消注释 `push` 触发，修改 ansible 文件后自动执行。

---

## 📚 相关文档

- [ORACLE_DEPLOYMENT.md](./ORACLE_DEPLOYMENT.md) - 完整部署指南
- [DEPLOYMENT_CHECKLIST.md](./DEPLOYMENT_CHECKLIST.md) - 检查清单
- [ansible/DEPLOY_GUIDE.md](./ansible/DEPLOY_GUIDE.md) - Ansible 详细说明
