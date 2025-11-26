# Contributing to OpenShop

Thank you for your interest in contributing to OpenShop! This guide will help you set up your development environment and understand the project structure.

## 📝 Contributor License Agreement (CLA)

Before you can contribute to OpenShop, you will need to sign the [Contributor License Agreement](CLA.md).

## 📑 Table of Contents

- [Prerequisites](#-prerequisites)
- [Quick Start Guide](#-quick-start-guide)
- [Project Architecture](#-project-architecture)
- [Service Documentation](#-service-documentation)
- [API Reference](#-api-reference)
- [Development Workflow](#-development-workflow)
- [Database Management](#-database-management)
- [Testing](#-testing)
- [Troubleshooting](#-troubleshooting)
- [Code Style Guidelines](#-code-style-guidelines)
- [Pull Request Process](#-pull-request-process)

---

## 🔧 Prerequisites

### Required Software

| Tool | Version | Purpose |
|------|---------|---------|
| Java JDK | 17+ | Backend runtime |
| Maven | 3.8+ | Build tool |
| Node.js | 18+ | Frontend runtime |
| npm | 9+ | Package manager |
| Docker Desktop | Latest | Infrastructure services |
| Git | Latest | Version control |

### Verification Commands

```bash
java -version        # Should show 17.x.x or higher
mvn -version         # Should show 3.8.x or higher
node --version       # Should show v18.x.x or higher
npm --version        # Should show 9.x.x or higher
docker --version     # Should show 20.x.x or higher
docker-compose --version  # Should show 2.x.x or higher
```

### System Requirements

- **RAM**: Minimum 8GB (16GB recommended)
- **CPU**: Minimum 4 cores
- **Disk Space**: 10GB free space
- **OS**: macOS, Linux, or Windows with WSL2

---

## 🚀 Quick Start Guide

### 1. Clone and Setup

```bash
# Clone repository
git clone https://github.com/yourusername/OpenShop.git
cd OpenShop

# Start infrastructure (PostgreSQL, Kafka, Zookeeper)
cd services
./start-databases.sh

# Wait for databases to be healthy (check with)
docker ps --filter "name=postgres-"
docker ps --filter "name=kafka"
```

### 2. Build and Start Backend

```bash
# Build all services
./build-all.sh

# Start all microservices
./start-local.sh
```

### 3. Start Frontend

```bash
# In a new terminal
cd ui
npm install
npm run dev
```

### 4. Access Application

- **Frontend**: http://localhost:5173
- **API Gateway**: http://localhost:8080
- **Individual Services**: Ports 8081-8088

---

## 🏗️ Project Architecture

### Backend Services

```
services/
├── apigateway/              # Port 8080 - Entry point for all requests
├── userservice/             # Port 8081 - Authentication & user management
├── productservice/          # Port 8082 - Product catalog with GraphQL
├── orderservice/            # Port 8083 - Order processing with Saga
├── paymentservice/          # Port 8084 - Payment processing
├── cartservice/             # Port 8085 - Shopping cart
├── inventoryservice/        # Port 8086 - Stock management
├── shippingservice/         # Port 8088 - Shipping tracking
└── common-events/           # Shared Kafka event definitions
```

### Frontend Structure

```
ui/
├── src/
│   ├── api/                 # API client configuration
│   ├── components/          # Reusable React components
│   │   ├── cart/            # Cart-related components
│   │   ├── checkout/        # Checkout flow components
│   │   ├── layout/          # Layout components
│   │   ├── product/         # Product components
│   │   ├── shared/          # Shared UI components
│   │   └── ui/              # Base UI components
│   ├── pages/               # Page-level components
│   ├── stores/              # State management (Redux + Zustand)
│   └── lib/                 # Utility functions
└── public/                  # Static assets
```

### Infrastructure Services

| Service | Port | Purpose | Connection |
|---------|------|---------|------------|
| postgres-user | 5432 | User data | userservice |
| postgres-product | 5433 | Product data | productservice |
| postgres-order | 5434 | Order data | orderservice |
| postgres-payment | 5437 | Payment data | paymentservice |
| postgres-cart | 5435 | Cart data | cartservice |
| postgres-inventory | 5436 | Inventory data | inventoryservice |
| postgres-shipping | 5439 | Shipping data | shippingservice |
| kafka | 9092 | Event streaming | order, payment, inventory, shipping |
| zookeeper | 2181 | Kafka coordination | kafka |

---

## 📚 Service Documentation

### API Gateway (Port 8080)

**Technology**: Spring Cloud Gateway  
**Database**: None  
**Purpose**: Routes requests, validates JWT, adds user context headers

**Key Routes**:
```yaml
/api/auth/**        → User Service (no auth)
/api/users/**       → User Service (auth required)
/api/products/**    → Product Service
/api/cart/**        → Cart Service (auth required)
/api/orders/**      → Order Service (auth required)
/api/inventory/**   → Inventory Service
/api/payments/**    → Payment Service
/api/shipping/**    → Shipping Service
```

**Configuration**: `services/apigateway/src/main/resources/application.yml`

### User Service (Port 8081)

**Technology**: Spring Boot + Spring Security + JWT  
**Database**: PostgreSQL (userdb on port 5432)  
**Purpose**: User registration, authentication, profile management

**Key Endpoints**:
```
POST   /api/auth/register    - Register new user
POST   /api/auth/login       - Login and get JWT token
GET    /api/users/me         - Get current user profile
PUT    /api/users/me         - Update profile
```

**User Roles**: CUSTOMER, SELLER, ADMIN

### Product Service (Port 8082)

**Technology**: Spring Boot + Spring GraphQL  
**Database**: PostgreSQL (productdb on port 5433)  
**Purpose**: Product catalog with both REST and GraphQL APIs

**REST Endpoints**:
```
POST   /api/products              - Create product (Seller only)
GET    /api/products              - Get all products
GET    /api/products/{id}         - Get product by ID
PUT    /api/products/update/{id}  - Update product (Owner only)
```

**GraphQL Endpoint**:
```
POST   /api/products/graphql      - GraphQL queries and mutations
```

**GraphQL Schema Example**:
```graphql
type Query {
  products: [Product]
  product(id: ID!): Product
}

type Mutation {
  createProduct(input: ProductInput!): Product
  updateProduct(id: ID!, input: ProductInput!): Product
}
```

### Order Service (Port 8083)

**Technology**: Spring Boot + Kafka (Saga Pattern)  
**Database**: PostgreSQL (orderdb on port 5434)  
**Purpose**: Order management with distributed transaction coordination

**Key Endpoints**:
```
POST   /api/orders/placeCart        - Create order from cart
GET    /api/orders                  - Get all orders
GET    /api/orders/user             - Get user's orders
GET    /api/orders/{id}             - Get order by ID
```

**Saga Flow**:
1. Create order → Reserve inventory → Process payment → Create shipment
2. On failure: Compensate (release inventory, refund payment)

**Kafka Topics Used**:
- `order.inventory.reserve.request`
- `inventory.order.reserve.response`
- `order.payment.request`
- `payment.order.response`
- `order.shipping.request`

### Payment Service (Port 8084)

**Technology**: Spring Boot + Kafka  
**Database**: PostgreSQL (paymentdb on port 5437)  
**Purpose**: Payment processing with idempotency

**Key Endpoints**:
```
POST   /api/payments/initiate       - Initiate payment
POST   /api/payments/process/{id}   - Process payment
GET    /api/payments/{orderId}      - Get payment status
POST   /api/payments/cancel/{id}    - Cancel/refund payment
```

**Payment States**: PENDING, SUCCESS, FAILED, CANCELLED

### Cart Service (Port 8085)

**Technology**: Spring Boot  
**Database**: PostgreSQL (cartdb on port 5435)  
**Purpose**: Shopping cart management

**Key Endpoints**:
```
GET    /api/cart                    - Get user's cart
POST   /api/cart/add                - Add item to cart
PUT    /api/cart/update/{itemId}    - Update item quantity
DELETE /api/cart/remove/{itemId}    - Remove item
DELETE /api/cart/clear              - Clear cart
POST   /api/cart/checkout           - Checkout and create orders
```

### Inventory Service (Port 8086)

**Technology**: Spring Boot + Kafka  
**Database**: PostgreSQL (inventorydb on port 5436)  
**Purpose**: Stock management and reservation

**Key Endpoints**:
```
POST   /api/inventory/create         - Create inventory (Seller)
GET    /api/inventory/{productId}    - Get inventory
POST   /api/inventory/increase       - Add stock (Seller)
POST   /api/inventory/reduce         - Reduce stock (Internal)
POST   /api/inventory/reserve        - Reserve stock (Internal)
POST   /api/inventory/release        - Release reservation (Internal)
```

### Shipping Service (Port 8088)

**Technology**: Spring Boot + Kafka  
**Database**: PostgreSQL (shippingdb on port 5439)  
**Purpose**: Shipment tracking

**Key Endpoints**:
```
POST   /api/shipping                 - Create shipment
GET    /api/shipping/{orderId}       - Get shipment by order
PUT    /api/shipping/{id}/status     - Update status
```

**Shipment States**: CREATED, IN_TRANSIT, DELIVERED

---

## 🔌 API Reference

### Authentication Flow

#### 1. Register User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "username": "johndoe",
    "password": "password123",
    "email": "john@example.com",
    "role": "CUSTOMER"
  }'
```

**Response**:
```json
{
  "id": 1,
  "name": "John Doe",
  "username": "johndoe",
  "email": "john@example.com",
  "role": "CUSTOMER",
  "createdAt": "2024-11-26T14:30:00"
}
```

#### 2. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "password123"
  }'
```

**Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 86400000,
  "user": {
    "id": 1,
    "username": "johndoe",
    "role": "CUSTOMER"
  }
}
```

### Complete E-commerce Flow

```bash
# Save token
TOKEN="your-jwt-token-here"

# 1. Browse products
curl http://localhost:8080/api/products \
  -H "Authorization: Bearer $TOKEN"

# 2. Add to cart
curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "productId": "uuid-here",
    "quantity": 2,
    "price": 99.99
  }'

# 3. View cart
curl http://localhost:8080/api/cart \
  -H "Authorization: Bearer $TOKEN"

# 4. Checkout
curl -X POST http://localhost:8080/api/cart/checkout \
  -H "Authorization: Bearer $TOKEN"

# 5. Check order status
curl http://localhost:8080/api/orders/user \
  -H "Authorization: Bearer $TOKEN"
```

### GraphQL Examples

```bash
# Query all products
curl -X POST http://localhost:8080/api/products/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ products { id name price category status } }"
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

## 💻 Development Workflow

### Running Individual Services

```bash
# Start only infrastructure
cd services
./start-databases.sh

# Run specific service
cd userservice
mvn spring-boot:run

# Or with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Hot Reload Development

Spring Boot DevTools is included and enables automatic restart on code changes.

**IntelliJ IDEA**:
1. Enable "Build project automatically" in Preferences → Compiler
2. Enable "Allow auto-make" in Advanced Settings

**VS Code**: Changes are detected automatically

### Building Services

```bash
# Build all services
cd services
./build-all.sh

# Build specific service
cd userservice
mvn clean install

# Build without tests (faster)
mvn clean install -DskipTests

# Build with specific profile
mvn clean install -P production
```

### Frontend Development

```bash
cd ui

# Install dependencies
npm install

# Start dev server (hot reload enabled)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Run linter
npm run lint
```

---

## 💾 Database Management

### Accessing Databases

```bash
# Connect to User database
docker exec -it postgres-user-db psql -U openshop -d userdb

# Connect to Product database
docker exec -it postgres-product-db psql -U openshop -d productdb

# Common psql commands:
\dt              # List tables
\d table_name    # Describe table
\q               # Quit
```

### Database Credentials

All PostgreSQL databases use:
- **Username**: `openshop`
- **Password**: `openshop123`
- **Host**: `localhost`
- **Ports**: See architecture section

### Managing Database Containers

```bash
# Start all databases
./start-databases.sh

# Stop all databases
./stop-databases.sh

# View database logs
docker logs postgres-user-db -f

# Remove all data (clean slate)
docker-compose down -v

# Restart specific database
docker-compose restart postgres-user
```

### Schema Management

Schemas are automatically created by Spring Boot JPA on first run with:
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

For production, use migrations with Flyway or Liquibase.

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

# Run with coverage
mvn test jacoco:report
```

### Integration Tests

```bash
# Run integration tests
mvn verify

# With test profile
mvn verify -Dspring.profiles.active=test
```

### Manual API Testing

Create test script (`test-api.sh`):

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"

echo "Testing API..."

# Register
REGISTER=$(curl -s -X POST $BASE_URL/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","username":"test","password":"test","email":"test@test.com","role":"CUSTOMER"}')

# Login
TOKEN=$(curl -s -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}' | jq -r '.token')

echo "Token: $TOKEN"

# Get products
curl -s $BASE_URL/api/products \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## 🔧 Troubleshooting

### Common Issues

#### 1. Port Already in Use

```bash
# Find process using port
lsof -i :8080

# Kill process
kill -9 <PID>
```

#### 2. Database Connection Failed

```bash
# Check if databases are running
docker ps --filter "name=postgres-"

# Restart databases
./stop-databases.sh
./start-databases.sh

# Check logs
docker logs postgres-user-db
```

#### 3. Kafka Connection Issues

```bash
# Check Kafka status
docker ps --filter "name=kafka"
docker ps --filter "name=zookeeper"

# Verify Kafka
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Restart Kafka
docker-compose restart zookeeper kafka
```

#### 4. Service Won't Start

```bash
# Clean and rebuild
cd userservice
mvn clean install -U

# Check Java version
java -version  # Must be 17+

# Check logs
tail -f userservice/logs/spring.log
```

#### 5. Frontend Build Errors

```bash
# Clear node_modules
cd ui
rm -rf node_modules package-lock.json
npm install

# Clear cache
npm cache clean --force
```

### Debug Mode

Enable debug logging in `application.yml`:

```yaml
logging:
  level:
    root: INFO
    com.openshop: DEBUG
    org.springframework.web: DEBUG
```

---

## 📝 Code Style Guidelines

### Java/Spring Boot

- Follow Spring Boot best practices
- Use meaningful variable and method names
- Add JavaDoc for public methods
- Keep methods small and focused
- Use constructor injection over field injection
- Handle exceptions properly
- Write unit tests for business logic

**Example**:
```java
@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;
    
    /**
     * Creates a new product
     * @param product Product to create
     * @return Created product
     * @throws ProductAlreadyExistsException if SKU already exists
     */
    @Transactional
    public Product createProduct(Product product) {
        if (productRepository.existsBySku(product.getSku())) {
            throw new ProductAlreadyExistsException(product.getSku());
        }
        return productRepository.save(product);
    }
}
```

### React/TypeScript

- Use functional components with hooks
- Properly type all props and state
- Extract reusable logic into custom hooks
- Keep components small and focused
- Use meaningful component and file names
- Follow React best practices

**Example**:
```typescript
interface ProductCardProps {
  product: Product;
  onAddToCart: (product: Product) => void;
}

export const ProductCard: React.FC<ProductCardProps> = ({ 
  product, 
  onAddToCart 
}) => {
  return (
    <Card>
      <CardContent>
        <Typography variant="h5">{product.name}</Typography>
        <Typography variant="body2">{product.description}</Typography>
        <Button onClick={() => onAddToCart(product)}>
          Add to Cart
        </Button>
      </CardContent>
    </Card>
  );
};
```

---

## 🔄 Pull Request Process

### 1. Fork and Clone

```bash
# Fork on GitHub, then:
git clone https://github.com/yourusername/OpenShop.git
cd OpenShop
git remote add upstream https://github.com/original/OpenShop.git
```

### 2. Create Branch

```bash
git checkout -b feature/your-feature-name
```

### 3. Make Changes

- Write clean, documented code
- Add tests for new features
- Update documentation if needed
- Follow code style guidelines

### 4. Test Locally

```bash
# Run tests
mvn test

# Build all services
./build-all.sh

# Start and verify
./start-local.sh
```

### 5. Commit

```bash
git add .
git commit -m "feat: add your feature description"
```

**Commit Message Format**:
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `refactor:` Code refactoring
- `test:` Adding tests
- `chore:` Build/config changes

### 6. Push and Create PR

```bash
git push origin feature/your-feature-name
```

Then create Pull Request on GitHub with:
- Clear description of changes
- Reference to related issues
- Screenshots if UI changes

---

## 🙏 Questions and Support

- **Documentation Issues**: Open an issue on GitHub
- **Feature Requests**: Use GitHub Discussions
- **Bug Reports**: Use GitHub Issues with detailed reproduction steps

---

**Thank you for contributing to OpenShop!** 🎉

**Last Updated**: November 2025
