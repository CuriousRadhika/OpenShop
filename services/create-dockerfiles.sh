#!/bin/bash

# Script to create Dockerfile for each service

set -e

echo "Creating Dockerfiles for all services..."

# List of services
services=(
    "userservice"
    "productservice"
    "orderservice"
    "paymentservice"
    "cartservice"
    "inventoryservice"
    "notificationservice"
    "shippingservice"
    "apigateway"
)

# Create Dockerfile for each service
for service in "${services[@]}"; do
    echo "Creating Dockerfile for $service..."
    
    cat > "$service/Dockerfile" << 'EOF'
FROM sapmachine:17

WORKDIR /app

# Copy the jar file
COPY target/*.jar app.jar

# Expose the port (will be overridden by specific service port)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

    echo "✓ Created Dockerfile for $service"
done

echo ""
echo "All Dockerfiles created successfully!"
echo ""
echo "Next steps:"
echo "1. Build all services: mvn clean install -DskipTests"
echo "2. Build Docker images: docker-compose build"
echo "3. Start services: docker-compose up -d"
