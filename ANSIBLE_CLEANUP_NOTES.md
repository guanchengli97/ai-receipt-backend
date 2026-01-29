# Ansible Java-App 已移除说明

## 📝 变更说明

已删除 Ansible 中的 `java-app` 角色和相关部署配置。

### 原因

你的项目采用的是 **Docker 容器化部署方案**：

```
GitHub Workflows deploy.yml
    ↓
编译 Java 代码
    ↓
打包 Docker 镜像
    ↓
推送到 Docker Hub
    ↓
SSH 启动 Docker 容器（在 Oracle 服务器上）
```

因此，**Ansible 的 java-app 角色是多余的**，会造成混淆。

### 已删除的内容

❌ **已删除：**
- `ansible/roles/java-app/` - 整个目录
- `ansible/deploy.yml` 中的 java-app role 配置
- `ansible/deploy.sh` 中的 java-app 选项
- `ansible/deploy.ps1` 中的 java-app 选项
- `.github/workflows/ansible-deploy.yml` 中的 java-app 选项

✅ **保留：**
- `ansible/roles/mysql/` - MySQL 部署（需要）
- `ansible/deploy.yml` - Ansible 主文件（只部署 MySQL）
- `ansible/deploy.sh` - Shell 脚本（只部署 MySQL）
- `ansible/deploy.ps1` - PowerShell 脚本（只部署 MySQL）
- `.github/workflows/ansible-deploy.yml` - GitHub Actions（只部署 MySQL）

### 现在的部署流程

```
1️⃣ 初始化：Ansible 部署 MySQL
   bash ansible/deploy.sh
   
   或
   
   GitHub Actions 按钮
   → Deploy Ansible MySQL → Run workflow

2️⃣ 应用部署：GitHub Workflows 部署 Docker 容器
   git push origin master
   → GitHub Actions 自动触发 deploy.yml
   → 构建 + 推送镜像
   → 启动 Docker 容器（连接到 MySQL）
```

### 使用示例

**部署 MySQL：**
```bash
cd ansible
bash deploy.sh
```

**部署应用（自动触发）：**
```bash
git push origin master
# GitHub Actions 自动执行
```

### 相关文档更新

已更新以下文档，移除了对 java-app 的引用：
- `ORACLE_QUICK_REFERENCE.md`
- `ORACLE_DEPLOYMENT.md`
- `ansible/DEPLOY_GUIDE.md`

### 如果需要恢复

如果以后需要使用 Ansible 部署 Java 应用（不用 Docker），可以从 Git 历史记录恢复。

---

**现在项目架构清晰简洁：**
- ✅ MySQL 部署 → Ansible
- ✅ 应用部署 → Docker + GitHub Workflows
- ❌ 无重复、无混淆
