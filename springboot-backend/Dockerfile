# 多阶段构建：减少最终镜像体积
# 阶段 1：构建
FROM maven:3.8-openjdk-8-slim AS builder

WORKDIR /build

# 先复制 pom.xml 下载依赖（利用 Docker 缓存层）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并编译
COPY src ./src
RUN mvn clean package -DskipTests -B

# 阶段 2：运行
FROM openjdk:8-jre-alpine

# 设置时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 创建应用目录
RUN mkdir -p /app/uploads /app/logs /app/static

WORKDIR /app

# 从构建阶段复制 jar 包
COPY --from=builder /build/target/medicine-box-1.0.0.jar app.jar

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# JVM 参数优化（根据容器内存调整）
ENV JAVA_OPTS="-server -Xms512m -Xmx1024m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
