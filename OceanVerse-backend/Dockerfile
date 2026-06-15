# 构建后端
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY oceanverse-common/pom.xml oceanverse-common/
COPY oceanverse-pojo/pom.xml oceanverse-pojo/
COPY user-auth-manager/pom.xml user-auth-manager/
COPY species-manager/pom.xml species-manager/
COPY eco-observation-manager/pom.xml eco-observation-manager/
COPY data-visual-analytics/pom.xml data-visual-analytics/
COPY smart-ai-service/pom.xml smart-ai-service/
COPY community-manager/pom.xml community-manager/
COPY message-service/pom.xml message-service/
COPY oceanverse-app/pom.xml oceanverse-app/
RUN mvn dependency:go-offline -B
COPY . .
RUN mvn package -DskipTests -B

# 运行
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/oceanverse-app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
