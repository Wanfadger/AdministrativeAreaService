FROM eclipse-temurin:21-jre-alpine

# Set the working directory
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the pre-built application jar
# Assuming the build step is done externally via Maven
COPY target/AdministrativeareaApi-0.0.1-SNAPSHOT.jar app.jar

# Expose the application port
EXPOSE 8084

# Run the app. Uses standard container support flags to respect memory limits.
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]