FROM gradle:jdk17-alpine

WORKDIR /app

COPY common common
COPY worker worker
COPY build.gradle.kts settings.gradle.kts ./

RUN gradle build

ENTRYPOINT java -jar worker/build/libs/worker-1.0-SNAPSHOT-all.jar ${SERVER_HOST:-localhost:9900} ${MAX_CONCURRENT_JOBS:-10} ${MAX_MEMORY:-100}
