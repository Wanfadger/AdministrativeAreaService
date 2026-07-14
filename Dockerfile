# syntax=docker/dockerfile:1

# ---------------------------------------------------------------- build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Dependencies resolve in their own layer, so a source-only change doesn't re-download
# the world. This is the whole point of copying pom.xml before src/.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q clean package -DskipTests

# ---------------------------------------------------------------- layer extraction
# Boot layered jars split the fat jar by change frequency (deps -> loader -> snapshot
# deps -> application). Docker then caches the slow-moving layers across rebuilds.
FROM eclipse-temurin:21-jre-alpine AS layers
WORKDIR /layers
COPY --from=build /build/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ---------------------------------------------------------------- runtime
FROM eclipse-temurin:21-jre-alpine AS runtime

# Non-root. The previous image ran the JVM as root.
RUN addgroup -S app && adduser -S -G app -h /app app
WORKDIR /app

COPY --from=layers --chown=app:app /layers/dependencies/          ./
COPY --from=layers --chown=app:app /layers/spring-boot-loader/    ./
COPY --from=layers --chown=app:app /layers/snapshot-dependencies/ ./
COPY --from=layers --chown=app:app /layers/application/           ./

USER app
EXPOSE 8084

# MaxRAMPercentage makes the JVM respect the container's cgroup limit instead of
# sizing the heap from the host's total RAM.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=10 \
  CMD wget -qO- http://localhost:8084/actuator/health/readiness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
