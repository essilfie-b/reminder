FROM openjdk:17-jdk-slim AS build
WORKDIR demo
COPY target/reminder-0.0.1-SNAPSHOT.jar demo.jar
ENTRYPOINT ["java", "-jar", "demo.jar"]
EXPOSE 8080