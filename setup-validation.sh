#!/bin/bash

# ============================================================================
# OpenShop Setup Validation Script
# ============================================================================
# This script checks if all prerequisites are installed and configured
# correctly before running OpenShop
# ============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
CHECKS_PASSED=0
CHECKS_FAILED=0
WARNINGS=0

# ============================================================================
# Helper Functions
# ============================================================================

print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
    ((CHECKS_PASSED++))
}

print_error() {
    echo -e "${RED}✗${NC} $1"
    ((CHECKS_FAILED++))
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
    ((WARNINGS++))
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

check_command() {
    if command -v "$1" &> /dev/null; then
        return 0
    else
        return 1
    fi
}

get_version() {
    local cmd=$1
    local version_flag=${2:---version}
    $cmd $version_flag 2>&1 | head -n 1
}

# ============================================================================
# Main Validation
# ============================================================================

clear
print_header "OpenShop Setup Validation"

echo "This script will check if your system meets all the requirements"
echo "to run OpenShop successfully."
echo ""
sleep 1

# ============================================================================
# Check Java
# ============================================================================

print_header "Checking Java Development Kit (JDK)"

if check_command java; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
    JAVA_MAJOR_VERSION=$(echo "$JAVA_VERSION" | cut -d'.' -f1)
    
    if [ "$JAVA_MAJOR_VERSION" -ge 17 ]; then
        print_success "Java $JAVA_VERSION is installed (Required: 17+)"
    else
        print_error "Java version $JAVA_VERSION is installed, but version 17+ is required"
        print_info "Install Java 17 or higher from: https://adoptium.net/"
    fi
else
    print_error "Java is not installed"
    print_info "Install Java 17+ from: https://adoptium.net/"
fi

echo ""

# ============================================================================
# Check Maven
# ============================================================================

print_header "Checking Apache Maven"

if check_command mvn; then
    MVN_VERSION=$(mvn -version 2>&1 | head -n 1 | awk '{print $3}')
    MVN_MAJOR_VERSION=$(echo "$MVN_VERSION" | cut -d'.' -f1)
    MVN_MINOR_VERSION=$(echo "$MVN_VERSION" | cut -d'.' -f2)
    
    if [ "$MVN_MAJOR_VERSION" -ge 3 ] && [ "$MVN_MINOR_VERSION" -ge 8 ]; then
        print_success "Maven $MVN_VERSION is installed (Required: 3.8+)"
    else
        print_warning "Maven version $MVN_VERSION may be too old (Recommended: 3.8+)"
    fi
else
    print_error "Maven is not installed"
    print_info "Install Maven from: https://maven.apache.org/download.cgi"
fi

echo ""

# ============================================================================
# Check Node.js
# ============================================================================

print_header "Checking Node.js"

if check_command node; then
    NODE_VERSION=$(node --version | cut -d'v' -f2)
    NODE_MAJOR_VERSION=$(echo "$NODE_VERSION" | cut -d'.' -f1)
    
    if [ "$NODE_MAJOR_VERSION" -ge 18 ]; then
        print_success "Node.js v$NODE_VERSION is installed (Required: 18+)"
    else
        print_error "Node.js version v$NODE_VERSION is installed, but version 18+ is required"
        print_info "Install Node.js 18+ from: https://nodejs.org/"
    fi
else
    print_error "Node.js is not installed"
    print_info "Install Node.js 18+ from: https://nodejs.org/"
fi

echo ""

# ============================================================================
# Check npm
# ============================================================================

print_header "Checking npm"

if check_command npm; then
    NPM_VERSION=$(npm --version)
    print_success "npm v$NPM_VERSION is installed"
else
    print_error "npm is not installed (usually comes with Node.js)"
fi

echo ""

# ============================================================================
# Check Docker
# ============================================================================

print_header "Checking Docker"

if check_command docker; then
    DOCKER_VERSION=$(docker --version | awk '{print $3}' | sed 's/,$//')
    print_success "Docker $DOCKER_VERSION is installed"
    
    # Check if Docker daemon is running
    if docker info &> /dev/null; then
        print_success "Docker daemon is running"
    else
        print_error "Docker daemon is not running"
        print_info "Start Docker Desktop or run: sudo systemctl start docker"
    fi
else
    print_error "Docker is not installed"
    print_info "Install Docker from: https://www.docker.com/products/docker-desktop"
fi

echo ""

# ============================================================================
# Check Docker Compose
# ============================================================================

print_header "Checking Docker Compose"

if check_command docker-compose || docker compose version &> /dev/null; then
    if check_command docker-compose; then
        COMPOSE_VERSION=$(docker-compose --version | awk '{print $3}' | sed 's/,$//')
        print_success "Docker Compose v$COMPOSE_VERSION is installed"
    else
        COMPOSE_VERSION=$(docker compose version --short)
        print_success "Docker Compose v$COMPOSE_VERSION is installed (plugin)"
    fi
else
    print_error "Docker Compose is not installed"
    print_info "Docker Compose should come with Docker Desktop"
fi

echo ""

# ============================================================================
# Check Git
# ============================================================================

print_header "Checking Git"

if check_command git; then
    GIT_VERSION=$(git --version | awk '{print $3}')
    print_success "Git $GIT_VERSION is installed"
else
    print_error "Git is not installed"
    print_info "Install Git from: https://git-scm.com/downloads"
fi

echo ""

# ============================================================================
# Check System Resources
# ============================================================================

print_header "Checking System Resources"

# Check RAM (different commands for different OS)
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    TOTAL_RAM=$(sysctl hw.memsize | awk '{print int($2/1024/1024/1024)}')
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux
    TOTAL_RAM=$(free -g | awk '/^Mem:/{print $2}')
else
    # Windows (WSL or other)
    TOTAL_RAM=$(cat /proc/meminfo | grep MemTotal | awk '{print int($2/1024/1024)}')
fi

if [ "$TOTAL_RAM" -ge 16 ]; then
    print_success "System has ${TOTAL_RAM}GB RAM (Recommended: 16GB+)"
elif [ "$TOTAL_RAM" -ge 8 ]; then
    print_warning "System has ${TOTAL_RAM}GB RAM (Minimum: 8GB, Recommended: 16GB)"
else
    print_error "System has ${TOTAL_RAM}GB RAM (Minimum required: 8GB)"
fi

# Check CPU cores
CPU_CORES=$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo "unknown")
if [ "$CPU_CORES" != "unknown" ]; then
    if [ "$CPU_CORES" -ge 4 ]; then
        print_success "System has $CPU_CORES CPU cores (Minimum: 4)"
    else
        print_warning "System has $CPU_CORES CPU cores (Recommended: 4+)"
    fi
fi

# Check disk space
AVAILABLE_SPACE=$(df -BG . | tail -1 | awk '{print $4}' | sed 's/G//')
if [ "$AVAILABLE_SPACE" -ge 10 ]; then
    print_success "Available disk space: ${AVAILABLE_SPACE}GB (Minimum: 10GB)"
else
    print_warning "Available disk space: ${AVAILABLE_SPACE}GB (Recommended: 10GB+)"
fi

echo ""

# ============================================================================
# Check Project Files
# ============================================================================

print_header "Checking Project Files"

# Check if we're in the right directory
if [ ! -f "README.md" ] || [ ! -d "services" ] || [ ! -d "ui" ]; then
    print_error "Not in OpenShop root directory"
    print_info "Run this script from the OpenShop root directory"
else
    print_success "In OpenShop root directory"
fi

# Check for .env file
if [ -f ".env" ]; then
    print_success ".env file exists"
else
    print_warning ".env file not found"
    print_info "Copy .env.example to .env: cp .env.example .env"
fi

# Check for key directories
if [ -d "services" ]; then
    print_success "services/ directory exists"
else
    print_error "services/ directory not found"
fi

if [ -d "ui" ]; then
    print_success "ui/ directory exists"
else
    print_error "ui/ directory not found"
fi

# Check for executable scripts
if [ -x "services/start-databases.sh" ]; then
    print_success "services/start-databases.sh is executable"
else
    print_warning "services/start-databases.sh is not executable"
    print_info "Run: chmod +x services/start-databases.sh"
fi

if [ -x "services/build-all.sh" ]; then
    print_success "services/build-all.sh is executable"
else
    print_warning "services/build-all.sh is not executable"
    print_info "Run: chmod +x services/build-all.sh"
fi

if [ -x "services/start-local.sh" ]; then
    print_success "services/start-local.sh is executable"
else
    print_warning "services/start-local.sh is not executable"
    print_info "Run: chmod +x services/start-local.sh"
fi

echo ""

# ============================================================================
# Check Ports Availability
# ============================================================================

print_header "Checking Port Availability"

REQUIRED_PORTS=(5173 8080 8081 8082 8083 8084 8085 8086 8087 8088 5432 5433 5434 5435 5436 5437 5438 5439 9092 2181)
PORTS_IN_USE=()

for port in "${REQUIRED_PORTS[@]}"; do
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1 || netstat -an 2>/dev/null | grep ":$port " | grep LISTEN >/dev/null; then
        PORTS_IN_USE+=($port)
    fi
done

if [ ${#PORTS_IN_USE[@]} -eq 0 ]; then
    print_success "All required ports are available"
else
    print_warning "Some ports are already in use: ${PORTS_IN_USE[*]}"
    print_info "You may need to stop services using these ports"
fi

echo ""

# ============================================================================
# Summary
# ============================================================================

print_header "Validation Summary"

echo "Checks passed: ${GREEN}$CHECKS_PASSED${NC}"
echo "Checks failed: ${RED}$CHECKS_FAILED${NC}"
echo "Warnings: ${YELLOW}$WARNINGS${NC}"
echo ""

if [ $CHECKS_FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ Your system is ready to run OpenShop!${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Copy .env.example to .env: cp .env.example .env"
    echo "2. Start infrastructure: cd services && ./start-databases.sh"
    echo "3. Build services: ./build-all.sh"
    echo "4. Start services: ./start-local.sh"
    echo "5. Start frontend: cd ui && npm install && npm run dev"
    echo ""
    exit 0
else
    echo -e "${RED}✗ Some requirements are missing${NC}"
    echo ""
    echo "Please install the missing requirements and run this script again."
    echo "Refer to the CONTRIBUTING.md file for detailed installation instructions."
    echo ""
    exit 1
fi
