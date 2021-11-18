FROM adoptopenjdk:8-jdk as maven
ENV LANG=C.UTF-8 LANGUAGE=C LC_ALL=C.UTF-8 TERM=linux

# Install programs
RUN apt-get update && apt-get install -y maven libatomic1

WORKDIR /app
# Get maven project pom and src
COPY pom.xml .
COPY src .

RUN mvn -B -e -C org.apache.maven.plugins:maven-dependency-plugin:3.0.2:go-offline
COPY . .
RUN mvn -B -e verify