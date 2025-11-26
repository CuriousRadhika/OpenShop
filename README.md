# OpenShop - Enterprise Microservices E-commerce Platform

![OpenShop Banner](assets/Readme%20file%20banner.jpg)

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Apache Camel](https://img.shields.io/badge/Apache%20Camel-Saga-red.svg)](https://camel.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue.svg)](https://www.postgresql.org/)

OpenShop is a robust, API-driven microservices-based e-commerce platform designed to facilitate seamless interaction between buyers and sellers. Built as a monolithic application for streamlined deployment, it provides a unified interface for catalog management, shopping cart operations, and order processing. The project demonstrates a hybrid API implementation, utilizing REST for transactional resources (Users, Orders, Cart) and GraphQL for flexible, high-volume data fetching (Product Catalog)

## 📑 Table of Contents

- [Architecture Overview](#-architecture-overview)
- [Key Features](#-key-features)

---

## 🏗️ Architecture Overview

OpenShop implements a sophisticated microservices architecture with the following components:

### Microservices Stack

```
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway (8080)                      │
│              Spring Cloud Gateway + JWT Auth                 │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│ User Service │     │   Product    │     │     Cart     │
│   (8081)     │     │   Service    │     │   Service    │
│              │     │   (8082)     │     │   (8085)     │
│ PostgreSQL   │     │ PostgreSQL   │     │ PostgreSQL   │
│ (5432)       │     │ (5433)       │     │ (5435)       │
└──────────────┘     └──────────────┘     └──────────────┘
                              │
                              │ GraphQL
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│    Order     │     │  Inventory   │     │   Payment    │
│   Service    │     │   Service    │     │   Service    │
│   (8083)     │     │   (8086)     │     │   (8084)     │
│ PostgreSQL   │     │ PostgreSQL   │     │ PostgreSQL   │
│ (5434)       │     │ (5436)       │     │ (5437)       │
└──────────────┘     └──────────────┘     └──────────────┘
        │
        │ Apache Camel Saga
        │
        ┌─────────────────────┐
        ▼                     ▼
┌──────────────┐     ┌──────────────┐
│ Notification │     │   Shipping   │
│   Service    │     │   Service    │
│   (8087)     │     │   (8088)     │
│ PostgreSQL   │     │ PostgreSQL   │
│ (5438)       │     │ (5439)       │
└──────────────┘     └──────────────┘
```

### Core Components

| Component | Port | Database Port | Technology | Purpose |
|-----------|------|---------------|------------|---------|
| **API Gateway** | 8080 | - | Spring Cloud Gateway | Request routing, JWT validation, load balancing |
| **User Service** | 8081 | 5432 | Spring Boot + PostgreSQL | User authentication, authorization, profile management |
| **Product Service** | 8082 | 5433 | Spring Boot + GraphQL + PostgreSQL | Product catalog with GraphQL API |
| **Order Service** | 8083 | 5434 | Spring Boot + Kafka + PostgreSQL | Order orchestration with Saga pattern via Kafka |
| **Payment Service** | 8084 | 5437 | Spring Boot + Kafka + PostgreSQL | Payment processing with idempotency |
| **Cart Service** | 8085 | 5435 | Spring Boot + PostgreSQL | Shopping cart management |
| **Inventory Service** | 8086 | 5436 | Spring Boot + Kafka + PostgreSQL | Stock management and reservation |
| **Notification Service** | 8087 | 5438 | Spring Boot + PostgreSQL | Multi-channel notifications |
| **Shipping Service** | 8088 | 5439 | Spring Boot + Kafka + PostgreSQL | Shipment tracking |
| **Kafka** | 9092 | - | Apache Kafka | Message broker for event-driven Saga pattern |
| **Zookeeper** | 2181 | - | Apache Zookeeper | Kafka cluster coordination |

---

## ✨ Key Features

### 🎯 Business Features
- **Multi-role Support**: Customer and seller  roles with granular permissions
- **Product Catalog**: Full CRUD operations with GraphQL query support
- **Shopping Cart**: Add, remove, update quantities, checkout
- **Order Processing**: End-to-end order workflow 
- **Inventory Management**: Real-time stock tracking and reservation
- **Payment Integration**: Mock payment gateway with success/failure simulation

### 🔧 Technical Features

- **GraphQL API**: Flexible product queries with nested field selection
- **JWT Authentication**: Stateless, secure authentication across all services
- **Service-to-Service Auth**: Header-based user context propagation
- **Database Persistence**: PostgreSQL for all services (production-ready)
- **API Gateway**: Centralized routing, authentication, and request filtering
- **Health Monitoring**: Spring Boot Actuator endpoints for all services
- **Fault Tolerance**: Compensation logic for failed transactions

### 📊 Design Patterns
- **Saga Pattern**: Distributed transaction coordination (Choreography-based)
- **API Gateway Pattern**: Single entry point for all client requests
- **Database per Service**: Independent data stores for each microservice
- **Event-Driven Architecture**: Asynchronous communication where appropriate

---


**Made with ❤️ using Spring Boot and modern microservices patterns**

