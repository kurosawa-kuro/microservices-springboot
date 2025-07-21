FROM openjdk:17-jdk-slim

LABEL maintainer="kurobytes.com"

COPY target/accounts-*.jar accounts.jar

VOLUME /data

ENTRYPOINT ["java", "-jar", "/accounts.jar"]
