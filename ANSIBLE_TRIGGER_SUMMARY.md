# Ansible 触发机制 - 快速总结

## 🎯 一句话回答

**Ansible 部署 MySQL 目前需要你在本地电脑上手动执行命令触发，不是自动的。**

---

## 📊 两种方式速查表

### 方式 1️⃣：本地手动触发（现有、推荐首次使用）

```bash
# 你的电脑上执行
cd ansible
bash deploy.sh mysql
# 完成！
```

| 特性 | 状态 |
|------|------|
| 触发位置 | 你的电脑 |
| 执行位置 | 你的电脑 |
| 自动化 | ❌ 手动 |
| 需要本地配置 | ✅ Ansible + SSH |
| 难度 | 中等 |
| 首次部署适合度 | ⭐⭐⭐⭐⭐ |

---

### 方式 2️⃣：GitHub Actions 触发（新增、推荐后续使用）

#### A. 手动按钮触发

```
GitHub UI
  ↓
Actions → Deploy Ansible MySQL → Run workflow
  ↓
选择 mysql
  ↓
点击 Run workflow
  ↓
自动执行！
```

| 特性 | 状态 |
|------|------|
| 触发位置 | GitHub 网页 |
| 执行位置 | GitHub 服务器 |
| 自动化 | ⚙️ 手动按钮 |
| 需要本地配置 | ❌ 无需 |
| 难度 | 简单 |
| 后续维护适合度 | ⭐⭐⭐⭐⭐ |

#### B. 自动触发（可选）

取消注释 `.github/workflows/ansible-deploy.yml` 中的 `push` 配置：

```
修改 ansible 文件
  ↓
git push
  ↓
GitHub Actions 自动执行
```

---

## 🚀 推荐流程

### 首次部署

```
1️⃣ 本地手动 Ansible 部署 MySQL
   bash ansible/deploy.sh mysql
   
2️⃣ 验证 MySQL 成功
   ssh ubuntu@<IP>
   mysql -u receipt_user -p ai_receipt_db

3️⃣ GitHub 配置 Secrets
   ORACLE_HOST, SSH_PRIVATE_KEY, VAULT_PASSWORD 等

4️⃣ Git 推送代码
   git push origin master

5️⃣ GitHub Actions 自动部署 Docker 应用
```

### 后续更新

**更新应用代码：**
```bash
git push origin master
→ GitHub Actions (deploy.yml) 自动部署 Docker
```

**更新 MySQL 配置：**
```bash
# 方式 A: 本地手动
bash ansible/deploy.sh mysql

# 方式 B: GitHub 按钮（新增）
GitHub → Actions → Deploy Ansible MySQL → Run workflow → mysql
```

---

## 🔑 需要的配置

### 方式 1（本地手动）

需要你的电脑上有：
- ✅ Ansible 安装
- ✅ SSH 密钥配置
- ✅ `ansible/hosts.ini` 配置
- ✅ `ansible/vars/vault.yml` 配置

### 方式 2（GitHub Actions）

需要在 GitHub 中配置：
- ✅ `ORACLE_HOST` - 服务器 IP
- ✅ `ORACLE_USERNAME` - SSH 用户名
- ✅ `SSH_PRIVATE_KEY` - SSH 私钥
- ✅ `VAULT_PASSWORD` - Vault 密码

---

## ✅ 对比总结

```
┌────────────┬──────────────────────┬─────────────────────┐
│ 维度       │ 本地手动 Ansible     │ GitHub Actions      │
├────────────┼──────────────────────┼─────────────────────┤
│ 触发       │ 你执行命令            │ 点击网页按钮         │
│ 本地配置   │ 需要                 │ 不需要               │
│ 实时日志   │ 有                   │ 有（网页查看）       │
│ 自动化程度 │ 低                   │ 高                   │
│ 首次部署   │ ⭐⭐⭐⭐⭐          │ ❌ 不推荐            │
│ 后续维护   │ ✅ 可用              │ ⭐⭐⭐⭐⭐ 推荐   │
│ 难度       │ 中等                 │ 简单                 │
│ 失败时     │ 立即修改重试          │ 查看日志或本地部署   │
└────────────┴──────────────────────┴─────────────────────┘
```

---

## 🎯 你现在应该做什么

### 如果想立即部署 MySQL

```bash
# 1. 编辑 ansible/hosts.ini，添加 Oracle 服务器
# 2. 创建 ansible/vars/vault.yml，设置密码
# 3. 执行
bash ansible/deploy.sh mysql
# 完成！
```

### 如果想后续自动化

```bash
# 1. 完成上面的 MySQL 部署
# 2. 在 GitHub 中添加 Secrets（4 个）
# 3. 后续可以直接点按钮部署
# GitHub → Actions → Deploy Ansible MySQL → Run workflow
```

---

## 📝 文档导航

- **完整详解** → [ANSIBLE_TRIGGER_GUIDE.md](./ANSIBLE_TRIGGER_GUIDE.md)
- **Oracle 部署** → [ORACLE_DEPLOYMENT.md](./ORACLE_DEPLOYMENT.md)
- **快速参考** → [ORACLE_QUICK_REFERENCE.md](./ORACLE_QUICK_REFERENCE.md)
- **部署检查** → [DEPLOYMENT_CHECKLIST.md](./DEPLOYMENT_CHECKLIST.md)

---

**简单来说：Ansible 目前需要你手动执行命令部署，但已经新增了 GitHub Actions 支持可选手动按钮触发！** ✨
