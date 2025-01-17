FROM gradle:8.3-jdk17 AS build

WORKDIR /app

COPY . .

RUN gradle build -x test --no-daemon

FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

ENV SPRING_DATASOURCE_URL=jdbc:postgresql://master_postgres:5432/note_db
ENV SPRING_DATASOURCE_USERNAME=user
ENV SPRING_DATASOURCE_PASSWORD=pswd
ENV SPRING_REDIS_HOST=master_redis
ENV SPRING_REDIS_PORT=6379
ENV AUTH_CLIENT_HOST=user-service
ENV AUTH_CLIENT_PORT=8081

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
