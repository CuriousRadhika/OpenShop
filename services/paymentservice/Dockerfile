FROM sapmachine:17

WORKDIR /app

# Copy the jar file
COPY target/*.jar app.jar

# Expose the port
EXPOSE 8084

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
