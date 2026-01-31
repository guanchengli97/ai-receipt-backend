# AI Receipt Backend (Spring Boot)

快速启动：

- 要求：已安装 Maven 和 JDK 11+
- 在项目目录运行：

```bash
mvn spring-boot:run
```

登录接口：

- URL: `POST /auth/login`
- 请求 JSON:

```json
{
  "username": "user",
  "password": "password"
}
```

- 返回示例：

```json
{
  "token": "<jwt-token>"
}
```

默认项目在 `src/main/java/com/example/aireceiptbackend` 下包含简单的登录实现（内存用户 `user` / `password`），以及 JWT 生成工具。请替换 `JwtUtil.SECRET` 为安全的密钥并在生产中使用持久化用户存储。
