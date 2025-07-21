FROM openjdk:17-jdk-slim

LABEL maintainer="eazybytes.com"

COPY target/cards-*.jar cards.jar

VOLUME /data

ENTRYPOINT ["java", "-jar", "/cards.jar"]
