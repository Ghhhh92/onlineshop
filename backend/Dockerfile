# 后端镜像：两阶段构建（Maven 打包 → 仅用 JRE 运行）
# 构建：docker build -t <你的命名空间>/onlineshop-backend:1.0.0 ./backend

# ---- 阶段 1：构建，用 Maven + JDK 17 打可执行 jar ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY src ./src
# 构建镜像时跳过测试（测试在 CI / 本地用 mvn test 单独执行，见《运行录屏》）
RUN mvn -B clean package -DskipTests

# ---- 阶段 2：运行，只带 JRE，体积更小 ----
FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=build /build/target/onlineshop-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8000
# 数据库连接通过环境变量注入：DB_URL / DB_USERNAME / DB_PASSWORD
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
