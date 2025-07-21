FROM openjdk:17-jdk-slim

LABEL maintainer="eazybytes.com"

COPY target/message-*.jar message.jar

ENTRYPOINT ["java", "-jar", "/message.jar"]
