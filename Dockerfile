# --- Stage 1: Build Stage ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Install Azure CLI (needed by some Gradle subprojects)
RUN apt-get update && apt-get install -y \
    curl ca-certificates apt-transport-https lsb-release gnupg \
 && curl -sL https://packages.microsoft.com/keys/microsoft.asc | gpg --dearmor | tee /etc/apt/trusted.gpg.d/microsoft.gpg > /dev/null \
 && AZ_REPO=$(lsb_release -cs) \
 && echo "deb [arch=amd64,arm64,armhf] https://packages.microsoft.com/repos/azure-cli/ $AZ_REPO main" | tee /etc/apt/sources.list.d/azure-cli.list \
 && apt-get update && apt-get install -y azure-cli \
 && rm -rf /var/lib/apt/lists/*

# Copy Gradle wrapper and project metadata first
COPY gradlew settings.gradle* build.gradle* ./
COPY gradle/ gradle/
RUN chmod +x ./gradlew

# Copy Lombok config
COPY lombok.config ./

# Copy all source code
COPY buildSrc/ buildSrc/
COPY commons/ commons/
COPY services/ services/
COPY utilities/ utilities/

# Build the selected module (SERVICE passed at build time)
ARG SERVICE=platform-initializer
ARG GRADLE_ARGS=""
RUN ./gradlew ":${SERVICE}:bootJar" -x test --no-daemon $GRADLE_ARGS

# Copy the resulting JAR to a fixed path
RUN JAR_FILE=$(find . -path "*/${SERVICE}/build/libs/*.jar" -maxdepth 6 -type f | head -n1) \
 && test -n "$JAR_FILE" \
 && cp "$JAR_FILE" /app.jar

# --- Stage 2: Runtime Stage ---
FROM eclipse-temurin:21-jre
# Create non-root user
RUN useradd -r -u 1001 spring
USER 1001
WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app.jar /app/app.jar

# Expose default port
EXPOSE 8080

# Java options
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

# Entry point
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
