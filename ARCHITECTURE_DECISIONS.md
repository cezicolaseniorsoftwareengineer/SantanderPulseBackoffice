# Architecture Decision Records (ADR)

## ADR-001: Clean Architecture Implementation

**Date**: 2025-10-06  
**Status**: Accepted  
**Decision Makers**: Cezi Cola (Senior Software Engineer)  
**Experience Context**: Based on 8+ years developing enterprise banking systems

### Context
The Santander Pulse Banking Platform requires an architecture that supports:
- High maintainability and testability
- Independence from external frameworks
- Scalability for enterprise banking operations
- Compliance with banking regulatory requirements

### Decision
Implement Clean Architecture as defined by Robert C. Martin, with the following layer structure:

```
├── domain/                 # Enterprise Business Rules
│   ├── entities/          # Core business entities
│   ├── value-objects/     # Immutable domain objects
│   └── repositories/      # Repository interfaces
├── application/           # Application Business Rules
│   ├── use-cases/        # Application-specific business rules
│   ├── dto/              # Data Transfer Objects
│   └── ports/            # Interface adapters
├── infrastructure/        # Frameworks & Drivers
│   ├── persistence/      # Database implementations
│   ├── web/             # REST controllers
│   └── security/        # Security configurations
└── configuration/        # Dependency injection setup
```

### Consequences

**Positive:**
- Framework independence enables technology stack evolution
- High testability through dependency inversion
- Clear separation of concerns
- Suitable for complex banking domain logic

**Negative:**
- Initial development overhead
- Requires deeper architectural understanding
- More files and abstractions

---

## ADR-002: JWT-Based Authentication Strategy

**Date**: 2025-10-06  
**Status**: Accepted  
**Decision Makers**: Cezi Cola (Senior Software Engineer)

### Context
Banking applications require:
- Stateless authentication for microservices
- Secure token management
- Integration with OAuth2 providers
- Compliance with banking security standards

### Decision
Implement JWT (JSON Web Tokens) with:
- HS512 algorithm for token signing
- 24-hour access token expiration
- 7-day refresh token lifecycle
- Secure HttpOnly cookie storage
- Integration with Google OAuth2

### Technical Implementation
```java
@Component
public class JwtService {
    private static final String SECRET_KEY = "base64-encoded-256-bit-key";
    private static final long EXPIRATION_TIME = 86400000; // 24 hours
    
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
            .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
            .compact();
    }
}
```

### Consequences

**Positive:**
- Stateless authentication suitable for microservices
- Industry-standard security approach
- OAuth2 integration capability
- Banking-grade security compliance

**Negative:**
- Token management complexity
- Refresh token rotation required
- Secret key management in production

---

## ADR-003: H2 Database for Development, PostgreSQL for Production

**Date**: 2025-10-06  
**Status**: Accepted  
**Decision Makers**: Cezi Cola (Senior Software Engineer)

### Context
Banking applications need:
- Fast development cycles with in-memory database
- Production-grade ACID compliance
- Transaction isolation for financial operations
- Audit trail capabilities

### Decision
Use H2 in-memory database for development and testing:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:pulsedb;DB_CLOSE_DELAY=-1
    username: pulse_user
    password: pulse_secure_password_2025
```

PostgreSQL for production deployment:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/santander_pulse
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

### Consequences

**Positive:**
- Fast development cycles
- Production-ready ACID compliance
- Banking industry standard (PostgreSQL)
- Comprehensive audit capabilities

**Negative:**
- Database-specific SQL differences
- Production migration complexity
- Additional infrastructure requirements

---

## ADR-004: Angular Standalone Components Architecture

**Date**: 2025-10-06  
**Status**: Accepted  
**Decision Makers**: Cezi Cola (Senior Software Engineer)

### Context
Modern Angular applications benefit from:
- Simplified component architecture
- Better tree-shaking capabilities
- Reduced bundle sizes
- Improved developer experience

### Decision
Implement Angular 18 standalone components with:
- Feature-based module organization
- Lazy loading for route-based code splitting
- Custom interceptors for HTTP security
- RxJS reactive patterns for state management

```typescript
@Component({
  selector: 'app-customer',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PulseUILibModule],
  templateUrl: './customer.component.html'
})
export class CustomerComponent {
  // Component implementation
}
```

### Consequences

**Positive:**
- Smaller bundle sizes
- Better performance
- Simplified dependency management
- Modern Angular best practices

**Negative:**
- Learning curve for teams familiar with NgModules
- Potential migration effort for existing code
- Less established patterns in some areas

---

## ADR-005: Comprehensive Testing Strategy

**Date**: 2025-10-06  
**Status**: Accepted  
**Decision Makers**: Cezi Cola (Senior Software Engineer)

### Context
Banking software requires:
- High reliability and correctness
- Regulatory compliance validation
- Continuous integration support
- Comprehensive test coverage

### Decision
Implement multi-layered testing approach:

**Backend Testing:**
- Unit Tests: JUnit 5 + Mockito (>90% coverage)
- Integration Tests: Spring Boot Test + TestContainers
- Contract Tests: Spring Cloud Contract
- Security Tests: OWASP ZAP integration

**Frontend Testing:**
- Unit Tests: Jasmine + Karma
- Component Tests: Angular Testing Library
- E2E Tests: Cypress with banking scenarios
- Visual Regression Tests: Percy/Chromatic

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerControllerIT {
    
    @Test
    @DisplayName("Should create customer with valid CPF")
    void shouldCreateCustomerWithValidCpf() {
        // Comprehensive integration test
    }
}
```

### Consequences

**Positive:**
- High confidence in banking operations
- Regulatory compliance validation
- Automated quality gates
- Documentation through tests

**Negative:**
- Initial setup complexity
- Longer build times
- Test maintenance overhead

---

**Architecture Decision Records**  
*Documenting technical decisions for enterprise banking platform*

**Maintained by Cezi Cola - Senior Software Engineer**  
*All Rights Reserved © 2025*