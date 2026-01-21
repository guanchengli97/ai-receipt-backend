FROM eclipse-temurin:17-jre-alpine
# 使用官方的JDK 17 环境运行

WORKDIR /app
#设置容器内的工作目录

COPY target/*.jar app.jar

# 将本地构建好的 Spring Boot Jar 包复制到容器中
# target/*.jar 是 Maven 默认打包输出目录
# 复制后重命名为 app.jar，方便统一启动

EXPOSE 8080
# 声明应用在容器内监听的端口（文档作用）
# 实际对外访问需在 docker run 时使用 -p 映射

CMD ["java", "-jar", "app.jar"]