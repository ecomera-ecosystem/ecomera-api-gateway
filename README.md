# Ecomera API Gateway

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.11-brightgreen?logo=springboot)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.0.1-6DB33F?logo=spring)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker)
![Gateway](https://img.shields.io/badge/API%20Gateway-Active-6DB33F)
![License](https://img.shields.io/badge/License-MIT-yellow?logo=open-source-initiative&logoColor=white)
Spring Cloud Gateway providing **routing, load balancing, and distributed tracing** for the Ecomera microservices ecosystem.

## Overview

Central entry point for all client requests. Routes traffic to appropriate microservices with built-in load balancing and request tracing.

## Tech Stack

- **Spring Boot**: 3.5.11
- **Spring Cloud Gateway**: Request routing and filtering
- **Eureka Client**: Service discovery
- **Micrometer + Zipkin**: Distributed tracing

## Running Locally

### Prerequisites
- Java 17+
- Maven 3.6+
- Eureka Server running on `localhost:8761`
- Zipkin running on `localhost:9411` (for tracing)

### Start Zipkin (Optional)
```bash
docker run -d -p 9411:9411 openzipkin/zipkin
```

### Start the Gateway
```bash
mvn spring-boot:run
```

Gateway available at: `http://localhost:8080`

## Routes

Routes are managed via Spring Cloud Config Server from [ecomera-config-server](https://github.com/ecomera-ecosystem/ecomera-config-server).

| Service | Path | Target |
|---------|------|--------|
| Auth Service | `/api/v1/auth/**` | `lb://ecomera-auth-service` |
| Product Service | `/api/v1/products/**` | `lb://ecomera-product-service` |
| Cart Service | `/api/v1/cart/**` | `lb://ecomera-cart-service` |

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | 8080 | Gateway port |
| `EUREKA_SERVER_URL` | http://localhost:8761/eureka/ | Eureka server URL |
| `ZIPKIN_URL` | http://localhost:9411/api/v2/spans | Zipkin endpoint |

## Distributed Tracing

All requests are traced with correlation IDs. View traces at: `http://localhost:9411`

## Docker Support
```bash
docker build -t ecomera-api-gateway .
docker run -p 8080:8080 ecomera-api-gateway
```

## Related Services

[//]: # (**Infrastructure:**)
- [Config Server](https://github.com/ecomera-ecosystem/ecomera-config-server)
- [Eureka Service Registry](https://github.com/ecomera-ecosystem/ecomera-eureka-service-registry)

[//]: # ()
[//]: # (**Microservices:**)

[//]: # (- [Auth Service]&#40;https://github.com/ecomera-ecosystem/ecomera-auth-service&#41;)

[//]: # (- [Order Service]&#40;https://github.com/ecomera-ecosystem/ecomera-order-service&#41;)

[//]: # (- [Product Service]&#40;https://github.com/ecomera-ecosystem/ecomera-product-service&#41;)

## License

MIT License - see [LICENSE](LICENSE) file for details