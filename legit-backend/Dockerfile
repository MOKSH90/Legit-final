# Stage 1: Build the application
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copy the Gradle wrapper files and project configuration files
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Download/cache dependencies (ignore failures if no sources are copied yet)
RUN ./gradlew dependencies --no-daemon || true

# Copy the rest of the application source files
COPY src src

# Run the installDist task to package the Ktor app
RUN ./gradlew installDist --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:25-jre
WORKDIR /app

# Copy the distribution from the build stage
COPY --from=build /app/build/install/legit /app

# Expose Ktor port (8080)
EXPOSE 8080

# Execute the start script
ENTRYPOINT ["/app/bin/legit"]
