FROM openjdk:17-jdk-slim

LABEL maintainer="eazybytes.com"

COPY target/gatewayserver-*.jar gatewayserver.jar

ENTRYPOINT ["java", "-jar", "/gatewayserver.jar"]
