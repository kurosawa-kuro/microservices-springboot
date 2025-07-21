FROM openjdk:17-jdk-slim

LABEL maintainer="kurobytes.com"

COPY target/cards-*.jar cards.jar

VOLUME /data

ENTRYPOINT ["java", "-jar", "/cards.jar"]
