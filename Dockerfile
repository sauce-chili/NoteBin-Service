FROM gradle:8.3-jdk17 AS buildr

WORKDIR /app

COPY . .

RUN gradle build

FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=buildr /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
