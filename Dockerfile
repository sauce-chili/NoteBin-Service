FROM gradle:8.3-jdk17 AS build

WORKDIR /app

COPY . .

RUN gradle build -x test --no-daemon

FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
