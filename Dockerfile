# Build application
FROM maven:3.8.1-jdk-11-slim as builder
WORKDIR /app

# Prepare maven
COPY ./pom.xml .
RUN mkdir scripts
RUN mvn dependency:go-offline

# Build app
COPY . .
RUN mvn package

# Build running container
FROM arm32v7/openjdk:11.0.3-jre
WORKDIR /app
COPY --from=builder /app/target/alpha-ess*-jar-with-dependencies.jar ./alpha-ess-2-mqtt.jar
LABEL name="Alpha-ESS-2-MQTT"
ENV TZ=UTC
ENTRYPOINT ["java",  "-jar", "alpha-ess-2-mqtt.jar"]