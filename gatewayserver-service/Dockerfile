FROM openjdk:17-jdk-slim

LABEL maintainer="kurobytes.com"

COPY target/gatewayserver-*.jar gatewayserver.jar

ENTRYPOINT ["java", "-jar", "/gatewayserver.jar"]
