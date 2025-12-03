# ======================================
# Multi-stage Dockerfile
# Stage 1: Build / Stage 2: Run (minimal image)
# ======================================

# Build stage
FROM gradle:8-jdk21 AS build
WORKDIR /app
COPY . .
RUN ./gradlew clean bootJar --no-daemon -x test

# Run stage (JRE only)
FROM eclipse-temurin:21-jre
WORKDIR /app

# Create non-root user for security
RUN groupadd --system appgroup && useradd --system -g appgroup appuser
USER appuser

# Copy JAR from build stage
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-default}", \
  "-jar", \
  "app.jar"]
