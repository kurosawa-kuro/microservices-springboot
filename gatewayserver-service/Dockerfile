FROM openjdk:21-jdk-slim

LABEL maintainer="kurobytes.com"

COPY target/gatewayserver-*.jar gatewayserver.jar

ENTRYPOINT ["java", "-jar", "/gatewayserver.jar"]
