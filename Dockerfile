# Stage 1: Build the application
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -B clean package -DskipTests


# Stage 2: Runtime image
FROM eclipse-temurin:21-jre

WORKDIR /app

# Update OS packages to include security fixes
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Create a dedicated non-root user with fixed numeric IDs
RUN groupadd --system --gid 10001 appgroup && \
    useradd --system --uid 10001 --gid 10001 appuser

# Copy the packaged application with correct ownership
COPY --from=build --chown=10001:10001 /app/target/*.jar app.jar

# Run the application as a non-root user
USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]