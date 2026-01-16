# 1) build stage
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -q -DskipTests package

# 2) run stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# тут кажемо Spring'у: використовуй профіль docker -> підтягнеться application-docker.yml
ENV SPRING_PROFILES_ACTIVE=docker

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
