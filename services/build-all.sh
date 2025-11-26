#!/bin/bash

# Build script for all OpenShop services

set -e

# Load environment variables from .env file
if [ -f .env ]; then
    echo "Loading environment variables from .env file..."
    export $(grep -v '^#' .env | grep -v '^[[:space:]]*$' | xargs)
    echo "✓ Environment variables loaded"
    echo ""
fi

echo "============================================"
echo "OpenShop Build Script"
echo "============================================"
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Error: Maven is not installed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Maven found: $(mvn -version | head -n 1)${NC}"
echo ""

# Check if Docker is installed (required for PostgreSQL)
if ! command -v docker &> /dev/null; then
    echo -e "${YELLOW}Warning: Docker is not installed${NC}"
    echo -e "${YELLOW}PostgreSQL databases will not be available for local testing${NC}"
    echo ""
else
    echo -e "${GREEN}✓ Docker found: $(docker --version)${NC}"
    echo ""
    
    # Check if PostgreSQL containers are running
    echo "Checking PostgreSQL containers status..."
    postgres_count=$(docker ps --filter "name=postgres-" --format "{{.Names}}" | wc -l)
    
    if [ "$postgres_count" -eq 0 ]; then
        echo -e "${YELLOW}⚠ No PostgreSQL containers are running${NC}"
        echo -e "${YELLOW}  To start databases: docker-compose up -d postgres-user postgres-product postgres-order postgres-cart postgres-inventory postgres-payment postgres-notification postgres-shipping${NC}"
        echo ""
    else
        echo -e "${GREEN}✓ Found $postgres_count PostgreSQL container(s) running${NC}"
        docker ps --filter "name=postgres-" --format "  - {{.Names}} ({{.Status}})"
        echo ""
    fi
fi

# List of services to build
services=(
    "common-events"
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

# Build all services
echo "Building all services..."
echo "This may take a few minutes..."
echo ""

failed_services=()

for service in "${services[@]}"; do
    echo -e "${YELLOW}Building $service...${NC}"
    cd "$service"
    
    if mvn clean install -DskipTests; then
        echo -e "${GREEN}✓ $service built successfully${NC}"
    else
        echo -e "${RED}✗ $service build failed${NC}"
        failed_services+=("$service")
    fi
    
    cd ..
    echo ""
done

echo "============================================"
if [ ${#failed_services[@]} -eq 0 ]; then
    echo -e "${GREEN}✓ All services built successfully!${NC}"
    echo "============================================"
    echo ""
    echo -e "${GREEN}PostgreSQL Migration Complete!${NC}"
    echo "All services now use PostgreSQL for persistent storage."
    echo ""
    echo "Next steps:"
    echo ""
    echo "1. Start PostgreSQL databases (if not running):"
    echo "   docker-compose up -d postgres-user postgres-product postgres-order postgres-cart postgres-inventory postgres-payment postgres-notification postgres-shipping"
    echo ""
    echo "2. Run services locally: ./start-local.sh"
    echo "   - Uses 'local' profile with localhost addresses"
    echo "   - Ensure PostgreSQL containers are running on localhost ports 5432-5439"
    echo ""
    echo "3. Run individual service manually (if needed):"
    echo "   cd <service-directory>"
    echo "   mvn spring-boot:run -Dspring-boot.run.profiles=local"
    echo ""
    echo "4. Run with Docker Compose (for Kubernetes-like environment):"
    echo "   - Create Dockerfiles: ./create-dockerfiles.sh"
    echo "   - Build images: docker-compose build"
    echo "   - Start all services: docker-compose up -d"
    echo ""
    echo "5. Deploy to Kubernetes: See k8s/README.md"
    echo ""
    echo "6. Read migration guide: POSTGRESQL_MIGRATION_GUIDE.md"
else
    echo -e "${RED}✗ Build completed with failures${NC}"
    echo "============================================"
    echo ""
    echo "Failed services:"
    for service in "${failed_services[@]}"; do
        echo -e "  ${RED}✗ $service${NC}"
    done
    echo ""
    echo "Please check the error messages above and fix any issues."
    exit 1
fi
