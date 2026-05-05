# checkout code
FROM drone/git as clone
WORKDIR /hamqtt

RUN git clone https://bitbucket.org/vdwals/hamqtt.git . \
    && git checkout 3.5.1

# Build application
FROM arm32v7/maven:3-eclipse-temurin-17 as builder
WORKDIR /hamqtt
COPY --from=clone /hamqtt .
RUN mvn install
RUN rm -rf /hamqtt

WORKDIR /app

# Prepare maven
COPY ./pom.xml .
COPY ./lombok.config ./lombok.config
RUN mvn dependency:go-offline

# Build app
COPY ./src ./src
RUN mvn package

# Build running container
FROM arm32v7/eclipse-temurin:17-jre-focal
WORKDIR /app
COPY --from=builder /app/target/alpha_ess*.jar ./alpha_ess_2_mqtt.jar
LABEL name="Alpha-ESS-2-MQTT"
ENV TZ=UTC
ENTRYPOINT ["java", "-Dlog4j2.formatMsgNoLookups=true", "-Xmx192m", "-XX:+UseSerialGC", "-jar", "alpha_ess_2_mqtt.jar"]