# syntax=docker/dockerfile:1
# ============================================================
# Cyxz Java 微服务通用 Dockerfile（基于预构建的 maven-deps 镜像）
# 依赖已在 cyxz-maven-deps 镜像中预装，离线编译无需访问网络
# 用法: docker build --build-arg SERVICE_NAME=cyxz-gateway -t cyxz-gateway .
# ============================================================

# ===== Stage 1: Maven Build（从预构建镜像开始）=====
FROM cyxz-maven-deps:latest AS builder
WORKDIR /build

# 复制全部源码
COPY . .

ARG SERVICE_NAME

# 编译：依赖大部分已在 maven-deps 镜像中，缺失的从阿里云补下载
# 不用 -am：公共模块 jar 已 install 到本地仓库，直接引用
RUN mvn package -Dmaven.test.skip=true -pl ${SERVICE_NAME} -q -s /tmp/maven-settings.xml

# ===== Stage 2: Runtime =====
FROM eclipse-temurin:17-jre

ARG SERVICE_NAME
LABEL service="${SERVICE_NAME}"

COPY --from=builder /build/${SERVICE_NAME}/target/*.jar app.jar

ENV JAVA_OPTS="-Xms128m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app.jar"]
