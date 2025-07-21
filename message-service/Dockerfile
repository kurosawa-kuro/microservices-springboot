FROM openjdk:21-jdk-slim

LABEL maintainer="kurobytes.com"

COPY target/message-*.jar message.jar

ENTRYPOINT ["java", "-jar", "/message.jar"]
