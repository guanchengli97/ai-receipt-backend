# AI Receipt Backend - Ansible 部署完整方案

## 📚 项目概述

本方案使用 Ansible 自动化部署工具，一键部署 AI Receipt Backend Java 应用和 MySQL 数据库到同一个服务器上，支持 Ubuntu、Debian、CentOS 等主流 Linux 发行版。

## ✨ 核心特性

- ✅ **完全自动化** - 一条命令部署完整系统
- ✅ **支持多系统** - Ubuntu/Debian/CentOS/RHEL
- ✅ **安全加密** - 使用 Ansible Vault 保护敏感信息
- ✅ **模块化设计** - 独立部署 MySQL 或应用
- ✅ **服务管理** - systemd 自动启动和管理
- ✅ **健康检查** - 自动验证部署成功
- ✅ **易于维护** - 清晰的配置和日志

## 📁 完整文件结构

```
ai-receipt-backend/
├── README.md                          # 项目说明
├── QUICK_START.md                     # ⭐ 快速开始指南
├── pom.xml                            # Maven 构建配置（已添加 MySQL 依赖）
├── Dockerfile                         # Docker 镜像配置
├── src/
│   └── main/
│       ├── java/com/example/
│       │   └── aireceiptbackend/
│       │       ├── AiReceiptBackendApplication.java
│       │       ├── config/
│       │       │   └── SecurityConfig.java
│       │       ├── controller/
│       │       │   └── AuthController.java
│       │       │   └── HealthController.java
│       │       ├── model/
│       │       │   ├── AuthRequest.java
│       │       │   ├── AuthResponse.java
│       │       │   ├── RegisterResponse.java
│       │       │   └── User.java                 # ⭐ 新增 JPA Entity
│       │       ├── repository/
│       │       │   └── UserRepository.java       # ⭐ 新增 Repository
│       │       ├── service/
│       │       │   └── AuthService.java
│       │       └── util/
│       │           └── JwtUtil.java
│       └── resources/
│           └── application.yml                   # ⭐ 新增配置文件
│
└── ansible/                           # ⭐ Ansible 部署文件
    ├── deploy.yml                     # 主部署 playbook
    ├── deploy.sh                      # Linux/Mac 部署脚本
    ├── deploy.ps1                     # Windows 部署脚本
    ├── init-database.yml              # 数据库初始化脚本
    ├── hosts.ini                      # ⭐ 主机配置（需要修改）
    ├── hosts.ini.example              # 配置示例
    ├── DEPLOY_GUIDE.md                # 详细部署指南
    ├── ARCHITECTURE.md                # 架构说明和流程
    ├── vars/
    │   ├── main.yml                   # 公开配置变量
    │   └── vault.yml                  # ⭐ 敏感信息（需要加密）
    └── roles/
        ├── mysql/
        │   ├── tasks/
        │   │   └── main.yml           # MySQL 安装配置任务
        │   └── templates/             # MySQL 配置模板
        └── java-app/
            ├── tasks/
            │   └── main.yml           # Java 应用部署任务
            └── templates/
                ├── application.yml.j2           # 应用配置模板
                └── ai-receipt-backend.service.j2 # systemd 服务模板
```

## 🚀 快速部署（5 分钟）

### 第 1 步：安装 Ansible

```bash
# Ubuntu/Debian
sudo apt-get update && sudo apt-get install ansible -y

# CentOS/RHEL  
sudo yum install ansible -y

# macOS
brew install ansible
```

### 第 2 步：配置主机

编辑 `ansible/hosts.ini`：

```ini
[webservers]
localhost ansible_connection=local
```

### 第 3 步：加密敏感信息

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
# Linux/Mac
bash ansible/deploy.sh all

# Windows PowerShell
.\ansible\deploy.ps1 -DeploymentType "all"

# 或手动执行
ansible-playbook ansible/deploy.yml \
    -i ansible/hosts.ini \
    --ask-vault-pass \
    --ask-become-pass
```

### 第 5 步：验证部署

```bash
# 检查应用
curl http://localhost:8080/api/health

# 检查数据库
mysql -u receipt_user -p -h localhost ai_receipt_db

# 查看应用日志
tail -f /opt/ai-receipt-backend/logs/app.log

# 检查应用服务
sudo systemctl status ai-receipt-backend
```

## 📋 主要功能说明

### Ansible Playbook 功能

#### 1. MySQL 角色 (roles/mysql)

**安装和配置：**
- 安装 MySQL Server 8.0+
- 创建应用数据库 (ai_receipt_db)
- 创建应用用户 (receipt_user)
- 配置用户权限
- 可选：配置远程访问

**关键任务：**
```yaml
- 更新包管理器
- 安装 MySQL 和 Python MySQL 驱动
- 启动 MySQL 服务
- 创建数据库和用户
- 初始化数据库结构（可选）
```

#### 2. Java 应用角色 (roles/java-app)

**部署流程：**
- 创建应用用户 (appuser)
- 安装 Java 11 Runtime
- 安装 Maven 构建工具
- 编译 Java 应用
- 生成配置文件
- 创建 systemd 服务
- 启动应用并验证

**关键任务：**
```yaml
- 安装 OpenJDK 11
- 构建 Maven 应用
- 配置 Spring Boot 应用
- 注册 systemd 服务
- 健康检查验证
```

### 配置文件

#### application.yml

Spring Boot 应用配置（支持变量替换）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://{{ db_host }}:{{ db_port }}/{{ db_name }}
    username: {{ db_user }}
    password: {{ db_password }}
  jpa:
    hibernate:
      ddl-auto: update

server:
  port: 8080
  servlet:
    context-path: /api

jwt:
  secret: {{ jwt_secret }}
```

#### systemd 服务文件

自动管理应用生命周期：

```ini
[Service]
Type=simple
User=appuser
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar /opt/ai-receipt-backend/ai-receipt-backend.jar
Restart=on-failure
RestartSec=10
```

## 🔑 关键特性

### 1. 模块化部署

分别部署 MySQL 或应用：

```bash
# 仅部署 MySQL
bash ansible/deploy.sh mysql

# 仅部署应用
bash ansible/deploy.sh java-app

# 全部部署
bash ansible/deploy.sh all
```

### 2. 安全加密

敏感信息使用 Ansible Vault 加密：

```bash
# 编辑加密文件
ansible-vault edit ansible/vars/vault.yml

# 指定密码文件
ansible-playbook deploy.yml --vault-password-file=.vault-pass
```

### 3. 多服务器支持

轻松扩展到多个服务器：

```ini
# hosts.ini
[webservers]
server1 ansible_host=192.168.1.100 ansible_user=ubuntu
server2 ansible_host=192.168.1.101 ansible_user=ubuntu
server3 ansible_host=192.168.1.102 ansible_user=ubuntu
```

```bash
# 并行部署（10 个并发）
ansible-playbook deploy.yml -i hosts.ini -f 10 --ask-vault-pass
```

### 4. 自动服务管理

```bash
# 启动/停止应用
sudo systemctl start ai-receipt-backend
sudo systemctl stop ai-receipt-backend

# 查看日志
journalctl -u ai-receipt-backend -f

# 检查状态
sudo systemctl status ai-receipt-backend
```

### 5. 数据库初始化

可选执行数据库初始化脚本：

```bash
ansible-playbook ansible/init-database.yml \
    -i ansible/hosts.ini \
    --ask-vault-pass
```

## 📊 已创建/修改的文件列表

### 新增文件

| 文件路径 | 说明 |
|---------|------|
| `ansible/deploy.yml` | 主 Playbook 文件 |
| `ansible/deploy.sh` | Linux/Mac 部署脚本 |
| `ansible/deploy.ps1` | Windows 部署脚本 |
| `ansible/init-database.yml` | 数据库初始化脚本 |
| `ansible/hosts.ini` | 主机清单配置 |
| `ansible/hosts.ini.example` | 配置示例 |
| `ansible/DEPLOY_GUIDE.md` | 详细部署指南 |
| `ansible/ARCHITECTURE.md` | 架构和流程说明 |
| `ansible/vars/main.yml` | 应用配置变量 |
| `ansible/vars/vault.yml` | 加密的敏感信息 |
| `ansible/roles/mysql/tasks/main.yml` | MySQL 安装脚本 |
| `ansible/roles/java-app/tasks/main.yml` | Java 应用部署脚本 |
| `ansible/roles/java-app/templates/application.yml.j2` | 应用配置模板 |
| `ansible/roles/java-app/templates/ai-receipt-backend.service.j2` | systemd 服务模板 |
| `src/main/resources/application.yml` | Spring Boot 配置文件 |
| `src/main/java/.../model/User.java` | JPA User Entity |
| `src/main/java/.../repository/UserRepository.java` | JPA Repository |
| `QUICK_START.md` | 快速开始指南 |

### 修改文件

| 文件路径 | 修改内容 |
|---------|--------|
| `pom.xml` | 添加 spring-boot-starter-data-jpa 和 mysql-connector-java 依赖 |

## 🔄 部署流程

```
┌─────────────────────────────────────────────┐
│ 1. 安装 Ansible                            │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│ 2. 编辑 hosts.ini 配置目标服务器           │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│ 3. 创建加密的 vault.yml 敏感信息           │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│ 4. 执行 deploy.yml 或 deploy.sh           │
└─────────────────────────────────────────────┘
                    ↓
        ┌──────────────────────────┐
        ↓                          ↓
    ┌────────────────┐      ┌──────────────────┐
    │ MySQL Role     │      │ Java App Role    │
    ├────────────────┤      ├──────────────────┤
    │ - 安装 MySQL   │      │ - 安装 Java      │
    │ - 创建数据库   │      │ - 编译应用       │
    │ - 创建用户     │      │ - 配置应用       │
    │ - 设置权限     │      │ - 启动服务       │
    └────────────────┘      │ - 健康检查       │
                            └──────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│ 5. 验证部署（检查应用和数据库）           │
└─────────────────────────────────────────────┘
```

## 📝 常用命令

```bash
# 查看所有可用 tags
ansible-playbook ansible/deploy.yml --list-tags

# 干运行（检查但不执行）
ansible-playbook ansible/deploy.yml -i ansible/hosts.ini --check

# 添加详细日志
ansible-playbook ansible/deploy.yml -i ansible/hosts.ini -vvv --ask-vault-pass

# 检查主机连接
ansible -i ansible/hosts.ini webservers -m ping

# 在远程主机上执行命令
ansible -i ansible/hosts.ini webservers -m shell -a "systemctl status mysql"

# 修改 Vault 密码
ansible-vault rekey ansible/vars/vault.yml
```

## ✅ 部署清单

使用以下清单确保部署顺利：

- [ ] 系统安装了 Ansible
- [ ] 配置了 hosts.ini（修改了目标服务器信息）
- [ ] 创建了加密的 vault.yml（修改了所有密码）
- [ ] 目标服务器支持 SSH 连接
- [ ] 执行了部署脚本
- [ ] 应用成功启动（检查健康状态）
- [ ] 数据库连接正常
- [ ] 应用日志没有错误

## 🐛 常见问题

**Q: 如何更改数据库密码？**
```bash
ansible-vault edit ansible/vars/vault.yml
# 修改 vault_db_password，然后重新部署
```

**Q: 如何在远程服务器上部署？**
```bash
# 编辑 hosts.ini，添加服务器信息
# 例如：prod-server ansible_host=192.168.1.100 ansible_user=ubuntu
# 然后执行部署
```

**Q: 如何查看应用日志？**
```bash
ssh user@server "tail -f /opt/ai-receipt-backend/logs/app.log"
# 或本地检查
sudo tail -f /opt/ai-receipt-backend/logs/app.log
```

**Q: 应用启动失败如何排查？**
```bash
# 查看服务状态
sudo systemctl status ai-receipt-backend

# 查看详细日志
journalctl -u ai-receipt-backend -f

# 查看应用日志
tail -f /opt/ai-receipt-backend/logs/app.log
```

## 📚 文档导航

- **快速开始** → [QUICK_START.md](./QUICK_START.md)
- **详细部署指南** → [ansible/DEPLOY_GUIDE.md](./ansible/DEPLOY_GUIDE.md)
- **架构和流程** → [ansible/ARCHITECTURE.md](./ansible/ARCHITECTURE.md)
- **配置示例** → [ansible/hosts.ini.example](./ansible/hosts.ini.example)

## 🎯 下一步

1. **修改 hosts.ini** - 配置你的目标服务器
2. **创建 vault.yml** - 加密敏感信息
3. **执行部署** - 运行 deploy.sh 或 deploy.ps1
4. **验证服务** - 检查应用和数据库
5. **定制配置** - 根据需要修改 vars/main.yml

## 💡 最佳实践

1. **安全性** - 定期更改密码，使用 SSH 密钥认证
2. **备份** - 定期备份数据库和配置文件
3. **监控** - 使用日志监控系统运行状态
4. **更新** - 定期更新系统包和应用依赖
5. **测试** - 在生产部署前在测试环境验证

## 📞 获取帮助

遇到问题时，请：

1. 查看 [DEPLOY_GUIDE.md](./ansible/DEPLOY_GUIDE.md) 中的故障排查部分
2. 检查 Ansible 日志输出（添加 `-vvv` 选项）
3. 查看目标服务器的系统日志
4. 参考 [ARCHITECTURE.md](./ansible/ARCHITECTURE.md) 中的故障排查流程

---

**祝部署顺利！** 🚀
