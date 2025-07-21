FROM openjdk:21-jdk-slim

LABEL maintainer="kurobytes.com"

COPY target/loans-*.jar loans.jar

VOLUME /data

ENTRYPOINT ["java", "-jar", "/loans.jar"]
