FROM docker.io/library/maven:3.8.6-openjdk-8 AS backend-builder

LABEL stage=backend-builder

WORKDIR /app

COPY springboot-backend/pom.xml springboot-backend/
COPY springboot-backend/src springboot-backend/src/

WORKDIR /app/springboot-backend
RUN mvn clean package -DskipTests -q

FROM docker.io/library/node:20-alpine AS frontend-builder

LABEL stage=frontend-builder

WORKDIR /app

COPY qianduan/package.json qianduan/package-lock.json qianduan/

WORKDIR /app/qianduan
RUN npm ci

COPY qianduan/src qianduan/src/
COPY qianduan/index.html qianduan/vite.config.js qianduan/

RUN npm run build

FROM docker.io/library/openjdk:8-jre-alpine

LABEL maintainer="medicine-box"
LABEL description="智能药箱助手 - Spring Boot后端 + Vue前端"

WORKDIR /app

RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

COPY --from=backend-builder /app/springboot-backend/target/medicine-box-1.0.0.jar app.jar
COPY --from=frontend-builder /app/qianduan/dist/ static/

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]