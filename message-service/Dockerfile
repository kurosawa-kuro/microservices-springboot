FROM openjdk:17-jdk-slim

LABEL maintainer="kurobytes.com"

COPY target/message-*.jar message.jar

ENTRYPOINT ["java", "-jar", "/message.jar"]
