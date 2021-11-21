# checkout code
FROM drone/git as clone
WORKDIR /hamqtt

RUN git clone https://bitbucket.org/vdwals/hamqtt.git .

# Build application
FROM arm32v7/maven as builder
WORKDIR /hamqtt
COPY --from=clone /hamqtt .
RUN mvn install
RUN rm -rf /hamqtt

WORKDIR /app

# Prepare maven
COPY ./pom.xml .
RUN mvn dependency:go-offline

# Build app
COPY ./src ./src
COPY ./lombok.config ./lombok.config
RUN mvn package

# Build running container
FROM arm32v7/openjdk:11.0.3-jre
WORKDIR /app
COPY --from=builder /app/target/alpha_ess*-jar-with-dependencies.jar ./alpha_ess_2_mqtt.jar
LABEL name="Alpha-ESS-2-MQTT"
ENV TZ=UTC
ENTRYPOINT ["java",  "-jar", "alpha_ess_2_mqtt.jar"]