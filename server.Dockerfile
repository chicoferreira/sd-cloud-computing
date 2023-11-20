FROM gradle:jdk17-alpine

WORKDIR /app

COPY common common
COPY server server
COPY build.gradle.kts settings.gradle.kts ./

RUN gradle build

EXPOSE 8080
EXPOSE 9900

ENTRYPOINT ["java", "-jar", "server/build/libs/server-1.0-SNAPSHOT-all.jar"]
