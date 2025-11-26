# Contributing


## 📑 Table of Contents

- [Prerequisites](#-prerequisites)
- [Quick Start Guide](#-quick-start-guide)
- [Detailed Setup Instructions](#-detailed-setup-instructions)
- [Service Documentation](#-service-documentation)
- [API Reference](#-api-reference)
- [Development Guide](#-development-guide)
- [Debugging](#-debugging)
- [Database Management](#-database-management)
- [Testing](#-testing)
- [Deployment Options](#-deployment-options)
- [Security](#-security)
- [Monitoring & Health Checks](#-monitoring--health-checks)
- [Troubleshooting](#-troubleshooting)
- [Performance Optimization](#-performance-optimization)
- [Project Structure](#-project-structure)
- [Contributing](#-contributing)

---
## 🔧 Prerequisites

### Required Software

#### For Local Development
```bash
# Java Development Kit (JDK) 17 or higher
java -version
# Expected: openjdk version "17.x.x" or higher

# Apache Maven 3.6 or higher
mvn -version
# Expected: Apache Maven 3.6.x or higher

# Docker Desktop (for PostgreSQL databases)
docker --version
# Expected: Docker version 20.x.x or higher

# Git
git --version
```

#### For Kubernetes Deployment (Optional)
```bash
# Minikube or access to a Kubernetes cluster
minikube version

# kubectl CLI
kubectl version --client
```

### System Requirements

- **RAM**: Minimum 8GB (16GB recommended for full stack)
- **CPU**: Minimum 4 cores
- **Disk Space**: 5GB free space
- **OS**: macOS, Linux, or Windows with WSL2

### Installation Guides

<details>
<summary><b>📦 Installing Prerequisites on macOS</b></summary>

```bash
# Install Homebrew (if not installed)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install Java 17
brew install openjdk@17
sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk

# Install Maven
brew install maven

# Install Docker Desktop
brew install --cask docker

# Start Docker Desktop from Applications
```
</details>

<details>
<summary><b>🐧 Installing Prerequisites on Linux (Ubuntu/Debian)</b></summary>

```bash
# Update package list
sudo apt update

# Install Java 17
sudo apt install openjdk-17-jdk

# Install Maven
sudo apt install maven

# Install Docker
sudo apt install docker.io docker-compose
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker $USER

# Log out and back in for group changes to take effect
```
</details>

<details>
<summary><b>🪟 Installing Prerequisites on Windows</b></summary>

1. **Install Java 17**
    - Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [Adoptium](https://adoptium.net/)
    - Add to PATH: `C:\Program Files\Java\jdk-17\bin`

2. **Install Maven**
    - Download from [Apache Maven](https://maven.apache.org/download.cgi)
    - Extract and add to PATH: `C:\apache-maven-3.x.x\bin`

3. **Install Docker Desktop**
    - Download from [Docker](https://www.docker.com/products/docker-desktop)
    - Ensure WSL2 backend is enabled

4. **Install Git**
    - Download from [Git SCM](https://git-scm.com/download/win)
</details>

---

## 🚀 Quick Start Guide

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/openshop.git
cd openshop
```

### 2. Start PostgreSQL Databases

```bash
# Start all PostgreSQL containers
./start-databases.sh

# Verify databases are running
docker ps --filter "name=postgres-"
```

**Expected Output:**
```
postgres-user-db     (healthy)
postgres-product-db  (healthy)
postgres-order-db    (healthy)
postgres-cart-db     (healthy)
postgres-inventory-db (healthy)
postgres-payment-db  (healthy)
postgres-notification-db (healthy)
postgres-shipping-db (healthy)
```

### 3. Build All Services

```bash
# Build all microservices
./build-all.sh
```

This will:
- Compile all Java code
- Run dependency resolution
- Package services as JAR files
- Skip tests for faster build (tests can be run separately)

### 4. Start All Services

```bash
# Start services in separate terminal windows
./start-local.sh
```

This script will:
1. Check prerequisites (Java, Maven, Docker)
2. Verify PostgreSQL databases are running
3. Start services in dependency order
4. Wait for each service to be healthy before starting the next
5. Open a new terminal window for each service

**Note**: On first run, services will automatically create database schemas.

### 5. Verify Services

```bash
# Check all services are healthy
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8081/actuator/health  # User Service
curl http://localhost:8082/actuator/health  # Product Service
# ... and so on
```

### 6. Test the API

```bash
# Register a new customer
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "username": "john_customer",
    "password": "password123",
    "email": "john@example.com",
    "role": "CUSTOMER"
  }'

# Login and get JWT token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_customer",
    "password": "password123"
  }'
```

**Save the JWT token** from the response - you'll need it for authenticated requests.

---

## 📖 Detailed Setup Instructions

### Step-by-Step Local Development Setup

#### 1. Environment Configuration

The project uses environment variables for configuration. These are automatically loaded from the `.env` file:

```bash
# View current configuration
cat .env
```

**Key Environment Variables:**

```properties
# Database Configuration
DB_USER=openshop_user
DB_PASSWORD=openshop_password

# JWT Configuration
JWT_SECRET=your-secret-key-change-this-in-production
JWT_EXPIRATION=86400000  # 24 hours in milliseconds

# Service URLs (auto-configured for local development)
USER_SERVICE_URL=http://localhost:8081
PRODUCT_SERVICE_URL=http://localhost:8082
ORDER_SERVICE_URL=http://localhost:8083
# ... etc
```

#### 2. Database Setup

**Starting Databases:**

```bash
./start-databases.sh
```

This creates 8 PostgreSQL containers:

| Service | Database | Port | Container Name |
|---------|----------|------|----------------|
| User | openshop_user_db | 5432 | postgres-user-db |
| Product | openshop_product_db | 5433 | postgres-product-db |
| Order | openshop_order_db | 5434 | postgres-order-db |
| Cart | openshop_cart_db | 5435 | postgres-cart-db |
| Inventory | openshop_inventory_db | 5436 | postgres-inventory-db |
| Payment | openshop_payment_db | 5437 | postgres-payment-db |
| Notification | openshop_notification_db | 5438 | postgres-notification-db |
| Shipping | openshop_shipping_db | 5439 | postgres-shipping-db |

**Database Connection Details:**
```
Host: localhost
Username: openshop_user
Password: openshop_password
```

**Managing Databases:**

```bash
# View database status
docker ps --filter "name=postgres-"

# View logs for a specific database
docker logs postgres-user-db

# Connect to a database using psql
docker exec -it postgres-user-db psql -U openshop_user -d openshop_user_db

# Stop all databases
./stop-databases.sh

# Remove database volumes (clean slate)
docker-compose down -v
```

#### 3. Building Services

**Build All Services:**
```bash
./build-all.sh
```

**Build Specific Service:**
```bash
cd userservice
mvn clean install
```

**Build with Tests:**
```bash
mvn clean install  # (without -DskipTests)
```

**Build Options:**
```bash
# Clean build (removes all previous builds)
mvn clean install

# Skip tests (faster)
mvn clean install -DskipTests

# Build without running tests but compile them
mvn clean install -DskipTests -Dmaven.test.skip=false

# Parallel build (faster on multi-core systems)
mvn clean install -T 4  # Uses 4 threads
```

#### 4. Running Services

**Automated Start (Recommended):**
```bash
./start-local.sh
```

**Manual Start (for debugging specific service):**
```bash
cd userservice
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Running Multiple Services Manually:**
```bash
# Terminal 1 - User Service
cd userservice && mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 2 - Product Service
cd productservice && mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 3 - API Gateway
cd apigateway && mvn spring-boot:run -Dspring-boot.run.profiles=local

# Continue for other services...
```

---

## 📚 Service Documentation

### API Gateway (Port 8080)

**Purpose**: Central entry point for all client requests

**Features**:
- JWT token validation
- Request routing to appropriate microservices
- User context extraction and header propagation
- CORS configuration
- Rate limiting (configurable)

**Configuration**: `apigateway/src/main/resources/application.yml`

**Routes**:
```yaml
/api/auth/**     → User Service (No auth required)
/api/users/**    → User Service (Auth required)
/api/products/** → Product Service
/api/cart/**     → Cart Service (Auth required)
/api/orders/**   → Order Service (Auth required)
/api/inventory/**→ Inventory Service
/api/payments/** → Payment Service
/api/shipping/** → Shipping Service
/api/notifications/** → Notification Service
```

### User Service (Port 8081)

**Purpose**: User authentication and management

**Features**:
- User registration (Customer, Seller, Admin)
- Login with JWT token generation
- Profile management (view/update)
- Role-based access control
- Password encryption (BCrypt)

**Database**: `openshop_user_db` (PostgreSQL on port 5432)

**Key Endpoints**:
```
POST   /api/auth/register    - Register new user
POST   /api/auth/login       - Login and get JWT token
GET    /api/users/me         - Get current user profile
PUT    /api/users/me         - Update current user profile
GET    /api/users/{id}       - Get user by ID (Admin/Self only)
DELETE /api/users/{id}       - Delete user (Admin only)
```

**Sample Entities**:
```java
User {
    id: Long
    name: String
    username: String (unique)
    email: String
    password: String (encrypted)
    role: Enum (ADMIN, SELLER, CUSTOMER)
    createdAt: LocalDateTime
    updatedAt: LocalDateTime
}
```

### Product Service (Port 8082)

**Purpose**: Product catalog management with GraphQL support

**Features**:
- RESTful API for basic CRUD operations
- GraphQL API for flexible queries
- Seller-based product ownership
- Inventory integration
- Product search and filtering

**Database**: `openshop_product_db` (PostgreSQL on port 5433)

**REST Endpoints**:
```
POST   /api/products           - Create product (Seller only)
GET    /api/products           - Get all products
GET    /api/products/{id}      - Get product by ID
PUT    /api/products/update/{id} - Update product (Owner only)
```

**GraphQL Endpoint**:
```
POST   /api/products/graphql   - GraphQL queries and mutations
```

**GraphQL Examples**:

<details>
<summary><b>Query All Products</b></summary>

```graphql
query {
  products {
    id
    name
    description
    category
    price
    currency
    sku
    sellerId
    imageUrl
    status
    createdAt
    updatedAt
  }
}
```
</details>

<details>
<summary><b>Query Product by ID</b></summary>

```graphql
query($id: ID!) {
  product(id: $id) {
    id
    name
    description
    price
    status
  }
}

# Variables
{
  "id": "123e4567-e89b-12d3-a456-426614174000"
}
```
</details>

<details>
<summary><b>Create Product (Mutation)</b></summary>

```graphql
mutation($input: ProductInput!) {
  createProduct(input: $input) {
    id
    name
    price
    status
  }
}

# Variables
{
  "input": {
    "name": "Gaming Laptop",
    "description": "High-performance gaming laptop",
    "category": "Electronics",
    "price": 1299.99,
    "currency": "USD",
    "sku": "GMLAP-001",
    "imageUrl": "https://example.com/laptop.jpg",
    "status": "ACTIVE"
  }
}
```
</details>

### Order Service (Port 8083)

**Purpose**: Order processing with Apache Camel Saga pattern

**Features**:
- Order placement from cart
- Distributed transaction coordination (Saga)
- Inventory reservation
- Payment processing
- Automatic compensation on failure
- Order status tracking

**Database**: `openshop_order_db` (PostgreSQL on port 5434)

**Key Endpoints**:
```
POST   /api/orders/placeCart        - Create order from cart
GET    /api/orders                  - Get all orders
GET    /api/orders/user             - Get user's orders
POST   /api/orders/compensate/{id}  - Trigger compensation
```

**Saga Flow**:
```
1. Reserve Inventory → Success/Fail
   ↓ Success
2. Process Payment → Success/Fail
   ↓ Fail
3. Compensation: Release Inventory + Cancel Payment
```

**Order States**:
- `PLACED` - Initial state
- `CONFIRMED` - Payment successful, inventory reserved
- `FAILED` - Order failed
- `OUT_OF_STOCK` - Insufficient inventory
- `CANCELLED_PAYMENT_FAILED` - Payment failed, compensated

### Cart Service (Port 8085)

**Purpose**: Shopping cart management

**Features**:
- Add/remove/update cart items
- Cart persistence per user
- Checkout integration with Order Service
- Price calculation

**Database**: `openshop_cart_db` (PostgreSQL on port 5435)

**Key Endpoints**:
```
GET    /api/cart           - Get user's cart
POST   /api/cart/add       - Add item to cart
DELETE /api/cart/remove/{itemId} - Remove item from cart
DELETE /api/cart/clear     - Clear entire cart
POST   /api/cart/checkout  - Checkout and create orders
```

### Inventory Service (Port 8086)

**Purpose**: Stock management and reservation

**Features**:
- Inventory tracking per product
- Stock increase/decrease
- Reservation for order processing
- Low stock alerts
- Seller access control

**Database**: `openshop_inventory_db` (PostgreSQL on port 5436)

**Key Endpoints**:
```
POST   /api/inventory/create         - Create inventory record (Seller)
GET    /api/inventory/{productId}    - Get inventory for product
POST   /api/inventory/increase       - Add stock (Seller)
POST   /api/inventory/reduce         - Remove stock (Internal)
```

### Payment Service (Port 8084)

**Purpose**: Payment processing with idempotency

**Features**:
- Payment initiation
- Mock payment processing (success/failure)
- Idempotency key support
- Payment status tracking
- Cancellation/refund (compensation)

**Database**: `openshop_payment_db` (PostgreSQL on port 5437)

**Key Endpoints**:
```
POST   /api/payments/initiate       - Initiate payment
POST   /api/payments/process/{orderId} - Process payment
GET    /api/payments/{orderId}      - Get payment status
POST   /api/payments/cancel/{orderId} - Cancel payment
```

**Payment States**:
- `PENDING` - Payment initiated
- `SUCCESS` - Payment successful
- `FAILED` - Payment failed
- `CANCELLED` - Payment cancelled (compensation)

### Notification Service (Port 8087)

**Purpose**: Multi-channel user notifications

**Features**:
- Email notifications (mock)
- SMS notifications (mock)
- In-app notifications
- Order status updates
- Notification history

**Database**: `openshop_notification_db` (PostgreSQL on port 5438)

**Key Endpoints**:
```
POST   /api/notifications/send  - Send notification
GET    /api/notifications       - Get user notifications
```

### Shipping Service (Port 8088)

**Purpose**: Shipment tracking

**Features**:
- Shipment creation
- Tracking number generation
- Status updates
- Address management

**Database**: `openshop_shipping_db` (PostgreSQL on port 5439)

**Key Endpoints**:
```
POST   /api/shipping                 - Create shipment
GET    /api/shipping/{orderId}       - Get shipment by order
PUT    /api/shipping/{id}/status     - Update shipment status
```

**Shipment States**:
- `CREATED` - Shipment created
- `IN_TRANSIT` - Package in transit
- `DELIVERED` - Package delivered

### Kafka Message Broker (Port 9092)

**Purpose**: Event-driven communication for Saga pattern

**Features**:
- Asynchronous message passing between services
- Event-driven Saga orchestration
- Distributed transaction coordination
- Fault tolerance and message replay
- Topic-based publish-subscribe pattern

**Components**:
- **Kafka**: Message broker (Port 9092)
- **Zookeeper**: Cluster coordination (Port 2181)

**Kafka Topics**:

| Topic Name | Producer | Consumer | Purpose |
|------------|----------|----------|---------|
| `order.payment.request` | Order Service | Payment Service | Request payment processing |
| `payment.order.response` | Payment Service | Order Service | Payment result notification |
| `order.inventory.reserve.request` | Order Service | Inventory Service | Request inventory reservation |
| `inventory.order.reserve.response` | Inventory Service | Order Service | Inventory reservation result |
| `order.shipping.request` | Order Service | Shipping Service | Request shipment creation |
| `shipping.order.response` | Shipping Service | Order Service | Shipment status update |
| `order.notification.request` | Order Service | Notification Service | Send order notifications |
| `payment.gateway.webhook` | External | Payment Service | Payment gateway webhooks |
| `payment.refund.request` | Order Service | Payment Service | Request payment refund |
| `order.payment.refund.response` | Payment Service | Order Service | Refund status update |

**Saga Flow with Kafka**:

```
Order Created
    ↓
Order Service → [order.inventory.reserve.request] → Inventory Service
    ↓
Inventory Service → [inventory.order.reserve.response] → Order Service
    ↓ (if success)
Order Service → [order.payment.request] → Payment Service
    ↓
Payment Service → [payment.order.response] → Order Service
    ↓ (if success)
Order Service → [order.shipping.request] → Shipping Service
    ↓
Shipping Service → [shipping.order.response] → Order Service
    ↓ (if any failure)
Compensation: Rollback all completed steps
```

**Managing Kafka**:

```bash
# Reset all Kafka topics
./reset-kafka-topics.sh

# List all topics
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Describe a specific topic
docker exec kafka kafka-topics --describe --topic order.payment.request --bootstrap-server localhost:9092

# View messages in a topic
docker exec kafka kafka-console-consumer --topic order.payment.request --from-beginning --bootstrap-server localhost:9092

# Produce test message
docker exec -it kafka kafka-console-producer --topic order.payment.request --bootstrap-server localhost:9092

# View Kafka logs
docker logs kafka -f

# Check consumer groups
docker exec kafka kafka-consumer-groups --list --bootstrap-server localhost:9092

# Describe consumer group
docker exec kafka kafka-consumer-groups --describe --group order-service-group --bootstrap-server localhost:9092
```

**Configuration** (in service `application.yml`):

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: order-service-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

**Common Events Module**:

The `common-events` module contains shared event classes and Kafka topic constants used across services:

```java
// Topic constants
public class KafkaTopics {
    public static final String ORDER_PAYMENT_REQUEST = "order.payment.request";
    public static final String PAYMENT_ORDER_RESPONSE = "payment.order.response";
    public static final String ORDER_INVENTORY_RESERVE_REQUEST = "order.inventory.reserve.request";
    // ... etc
}

// Event classes
@Data
public class PaymentRequestEvent {
    private String orderId;
    private Long userId;
    private BigDecimal amount;
    private String correlationId;
}
```

---

## 🔌 API Reference

### Authentication Flow

#### 1. Register a User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jane Smith",
    "username": "jane_seller",
    "password": "securepass123",
    "email": "jane@example.com",
    "role": "SELLER"
  }'
```

**Response**:
```json
{
  "id": 1,
  "name": "Jane Smith",
  "username": "jane_seller",
  "email": "jane@example.com",
  "role": "SELLER",
  "createdAt": "2025-01-13T10:00:00",
  "updatedAt": "2025-01-13T10:00:00"
}
```

#### 2. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jane_seller",
    "password": "securepass123"
  }'
```

**Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresAt": "2025-01-14T10:00:00",
  "user": {
    "id": 1,
    "username": "jane_seller",
    "role": "SELLER"
  }
}
```

### Complete E-commerce Flow Example

#### Step 1: Seller Creates Products

```bash
# Login as seller and save token
TOKEN="your-seller-jwt-token"

# Create product 1
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "MacBook Pro 16\"",
    "description": "Apple M3 Pro, 36GB RAM, 1TB SSD",
    "category": "Laptops",
    "price": 2499.00,
    "currency": "USD",
    "sku": "MBP-M3-36-1TB",
    "imageUrl": "https://example.com/mbp.jpg",
    "status": "ACTIVE"
  }'

# Create product 2
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Magic Mouse",
    "description": "Wireless rechargeable mouse",
    "category": "Accessories",
    "price": 79.00,
    "currency": "USD",
    "sku": "MM-WHT",
    "imageUrl": "https://example.com/mouse.jpg",
    "status": "ACTIVE"
  }'
```

#### Step 2: Add Initial Inventory

```bash
# Add inventory for MacBook
curl -X POST http://localhost:8080/api/inventory/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "productId": "product-uuid-here",
    "quantity": 50
  }'

# Add inventory for Mouse
curl -X POST http://localhost:8080/api/inventory/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "productId": "mouse-product-uuid",
    "quantity": 200
  }'
```

#### Step 3: Customer Browses Products

```bash
# Register as customer
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Bob Customer",
    "username": "bob_buyer",
    "password": "password123",
    "email": "bob@example.com",
    "role": "CUSTOMER"
  }'

# Login as customer
CUSTOMER_TOKEN="customer-jwt-token"

# Get all products
curl http://localhost:8080/api/products \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"

# Get specific product
curl http://localhost:8080/api/products/{product-id} \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

#### Step 4: Add to Cart

```bash
# Add MacBook to cart
curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "productId": "macbook-uuid",
    "quantity": 1,
    "price": 2499.00
  }'

# Add Mouse to cart
curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "productId": "mouse-uuid",
    "quantity": 2,
    "price": 79.00
  }'

# View cart
curl http://localhost:8080/api/cart \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

#### Step 5: Checkout

```bash
# Checkout cart (creates orders, reserves inventory, processes payment)
curl -X POST http://localhost:8080/api/cart/checkout \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

**Response**:
```json
{
  "message": "Checkout completed successfully",
  "orderIds": ["order-uuid-1", "order-uuid-2"],
  "totalAmount": 2657.00
}
```

#### Step 6: Check Order Status

```bash
# Get all user orders
curl http://localhost:8080/api/orders/user \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"

# Check payment status
curl http://localhost:8080/api/payments/{order-id} \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"

# Check shipment status
curl http://localhost:8080/api/shipping/{order-id} \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

### Using GraphQL for Products

```bash
# Query products with GraphQL
curl -X POST http://localhost:8080/api/products/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query { products { id name price category status } }"
  }'

# Query specific product
curl -X POST http://localhost:8080/api/products/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query($id: ID!) { product(id: $id) { id name description price } }",
    "variables": { "id": "product-uuid" }
  }'
```

---

## 💻 Development Guide

### IDE Setup

#### IntelliJ IDEA (Recommended)

1. **Import Project**:
    - File → Open → Select `openshop` directory
    - Choose "Maven" when prompted
    - Wait for dependency resolution

2. **Configure JDK**:
    - File → Project Structure → Project
    - Set SDK to Java 17
    - Set language level to 17

3. **Enable Annotation Processing**:
    - Preferences → Build, Execution, Deployment → Compiler → Annotation Processors
    - Check "Enable annotation processing"

4. **Run Configuration** (Example for User Service):
    - Run → Edit Configurations → Add New → Spring Boot
    - Main class: `com.openshop.userservice.UserServiceApplication`
    - Working directory: `$MODULE_WORKING_DIR$`
    - Environment variables: `SPRING_PROFILES_ACTIVE=local`

#### VS Code

1. **Install Extensions**:
    - Java Extension Pack
    - Spring Boot Extension Pack
    - Maven for Java

2. **Open Project**:
    - File → Open Folder → Select `openshop`

3. **Run Service**:
    - Open service's main application class
    - Click "Run" above the main method
    - Or use integrated terminal: `mvn spring-boot:run`

#### Eclipse

1. **Import Maven Project**:
    - File → Import → Maven → Existing Maven Projects
    - Select root directory

2. **Update Maven Project**:
    - Right-click project → Maven → Update Project

3. **Run Configuration**:
    - Run → Run Configurations → Spring Boot App
    - Select main class and profile

### Code Structure

#### Typical Service Structure

```
userservice/
├── src/
│   ├── main/
│   │   ├── java/com/openshop/userservice/
│   │   │   ├── UserServiceApplication.java    # Main application class
│   │   │   ├── config/                        # Configuration classes
│   │   │   │   ├── SecurityConfig.java        # Security configuration
│   │   │   │   └── JwtConfig.java             # JWT configuration
│   │   │   ├── controller/                    # REST controllers
│   │   │   │   ├── AuthController.java        # Authentication endpoints
│   │   │   │   └── UserController.java        # User management endpoints
│   │   │   ├── dto/                           # Data Transfer Objects
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── LoginResponse.java
│   │   │   │   └── UserDTO.java
│   │   │   ├── entity/                        # JPA entities
│   │   │   │   └── User.java
│   │   │   ├── repository/                    # Database repositories
│   │   │   │   └── UserRepository.java
│   │   │   ├── service/                       # Business logic
│   │   │   │   └── UserService.java
│   │   │   └── util/                          # Utility classes
│   │   │       └── JwtUtil.java
│   │   └── resources/
│   │       ├── application.yml                # Main configuration
│   │       ├── application-local.yml          # Local profile
│   │       └── application-prod.yml           # Production profile
│   └── test/
│       └── java/com/openshop/userservice/
│           ├── UserServiceApplicationTests.java
│           ├── controller/                    # Controller tests
│           ├── service/                       # Service tests
│           └── repository/                    # Repository tests
├── pom.xml                                    # Maven dependencies
└── Dockerfile                                 # Docker image definition
```

### Adding a New Feature

#### Example: Adding Product Reviews

1. **Create Entity** (`productservice/entity/Review.java`):
```java
@Entity
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID productId;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private Integer rating;  // 1-5
    
    @Column(length = 1000)
    private String comment;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    // Getters and setters
}
```

2. **Create Repository**:
```java
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByProductId(UUID productId);
    List<Review> findByUserId(Long userId);
}
```

3. **Create Service**:
```java
@Service
public class ReviewService {
    @Autowired
    private ReviewRepository reviewRepository;
    
    public Review createReview(Review review) {
        // Validation logic
        return reviewRepository.save(review);
    }
    
    public List<Review> getProductReviews(UUID productId) {
        return reviewRepository.findByProductId(productId);
    }
}
```

4. **Create Controller**:
```java
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    @Autowired
    private ReviewService reviewService;
    
    @PostMapping
    public ResponseEntity<Review> createReview(@RequestBody Review review) {
        return ResponseEntity.ok(reviewService.createReview(review));
    }
    
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Review>> getProductReviews(@PathVariable UUID productId) {
        return ResponseEntity.ok(reviewService.getProductReviews(productId));
    }
}
```

5. **Add Route to API Gateway** (`apigateway/application.yml`):
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: review-service
          uri: ${PRODUCT_SERVICE_URL:http://localhost:8082}
          predicates:
            - Path=/api/reviews/**
```

6. **Test the Feature**:
```bash
# Create review
curl -X POST http://localhost:8080/api/reviews \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "productId": "product-uuid",
    "rating": 5,
    "comment": "Excellent product!"
  }'
```

### Hot Reload Development

Spring Boot DevTools enables automatic restart on code changes:

1. **Ensure DevTools is included** (already in pom.xml):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

2. **IntelliJ IDEA Configuration**:
    - Preferences → Build, Execution, Deployment → Compiler
    - Check "Build project automatically"
    - Preferences → Advanced Settings
    - Check "Allow auto-make to start even if developed application is currently running"

3. **VS Code**: Save files and Maven will auto-compile

---

## 🐛 Debugging

### Debugging a Service

#### IntelliJ IDEA

1. **Set Breakpoints**: Click in the gutter next to line numbers
2. **Debug Mode**: Click the debug icon next to the run button
3. **Or use Remote Debugging**:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
   ```
   Then attach debugger to localhost:5005

#### VS Code

1. **Create launch.json**:
```json
{
  "type": "java",
  "name": "Debug User Service",
  "request": "attach",
  "hostName": "localhost",
  "port": 5005
}
```

2. **Run with Debug Port**:
```bash
cd userservice
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

3. **Attach Debugger**: Press F5 in VS Code

### Logging

#### Viewing Logs

```bash
# View specific service logs (if running via start-local.sh)
# Check the terminal window for that service

# View API Gateway logs
tail -f apigateway/logs/spring.log

# View all Docker container logs
docker-compose logs -f

# View specific container logs
docker logs postgres-user-db -f
```

#### Adjusting Log Levels

Edit `application.yml` in each service:

```yaml
logging:
  level:
    root: INFO
    com.openshop: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

Or set via environment variables:
```bash
export LOGGING_LEVEL_COM_OPENSHOP=DEBUG
mvn spring-boot:run
```

### Common Debugging Scenarios

#### 1. Service Not Starting

```bash
# Check port availability
lsof -i :8081

# Check Java version
java -version

# Check Maven dependencies
cd userservice
mvn dependency:tree

# Clean and rebuild
mvn clean install -U
```

#### 2. Database Connection Issues

```bash
# Check if PostgreSQL is running
docker ps --filter "name=postgres-"

# Test database connectivity
docker exec -it postgres-user-db psql -U openshop_user -d openshop_user_db -c "SELECT 1;"

# Check database logs
docker logs postgres-user-db

# Verify connection string in application.yml
cat userservice/src/main/resources/application-local.yml
```

#### 3. JWT Token Issues

```bash
# Verify token generation
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}' -v

# Decode JWT token (use jwt.io or)
echo "your-token-here" | cut -d '.' -f 2 | base64 -d | jq

# Check token expiration
# Look for "exp" field in decoded token
```

#### 4. Inter-Service Communication

```bash
# Test direct service access (bypass gateway)
curl http://localhost:8081/api/users/me \
  -H "Authorization: Bearer $TOKEN"

# Check if service URLs are correct
env | grep SERVICE_URL

# Test internal service call
curl http://localhost:8083/api/orders/user \
  -H "X-User-Id: 1" \
  -H "X-User-Role: CUSTOMER"
```

---

## 💾 Database Management

### Accessing Databases

#### Using Docker Exec (psql)

```bash
# Connect to User Service database
docker exec -it postgres-user-db psql -U openshop_user -d openshop_user_db

# Connect to Product Service database
docker exec -it postgres-product-db psql -U openshop_user -d openshop_product_db
```

**Common psql Commands**:
```sql
-- List all tables
\dt

-- Describe table structure
\d users

-- Query data
SELECT * FROM users;

-- Count records
SELECT COUNT(*) FROM orders;

-- Exit psql
\q
```

#### Using Database GUI Tools

**DBeaver (Recommended)**:
1. Download from [dbeaver.io](https://dbeaver.io/)
2. Create new PostgreSQL connection:
    - Host: localhost
    - Port: 5432 (or respective service port)
    - Database: openshop_user_db
    - Username: openshop_user
    - Password: openshop_password

**pgAdmin**:
1. Download from [pgadmin.org](https://www.pgadmin.org/)
2. Add server with same credentials

### Database Operations

#### Viewing Schema

```bash
# View User Service schema
docker exec -it postgres-user-db psql -U openshop_user -d openshop_user_db -c "\d+"
```

#### Backup and Restore

**Backup**:
```bash
# Backup User Service database
docker exec postgres-user-db pg_dump -U openshop_user openshop_user_db > user_backup.sql

# Backup all databases
./backup-databases.sh  # (create this script if needed)
```

**Restore**:
```bash
# Restore User Service database
docker exec -i postgres-user-db psql -U openshop_user openshop_user_db < user_backup.sql
```

#### Database Reset

```bash
# Stop all services
./stop-all.sh

# Remove all database volumes
docker-compose down -v

# Restart databases (will recreate schemas)
./start-databases.sh

# Restart services (will run migrations)
./start-local.sh
```

#### Manual Schema Creation

```sql
-- Connect to database
docker exec -it postgres-user-db psql -U openshop_user -d openshop_user_db

-- Create tables manually (example)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Monitoring Database Performance

```bash
# View active connections
docker exec -it postgres-user-db psql -U openshop_user -d openshop_user_db \
  -c "SELECT * FROM pg_stat_activity;"

# View table sizes
docker exec -it postgres-user-db psql -U openshop_user -d openshop_user_db \
  -c "SELECT relname, pg_size_pretty(pg_total_relation_size(relid)) AS size 
      FROM pg_catalog.pg_statio_user_tables 
      ORDER BY pg_total_relation_size(relid) DESC;"

# View slow queries (if enabled)
docker exec -it postgres-user-db psql -U openshop_user -d openshop_user_db \
  -c "SELECT query, calls, total_time, mean_time 
      FROM pg_stat_statements 
      ORDER BY total_time DESC 
      LIMIT 10;"
```

---

## 🧪 Testing

### Unit Tests

```bash
# Run all tests for all services
mvn test

# Run tests for specific service
cd userservice
mvn test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run specific test method
mvn test -Dtest=UserServiceTest#testUserCreation

# Skip tests during build
mvn clean install -DskipTests
```

### Integration Tests

```bash
# Run integration tests (marked with @SpringBootTest)
mvn verify

# Run with specific profile
mvn verify -Dspring.profiles.active=test
```

### API Testing with cURL

Create a test script (`test-api.sh`):

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"

echo "1. Testing User Registration..."
REGISTER_RESPONSE=$(curl -s -X POST $BASE_URL/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "username": "testuser",
    "password": "password123",
    "email": "test@example.com",
    "role": "CUSTOMER"
  }')
echo "✓ Registration successful"

echo "2. Testing Login..."
LOGIN_RESPONSE=$(curl -s -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }')
TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token')
echo "✓ Login successful, token: ${TOKEN:0:20}..."

echo "3. Testing Get Products..."
curl -s $BASE_URL/api/products \
  -H "Authorization: Bearer $TOKEN" | jq '.[] | {id, name, price}'
echo "✓ Products retrieved"

echo "All tests passed!"
```

### API Testing with Bruno

Bruno API collections are included in the project:

```bash
# Install Bruno (if not installed)
brew install bruno  # macOS
# or download from https://www.usebruno.com/

# Open collections
bruno open "OpenShop Buyer APIs"
bruno open "OpenShop Product Service"
bruno open "OpenShop User Service"
```

### Load Testing

Using Apache Bench:

```bash
# Install Apache Bench
brew install httpd  # macOS
sudo apt install apache2-utils  # Linux

# Test login endpoint
ab -n 1000 -c 10 -T "application/json" \
  -p login.json \
  http://localhost:8080/api/auth/login

# login.json content:
# {"username":"testuser","password":"password123"}
```

Using JMeter:

1. Download [Apache JMeter](https://jmeter.apache.org/)
2. Create test plan:
    - Add Thread Group (users)
    - Add HTTP Request (API calls)
    - Add Listeners (view results)
3. Run and analyze results

---

## 🚢 Deployment Options

### Option 1: Docker Compose (Production-like)

#### Prerequisites
- Docker Desktop installed
- Services built (`./build-all.sh`)

#### Deployment Steps

1. **Create Dockerfiles** (if not exists):
```bash
./create-dockerfiles.sh
```

2. **Build Docker Images**:
```bash
# Build all images
docker-compose build

# Build specific service
docker-compose build userservice
```

3. **Start Stack**:
```bash
# Start all services in detached mode
docker-compose up -d

# Start with rebuild
docker-compose up -d --build

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f api-gateway
```

4. **Verify Deployment**:
```bash
# Check running containers
docker-compose ps

# Test API Gateway
curl http://localhost:8080/actuator/health
```

5. **Scale Services**:
```bash
# Scale order service to 3 instances
docker-compose up -d --scale order-service=3

# View scaled services
docker-compose ps
```

6. **Stop Services**:
```bash
# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Option 2: Kubernetes/Minikube

Detailed Kubernetes deployment instructions are available in [k8s/README.md](k8s/README.md).

#### Quick Start

1. **Start Minikube**:
```bash
minikube start --cpus=4 --memory=8192 --driver=docker
```

2. **Build Images**:
```bash
# Point to Minikube Docker daemon
eval $(minikube docker-env)

# Build all images
./build-all.sh
docker-compose build
```

3. **Deploy to Kubernetes**:
```bash
cd k8s
kubectl apply -f openshop-services.yaml
```

4. **Access Services**:
```bash
# Get Minikube IP
minikube service api-gateway --url

# Or use port forwarding
kubectl port-forward svc/api-gateway 8080:8080
```

5. **Monitor Deployment**:
```bash
# Watch pods
kubectl get pods -w

# View logs
kubectl logs -f deployment/user-service

# Describe service
kubectl describe service api-gateway
```

### Option 3: Cloud Deployment (AWS, GCP, Azure)

#### AWS ECS/EKS
1. Push Docker images to ECR
2. Create ECS task definitions or EKS cluster
3. Deploy services with load balancers
4. Configure RDS for PostgreSQL

#### GCP Cloud Run/GKE
1. Push images to Container Registry
2. Deploy to Cloud Run or GKE
3. Use Cloud SQL for PostgreSQL
4. Configure Cloud Load Balancing

#### Azure Container Instances/AKS
1. Push images to Azure Container Registry
2. Deploy to Container Instances or AKS
3. Use Azure Database for PostgreSQL
4. Configure Application Gateway

---

## 🔐 Security

### Authentication Flow

```
1. User → POST /api/auth/login → User Service
2. User Service → Validates credentials
3. User Service → Generates JWT token
4. User ← JWT token + User details
5. User → Subsequent requests with "Authorization: Bearer <token>"
6. API Gateway → Validates JWT
7. API Gateway → Extracts user context (ID, role)
8. API Gateway → Adds X-User-Id and X-User-Role headers
9. Service → Receives request with user context
```

### JWT Token Structure

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "username",
    "userId": 1,
    "role": "CUSTOMER",
    "exp": 1704067200,
    "iat": 1704063600
  },
  "signature": "..."
}
```

### Role-Based Access Control

| Endpoint | Customer | Seller | Admin |
|----------|----------|--------|-------|
| POST /api/products | ❌ | ✅ | ✅ |
| GET /api/products | ✅ | ✅ | ✅ |
| PUT /api/products/{id} | ❌ | ✅ (own) | ✅ |
| POST /api/cart/add | ✅ | ✅ | ✅ |
| POST /api/inventory/create | ❌ | ✅ | ✅ |
| POST /api/inventory/reduce | ❌ | ❌ | ❌ (Internal) |
| DELETE /api/users/{id} | ❌ | ❌ | ✅ |

### Security Best Practices

1. **Change Default JWT Secret** (`.env`):
```properties
JWT_SECRET=your-very-long-and-secure-random-secret-key-change-this
```

2. **Use HTTPS in Production**:
```yaml
# application-prod.yml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEY_STORE_PASSWORD}
```

3. **Enable CORS Properly**:
```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList("https://yourdomain.com"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(Arrays.asList("*"));
        // ... configure as needed
    }
}
```

4. **Implement Rate Limiting**:
```xml
<!-- Add to API Gateway pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway-rate-limiter</artifactId>
</dependency>
```

5. **Use Environment Variables for Secrets**:
```bash
# Never commit .env to git
echo ".env" >> .gitignore
```

---

## 📊 Monitoring & Health Checks

### Health Endpoints

Every service exposes Spring Boot Actuator endpoints:

```bash
# Check health
curl http://localhost:8081/actuator/health

# Get detailed info (requires admin privileges)
curl http://localhost:8081/actuator/info

# View metrics
curl http://localhost:8081/actuator/metrics

# View specific metric
curl http://localhost:8081/actuator/metrics/jvm.memory.used
```

### Monitoring All Services

Create a health check script (`check-health.sh`):

```bash
#!/bin/bash

services=(
    "API Gateway:8080"
    "User Service:8081"
    "Product Service:8082"
    "Order Service:8083"
    "Payment Service:8084"
    "Cart Service:8085"
    "Inventory Service:8086"
    "Notification Service:8087"
    "Shipping Service:8088"
)

for service in "${services[@]}"; do
    name="${service%%:*}"
    port="${service##*:}"
    
    response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/actuator/health)
    
    if [ "$response" = "200" ]; then
        echo "✅ $name (Port $port): UP"
    else
        echo "❌ $name (Port $port): DOWN"
    fi
done
```

### Logging Aggregation

For production, consider using:
- **ELK Stack** (Elasticsearch, Logstash, Kibana)
- **Prometheus + Grafana**
- **Datadog**
- **New Relic**

Example Prometheus configuration:

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'openshop'
    static_configs:
      - targets: 
        - 'localhost:8080'  # API Gateway
        - 'localhost:8081'  # User Service
        # ... other services
    metrics_path: '/actuator/prometheus'
```

---

## 🔧 Troubleshooting

### Common Issues and Solutions

#### Issue 1: Port Already in Use

**Error**: `Port 8080 is already in use`

**Solution**:
```bash
# Find process using port
lsof -i :8080

# Kill process
kill -9 <PID>

# Or use different port
export SERVER_PORT=8090
mvn spring-boot:run
```

#### Issue 2: Database Connection Failed

**Error**: `Connection refused to localhost:5432`

**Solution**:
```bash
# Check if PostgreSQL is running
docker ps --filter "name=postgres-"

# If not running, start databases
./start-databases.sh

# Check database logs
docker logs postgres-user-db

# Test connection
docker exec -it postgres-user-db psql -U openshop_user -d openshop_user_db -c "SELECT 1;"
```

#### Issue 3: OutOfMemoryError

**Error**: `java.lang.OutOfMemoryError: Java heap space`

**Solution**:
```bash
# Increase heap size
export MAVEN_OPTS="-Xmx2048m -Xms512m"
mvn spring-boot:run

# Or in pom.xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <jvmArguments>-Xmx2048m -Xms512m</jvmArguments>
    </configuration>
</plugin>
```

#### Issue 4: 401 Unauthorized

**Error**: API returns 401 even with valid token

**Solution**:
```bash
# Check token expiration
echo "$TOKEN" | cut -d '.' -f 2 | base64 -d | jq

# Get new token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"pass"}'

# Check if Authorization header is correct
# Format: "Authorization: Bearer <token>"
```

#### Issue 5: Service Discovery Fails

**Error**: Services can't communicate

**Solution**:
```bash
# Check service URLs
env | grep SERVICE_URL

# Verify all services are running
./check-health.sh

# Check API Gateway routes
curl http://localhost:8080/actuator/gateway/routes | jq

# Test direct service access
curl http://localhost:8082/api/products
```

#### Issue 6: Maven Build Fails

**Error**: `Failed to execute goal...`

**Solution**:
```bash
# Clean Maven cache
mvn clean

# Update dependencies
mvn clean install -U

# Force download
mvn dependency:purge-local-repository

# Check Maven version
mvn -version  # Should be 3.6+

# Use specific Java version
export JAVA_HOME=/path/to/java-17
mvn clean install
```

#### Issue 7: Docker Build Fails

**Error**: `Cannot connect to Docker daemon`

**Solution**:
```bash
# Start Docker Desktop
open -a Docker  # macOS

# Check Docker status
docker info

# Restart Docker daemon
sudo systemctl restart docker  # Linux

# Clean Docker cache
docker system prune -a
```

#### Issue 8: Kafka Connection Failed

**Error**: `Failed to connect to Kafka broker`

**Solution**:
```bash
# Check if Kafka is running
docker ps --filter "name=kafka"
docker ps --filter "name=zookeeper"

# Check Kafka logs
docker logs kafka
docker logs zookeeper

# Verify Kafka is healthy
docker exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092

# Test Kafka connectivity
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Restart Kafka and Zookeeper
docker-compose restart zookeeper kafka

# If issues persist, recreate containers
docker-compose down
docker-compose up -d zookeeper kafka
```

#### Issue 9: Kafka Consumer Not Receiving Messages

**Error**: Consumer not processing messages from topics

**Solution**:
```bash
# Check if topics exist
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Describe topic to see partitions and consumer groups
docker exec kafka kafka-topics --describe --topic order.payment.request --bootstrap-server localhost:9092

# Check consumer group status
docker exec kafka kafka-consumer-groups --describe --group order-service-group --bootstrap-server localhost:9092

# Reset consumer group offsets (if needed)
docker exec kafka kafka-consumer-groups --group order-service-group --reset-offsets --to-earliest --all-topics --bootstrap-server localhost:9092 --execute

# Manually consume messages to verify
docker exec kafka kafka-console-consumer --topic order.payment.request --from-beginning --bootstrap-server localhost:9092

# Check service Kafka configuration
cat orderservice/src/main/resources/application-local.yml | grep -A 10 kafka
```

#### Issue 10: Messages Stuck in Kafka Topics

**Error**: Messages published but not consumed

**Solution**:
```bash
# Check consumer group lag
docker exec kafka kafka-consumer-groups --describe --group order-service-group --bootstrap-server localhost:9092

# View messages in topic
docker exec kafka kafka-console-consumer --topic order.payment.request --from-beginning --max-messages 10 --bootstrap-server localhost:9092

# Check if consumer service is running and healthy
curl http://localhost:8084/actuator/health

# Check service logs for Kafka errors
docker logs payment-service | grep -i kafka

# Reset topics and restart services
./reset-kafka-topics.sh
docker-compose restart order-service payment-service inventory-service shipping-service
```

### Debug Mode

Enable detailed logging for troubleshooting:

```yaml
# application-local.yml
logging:
  level:
    root: INFO
    com.openshop: DEBUG
    org.springframework.web: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type: TRACE
```

---

## ⚡ Performance Optimization

### Database Optimization

1. **Connection Pooling**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
```

2. **Query Optimization**:
```java
// Use @EntityGraph to avoid N+1 queries
@EntityGraph(attributePaths = {"items"})
@Query("SELECT c FROM Cart c WHERE c.userId = :userId")
Optional<Cart> findByUserId(@Param("userId") Long userId);
```

3. **Indexing**:
```sql
CREATE INDEX idx_user_username ON users(username);
CREATE INDEX idx_product_seller ON products(seller_id);
CREATE INDEX idx_order_user ON orders(user_id);
```

### Caching

Add Redis caching:

```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  cache:
    type: redis
  redis:
    host: localhost
    port: 6379
```

```java
@Cacheable(value = "products", key = "#id")
public Product getProductById(UUID id) {
    return productRepository.findById(id)
        .orElseThrow(() -> new ProductNotFoundException(id));
}
```

### Async Processing

```java
@Async
@Transactional
public CompletableFuture<Order> processOrderAsync(Order order) {
    // Long-running order processing
    return CompletableFuture.completedFuture(processedOrder);
}
```

---

## 📁 Project Structure

```
openshop/
├── .env                          # Environment variables
├── .gitignore                    # Git ignore rules
├── docker-compose.yml            # Docker Compose configuration
├── openshop.yaml                 # OpenAPI specification
├── pom.xml                       # Root Maven configuration
├── README.md                     # This file
│
├── build-all.sh                  # Build all services
├── start-local.sh                # Start services locally
├── start-databases.sh            # Start PostgreSQL databases
├── stop-all.sh                   # Stop all services
├── stop-databases.sh             # Stop all databases
├── create-dockerfiles.sh         # Generate Dockerfiles
│
├── apigateway/                   # API Gateway Service
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/openshop/apigateway/
│   │   │   │       ├── ApiGatewayApplication.java
│   │   │   │       ├── config/
│   │   │   │       │   ├── GatewayConfig.java
│   │   │   │       │   └── SecurityConfig.java
│   │   │   │       ├── filter/
│   │   │   │       │   └── JwtAuthenticationFilter.java
│   │   │   │       └── util/
│   │   │   │           └── JwtUtil.java
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── application-local.yml
│   │   └── test/
│   ├── pom.xml
│   └── Dockerfile
│
├── userservice/                  # User Service
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/openshop/userservice/
│   │   │   │       ├── UserServiceApplication.java
│   │   │   │       ├── controller/
│   │   │   │       │   ├── AuthController.java
│   │   │   │       │   └── UserController.java
│   │   │   │       ├── dto/
│   │   │   │       ├── entity/
│   │   │   │       │   └── User.java
│   │   │   │       ├── repository/
│   │   │   │       │   └── UserRepository.java
│   │   │   │       ├── service/
│   │   │   │       │   └── UserService.java
│   │   │   │       └── config/
│   │   │   └── resources/
│   │   └── test/
│   └── pom.xml
│
├── productservice/               # Product Service (GraphQL)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/openshop/productservice/
│   │   │   │       ├── ProductServiceApplication.java
│   │   │   │       ├── controller/
│   │   │   │       ├── graphql/
│   │   │   │       │   ├── ProductResolver.java
│   │   │   │       │   └── ProductMutationResolver.java
│   │   │   │       └── entity/
│   │   │   └── resources/
│   │   │       └── graphql/
│   │   │           └── schema.graphqls
│   │   └── test/
│   └── pom.xml
│
├── orderservice/                 # Order Service (Saga)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/openshop/orderservice/
│   │   │   │       ├── OrderServiceApplication.java
│   │   │   │       ├── saga/
│   │   │   │       │   └── OrderSagaOrchestrator.java
│   │   │   │       └── service/
│   │   │   └── resources/
│   │   └── test/
│   └── pom.xml
│
├── cartservice/                  # Cart Service
├── inventoryservice/             # Inventory Service
├── paymentservice/               # Payment Service
├── notificationservice/          # Notification Service
├── shippingservice/              # Shipping Service
│
├── k8s/                          # Kubernetes manifests
│   ├── README.md
│   ├── openshop-services.yaml
│   └── user-service.yaml
│
├── OpenShop Buyer APIs/          # Bruno API collections
├── OpenShop Product Service/
└── OpenShop User Service/
```

---

## 🤝 Contributing

We welcome contributions! Please follow these guidelines:

### 1. Fork and Clone

```bash
# Fork the repository on GitHub
# Then clone your fork
git clone https://github.com/yourusername/openshop.git
cd openshop
git remote add upstream https://github.com/original/openshop.git
```

### 2. Create a Branch

```bash
git checkout -b feature/your-feature-name
```

### 3. Make Changes

- Write clean, documented code
- Follow existing code style
- Add tests for new features
- Update documentation if needed

### 4. Test Your Changes

```bash
# Run tests
mvn test

# Build all services
./build-all.sh

# Start and verify
./start-local.sh
```

### 5. Commit and Push

```bash
git add .
git commit -m "feat: add your feature description"
git push origin feature/your-feature-name
```

### 6. Create Pull Request

- Go to GitHub and create a Pull Request
- Describe your changes
- Link related issues

### Code Style

- Follow Java naming conventions
- Use meaningful variable names
- Add JavaDoc comments for public methods
- Keep methods small and focused
- Use Spring Boot best practices

---

## 📄 License

This project is provided as-is for educational and development purposes.

---

## 🙏 Acknowledgments

- **Spring Boot** - Microservices framework
- **Apache Camel** - Integration and Saga orchestration
- **PostgreSQL** - Robust relational database
- **GraphQL** - Flexible API query language
- **Docker** - Containerization
- **Kubernetes** - Container orchestration

---

## 📞 Support

### Documentation

- [Local Development Guide](LOCAL_DEVELOPMENT.md)
- [Kubernetes Deployment](k8s/README.md)
- [Apache Camel Saga](APACHE_CAMEL_SAGA_MIGRATION.md)
- [PostgreSQL Migration](POSTGRESQL_MIGRATION_GUIDE.md)
- [Service Analysis](SERVICE_ANALYSIS_AND_IMPROVEMENTS.md)

### Useful Links

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Apache Camel Documentation](https://camel.apache.org/)
- [GraphQL Documentation](https://graphql.org/)
- [Docker Documentation](https://docs.docker.com/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)

### Community

- Report bugs via GitHub Issues
- Ask questions on Stack Overflow (tag: openshop)
- Join discussions on GitHub Discussions

---

**Made with ❤️ using Spring Boot and modern microservices patterns**

**Version**: 2.0.0  
**Last Updated**: January 2025
