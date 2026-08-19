# syntax=docker/dockerfile:1
# ============================================================
# Cyxz Maven 依赖预构建镜像
# 把所有外部依赖 + 公共模块 jar 预装到本地仓库
# 微服务 Dockerfile FROM 这个镜像，编译时无需重复下载依赖
#
# 构建命令:
#   docker build -f maven-deps.Dockerfile -t cyxz-maven-deps:latest ..
#
# 何时重新构建:
#   - pom.xml 中的依赖发生变化时
#   - 公共模块(common/security/*-api)源码发生变化时
# 首次构建（全新环境）会联网下载全部依赖，约 10-15 分钟；
# 之后 pom 未变时层缓存命中，秒级完成。
# ============================================================

FROM maven:3.9-eclipse-temurin-17
WORKDIR /build

# 阿里云 Maven 镜像源
RUN cat > /tmp/maven-settings.xml << 'EOF'
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central</mirrorOf>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
EOF

# ---- 第一层：复制所有 pom.xml（pom 变更时此层起全部重建）----
COPY pom.xml .
COPY cyxz-common/pom.xml cyxz-common/
COPY cyxz-security/pom.xml cyxz-security/
COPY cyxz-user-api/pom.xml cyxz-user-api/
COPY cyxz-auth-api/pom.xml cyxz-auth-api/
COPY cyxz-post-api/pom.xml cyxz-post-api/
COPY cyxz-comment-api/pom.xml cyxz-comment-api/
COPY cyxz-message-api/pom.xml cyxz-message-api/
COPY cyxz-circle-api/pom.xml cyxz-circle-api/
COPY cyxz-governance-api/pom.xml cyxz-governance-api/
COPY cyxz-audit-api/pom.xml cyxz-audit-api/
COPY cyxz-gateway/pom.xml cyxz-gateway/
COPY cyxz-auth/pom.xml cyxz-auth/
COPY cyxz-user/pom.xml cyxz-user/
COPY cyxz-post/pom.xml cyxz-post/
COPY cyxz-comment/pom.xml cyxz-comment/
COPY cyxz-message/pom.xml cyxz-message/
COPY cyxz-search/pom.xml cyxz-search/
COPY cyxz-upload/pom.xml cyxz-upload/
COPY cyxz-circle/pom.xml cyxz-circle/
COPY cyxz-governance/pom.xml cyxz-governance/
COPY cyxz-audit/pom.xml cyxz-audit/
COPY cyxz-analytics/pom.xml cyxz-analytics/

# ---- 第二层：编译安装公共模块到本地仓库 ----
# 公共模块源码没变时，这层缓存命中
COPY cyxz-common/src cyxz-common/src
COPY cyxz-security/src cyxz-security/src
COPY cyxz-user-api/src cyxz-user-api/src
COPY cyxz-auth-api/src cyxz-auth-api/src
COPY cyxz-post-api/src cyxz-post-api/src
COPY cyxz-comment-api/src cyxz-comment-api/src
COPY cyxz-message-api/src cyxz-message-api/src
COPY cyxz-circle-api/src cyxz-circle-api/src
COPY cyxz-governance-api/src cyxz-governance-api/src
COPY cyxz-audit-api/src cyxz-audit-api/src

# 联网模式编译安装：全新环境首次构建可正常下载依赖，
# 依赖已存在时跳过下载，不重复拉取
RUN mvn install -Dmaven.test.skip=true \
    -pl cyxz-common,cyxz-security,cyxz-user-api,cyxz-auth-api,cyxz-post-api,cyxz-comment-api,cyxz-message-api,cyxz-circle-api,cyxz-governance-api,cyxz-audit-api \
    -am -s /tmp/maven-settings.xml -q
