---
description: Backend development standards, best practices, and conventions for the Java/Spring Boot application including Domain-Driven Design, SOLID principles, architecture patterns, API design, and testing practices
globs: [ 
  "backend/src/**/*.java",
  "backend/src/**/*.xml",
  "backend/src/**/*.yml",
  "backend/pom.xml"
]
alwaysApply: true
---

# Backend Project Standards and Best Practices

## Table of Contents

- [Overview](#overview)
- [Technology Stack](#technology-stack)
  - [Core Technologies](#core-technologies)
  - [Database & ORM](#database--orm)
  - [Testing Framework](#testing-framework)
  - [Development Tools](#development-tools)
- [Architecture Overview](#architecture-overview)
  - [Domain-Driven Design (DDD)](#domain-driven-design-ddd)
  - [Layered Architecture](#layered-architecture)
  - [Project Structure](#project-structure)
- [Domain-Driven Design Principles](#domain-driven-design-principles)
  - [Entities](#entities)
  - [Value Objects](#value-objects)
  - [Aggregates](#aggregates)
  - [Repositories](#repositories)
  - [Domain Services](#domain-services)
  - [Additional Recommendations](#additional-recommendations)
- [SOLID and DRY Principles](#solid-and-dry-principles)
  - [Single Responsibility Principle (SRP)](#single-responsibility-principle-srp)
  - [Open/Closed Principle (OCP)](#openclosed-principle-ocp)
  - [Liskov Substitution Principle (LSP)](#liskov-substitution-principle-lsp)
  - [Interface Segregation Principle (ISP)](#interface-segregation-principle-isp)
  - [Dependency Inversion Principle (DIP)](#dependency-inversion-principle-dip)
  - [DRY (Don't Repeat Yourself)](#dry-dont-repeat-yourself)
- [Coding Standards](#coding-standards)
  - [Language and Naming Conventions](#language-and-naming-conventions)
  - [Java Best Practices](#java-best-practices)
  - [Error Handling](#error-handling)
  - [Validation Patterns](#validation-patterns)
  - [Logging Standards](#logging-standards)
- [API Design Standards](#api-design-standards)
  - [REST Endpoints](#rest-endpoints)
  - [Request/Response Patterns](#requestresponse-patterns)
  - [Error Response Format](#error-response-format)
  - [CORS Configuration](#cors-configuration)
- [Database Patterns](#database-patterns)
  - [JPA Entity Configuration](#jpa-entity-configuration)
  - [Migrations](#migrations)
  - [Repository Pattern](#repository-pattern)
- [Testing Standards](#testing-standards)
  - [Unit Testing](#unit-testing)
  - [Integration Testing](#integration-testing)
  - [Test Coverage Requirements](#test-coverage-requirements)
  - [Mocking Standards](#mocking-standards)
- [Performance Best Practices](#performance-best-practices)
  - [Database Query Optimization](#database-query-optimization)
  - [Async Operations](#async-operations)
  - [Error Handling Performance](#error-handling-performance)
- [Security Best Practices](#security-best-practices)
  - [Input Validation](#input-validation)
  - [Environment Variables](#environment-variables)
  - [Dependency Injection](#dependency-injection)
- [Development Workflow](#development-workflow)
  - [Git Workflow](#git-workflow)
  - [Development Scripts](#development-scripts)
  - [Code Quality](#code-quality)
- [Docker Deployment](#docker-deployment)
  - [Dockerfile Configuration](#dockerfile-configuration)
  - [Docker Compose Configuration](#docker-compose-configuration)

---

## Overview

This document outlines the best practices, conventions, and standards used in the backend application. The backend follows Domain-Driven Design (DDD) principles and implements a layered architecture to ensure code consistency, maintainability, and scalability.

## Technology Stack

### Core Technologies
- **Java**: Version 17 or higher (LTS)
- **Spring Boot**: 3.5.x framework
- **Spring Framework**: 6.x for dependency injection and AOP
- **Maven**: Build automation and dependency management

### Database & ORM
- **PostgreSQL**: Relational database (Docker container)
- **Spring Data JPA**: Repository abstraction layer
- **Hibernate**: JPA implementation (included with Spring Data JPA)

### Testing Framework
- **JUnit 5**: Unit and integration testing framework
- **Mockito**: Mocking framework for unit tests
- **Spring Boot Test**: Integration testing support
- **Coverage Threshold**: 90% for branches, functions, lines, and statements
- **Test Location**: `src/test/java` with `*Test.java` and `*IT.java` naming

### Development Tools
- **Maven**: Build tool
- **Spring Boot DevTools**: Hot reload and development utilities
- **Lombok**: Reduce boilerplate code
- **MapStruct**: Type-safe bean mapping
- **Spring Boot Actuator**: Application monitoring and metrics

## Architecture Overview

### Domain-Driven Design (DDD)

Domain-Driven Design is a methodology that focuses on modeling software according to business logic and domain knowledge. By centering development on a deep understanding of the domain, DDD facilitates the creation of complex systems.

**Benefits:**
- **Improved Communication**: Promotes a common language between developers and domain experts, improving communication and reducing interpretation errors.
- **Clear Domain Models**: Helps build models that accurately reflect business rules and processes.
- **High Maintainability**: By dividing the system into subdomains, it facilitates maintenance and software evolution.

### Layered Architecture

The backend follows a layered DDD architecture:

**Presentation Layer** (`src/presentation/`)
- Controllers handle HTTP requests/responses
- Routes define API endpoints
- Controllers use services from Application layer

**Application Layer** (`src/application/`)
- Services contain business logic and orchestration
- Validator handles input validation
- Services use repositories from Domain layer

**Domain Layer** (`src/domain/`)
- Models define core business entities (Candidate, Position, Application, Interview, etc.)
- Repository interfaces define data access contracts
- Pure business logic without external dependencies

**Infrastructure Layer** (`src/infrastructure/`)
- Spring Data JPA handles database operations
- JPA entities map to database tables
- Repository implementations (via Spring Data JPA) satisfy domain interfaces
- External service integrations

### Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── empresa/
│   │   │           └── app/
│   │   │               ├── config/              # Spring configuration classes
│   │   │               │   ├── SecurityConfig.java
│   │   │               │   ├── WebConfig.java
│   │   │               │   └── JpaConfig.java
│   │   │               ├── domain/              # Domain layer (DDD)
│   │   │               │   ├── model/           # Domain entities (pure Java)
│   │   │               │   │   ├── Candidate.java
│   │   │               │   │   └── Position.java
│   │   │               │   ├── repository/      # Repository interfaces
│   │   │               │   │   ├── ICandidateRepository.java
│   │   │               │   │   └── IPositionRepository.java
│   │   │               │   ├── service/         # Domain services
│   │   │               │   │   └── CandidateDomainService.java
│   │   │               │   └── valueobject/     # Value objects
│   │   │               │       └── Email.java
│   │   │               ├── application/         # Application layer
│   │   │               │   ├── service/         # Application services
│   │   │               │   │   ├── CandidateService.java
│   │   │               │   │   └── PositionService.java
│   │   │               │   ├── dto/            # Data Transfer Objects
│   │   │               │   │   ├── request/
│   │   │               │   │   └── response/
│   │   │               │   └── mapper/         # DTO mappers
│   │   │               │       └── CandidateMapper.java
│   │   │               ├── infrastructure/      # Infrastructure layer
│   │   │               │   ├── persistence/    # JPA implementations
│   │   │               │   │   ├── entity/     # JPA entities
│   │   │               │   │   │   ├── CandidateEntity.java
│   │   │               │   │   │   └── PositionEntity.java
│   │   │               │   │   └── repository/ # Spring Data JPA repositories
│   │   │               │   │       ├── CandidateJpaRepository.java
│   │   │               │   │       └── CandidateRepositoryImpl.java
│   │   │               │   └── external/       # External integrations
│   │   │               ├── presentation/        # Presentation layer
│   │   │               │   ├── controller/      # REST controllers
│   │   │               │   │   ├── CandidateController.java
│   │   │               │   │   └── PositionController.java
│   │   │               │   ├── dto/            # API DTOs
│   │   │               │   └── exception/      # Exception handlers
│   │   │               │       └── GlobalExceptionHandler.java
│   │   │               └── AppApplication.java # Main Spring Boot class
│   │   └── resources/
│   │       ├── application.yml                 # Main configuration
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/
│   │           └── V1__initial_schema.sql
│   └── test/
│       ├── java/                                # Test classes
│       │   └── com/
│       │       └── empresa/
│       │           └── app/
│       │               ├── domain/
│       │               ├── application/
│       │               ├── infrastructure/
│       │               └── presentation/
│       └── resources/
│           └── application-test.yml
├── pom.xml                                      # Maven configuration
├── Dockerfile                                   # Docker image definition
├── docker-compose.yml                           # Docker Compose configuration
└── .dockerignore                                # Docker ignore file
```

## Domain-Driven Design Principles

### Entities

Entities are objects with a distinct identity that persists over time.

**Before:**
```java
// Previously, candidate data might have been handled as a simple data class without methods.
public class Candidate {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    // Only getters and setters, no business logic
}
```

**After:**
```java
package com.empresa.app.domain.model;

public class Candidate {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    
    // Constructor and methods that encapsulate business logic
    public Candidate(Long id, String firstName, String lastName, String email) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        validate();
    }
    
    // Business logic methods
    public void updateEmail(String newEmail) {
        if (newEmail == null || !newEmail.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        this.email = newEmail;
    }
    
    private void validate() {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name is required");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Valid email is required");
        }
    }
    
    // Getters
    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
}
```

**Explanation**: `Candidate` is an entity because it has a unique identifier (`id`) that distinguishes it from other candidates, even if other properties are identical.

**Best Practice**: Entities should encapsulate business logic related to their domain concept and maintain consistency of their internal state.

### Value Objects

Value Objects describe aspects of the domain without conceptual identity. They are defined by their attributes rather than an identifier.

**Before:**
```java
// Handling education information as a simple data class
public class Education {
    private String institution;
    private String degree;
    private LocalDate startDate;
    private LocalDate endDate;
    // Only getters and setters
}
```

**After:**
```java
// Using Java Record for immutable value object (Java 14+)
public record Education(
    String institution,
    String title,
    LocalDate startDate,
    LocalDate endDate
) {
    public Education {
        if (institution == null || institution.isBlank()) {
            throw new IllegalArgumentException("Institution is required");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
    }
}
```

**Explanation**: `Education` can be considered a Value Object in some contexts, as it describes a candidate's education without needing a unique identifier. However, in the current model, it has been assigned an id, which could contradict the pure definition of a Value Object in DDD.

**Recommendation**: Classes like `Education` and `WorkExperience` currently have unique identifiers, classifying them as entities. In many cases, these could be treated as Value Objects within the context of a `Candidate` aggregate. Consider removing unique identifiers from classes that should be Value Objects, or incorporating them as part of the Candidate document if using a NoSQL database.

### Aggregates

Aggregates are clusters of objects that must be treated as a unit. They have a root entity that enforces invariants and consistency boundaries.

**Before:**
```java
// Candidate and education data handled separately
Candidate candidate = new Candidate(1L, "John", "Doe", "john@example.com");
List<Education> educations = List.of(
    new Education("University", "Bachelor", LocalDate.of(2010, 1, 1), LocalDate.of(2014, 1, 1))
);
```

**After:**
```java
public class Candidate {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private List<Education> educations;
    
    public Candidate(Long id, String firstName, String lastName, String email, List<Education> educations) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.educations = educations != null ? new ArrayList<>(educations) : new ArrayList<>();
        validate();
    }
    
    // Aggregate root methods
    public void addEducation(Education education) {
        if (education == null) {
            throw new IllegalArgumentException("Education cannot be null");
        }
        this.educations.add(education);
    }
    
    public void removeEducation(Education education) {
        this.educations.remove(education);
    }
    
    // Getters
    public List<Education> getEducations() {
        return Collections.unmodifiableList(educations);
    }
}
```

**Explanation**: `Candidate` acts as an aggregate root that contains `Education`, `WorkExperience`, `Resume`, and `Application`. `Candidate` is the root of the aggregate, as the other entities only make sense in relation to a candidate.

**Recommendation**: Aggregates should be carefully designed to ensure that all operations within the aggregate boundary maintain consistency. Operations that affect `Education` and `WorkExperience` should be handled through the aggregate root, `Candidate`, to maintain integrity and encapsulation.

### Repositories

Repositories provide interfaces for accessing aggregates and entities, encapsulating data access logic.

**Before:**
```java
// Direct database access without abstraction
public Candidate getCandidateById(Long id) {
    return jdbcTemplate.queryForObject(
        "SELECT * FROM candidates WHERE id = ?",
        new Object[]{id},
        candidateRowMapper
    );
}
```

**After:**
```java
// Domain layer interface
public interface ICandidateRepository {
    Optional<Candidate> findById(Long id);
    Candidate save(Candidate candidate);
    List<Candidate> findAll();
    Optional<Candidate> findByEmail(String email);
}

// Infrastructure layer implementation
@Component
public class CandidateRepositoryImpl implements ICandidateRepository {
    private final CandidateJpaRepository jpaRepository;
    private final CandidateMapper mapper;
    
    @Autowired
    public CandidateRepositoryImpl(CandidateJpaRepository jpaRepository, CandidateMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Optional<Candidate> findById(Long id) {
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }
    
    @Override
    public Candidate save(Candidate candidate) {
        CandidateEntity entity = mapper.toEntity(candidate);
        CandidateEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
    
    @Override
    public List<Candidate> findAll() {
        return jpaRepository.findAll().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
}
```

**Explanation**: `ICandidateRepository` provides a clear interface for accessing candidate data, encapsulating database access logic. The implementation uses Spring Data JPA and mappers to convert between domain models and JPA entities.

**Recommendation**: 
- Develop complete repository interfaces for each entity and aggregate, ensuring all database interactions for those entities pass through the repository
- Implement repository methods that handle collections of entities, such as lists of Candidates, that can be filtered or modified in bulk
- Use dependency injection (Spring's `@Autowired` or constructor injection) to inject JPA repositories into repository implementations

### Domain Services

Domain Services contain business logic that doesn't naturally belong to an entity or value object.

**Before:**
```java
// Loose static methods to handle business logic
public class CandidateUtils {
    public static int calculateAge(Candidate candidate) {
        LocalDate today = LocalDate.now();
        LocalDate birthDate = candidate.getBirthDate();
        return Period.between(birthDate, today).getYears();
    }
}
```

**After:**
```java
// Domain service with @Service annotation
@Service
public class CandidateDomainService {
    
    public int calculateAge(Candidate candidate) {
        if (candidate.getBirthDate() == null) {
            throw new IllegalArgumentException("Birth date is required to calculate age");
        }
        LocalDate today = LocalDate.now();
        LocalDate birthDate = candidate.getBirthDate();
        return Period.between(birthDate, today).getYears();
    }
    
    public boolean isEligibleForPosition(Candidate candidate, Position position) {
        // Business logic that doesn't belong to Candidate or Position entities
        return calculateAge(candidate) >= position.getMinimumAge() &&
               candidate.getExperienceYears() >= position.getRequiredExperience();
    }
}
```

**Explanation**: `CandidateService` encapsulates business logic related to candidates, such as calculating age, providing a centralized and coherent point for handling these operations.

### Additional Recommendations

**Use of Factories**

Factories are useful in DDD to encapsulate the logic of creating complex objects, ensuring that all created objects comply with domain rules from the moment of creation.

**Recommendation**: Implement factories for the creation of entities and aggregates, especially those that are complex and require specific initial configuration that complies with business rules.

**Improvement in Relationship Modeling**

Relationships between entities and aggregates must be clear and consistent with business rules.

**Recommendation**: Review and possibly redesign relationships between entities to ensure they accurately reflect domain needs and rules. This may include removing unnecessary relationships or adding new relationships that facilitate business operations.

**Domain Events Integration**

Domain events are an important part of DDD and can be used to handle side effects of domain operations in a decoupled manner.

**Recommendation**: Implement a domain event system that allows entities and aggregates to publish events that other system components can handle without being tightly coupled to the entities that generate them.

## SOLID and DRY Principles

### SOLID Principles

SOLID principles are five object-oriented design principles that help create more understandable, flexible, and maintainable systems.

#### Single Responsibility Principle (SRP)

Each class should have a single responsibility or reason to change.

**Before:**
```java
// A method that handles multiple responsibilities: validation and data storage
public void processCandidate(Candidate candidate) {
    if (!candidate.getEmail().contains("@")) {
        System.err.println("Invalid email");
        return;
    }
    candidateRepository.save(candidate);
    System.out.println("Candidate saved");
}
```

**After:**
```java
public class Candidate {
    // The class now only handles logic related to the candidate
    public void validateEmail() {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
    }
}

@Service
public class CandidateRepository {
    public Candidate save(Candidate candidate) {
        candidate.validateEmail();
        return jpaRepository.save(mapper.toEntity(candidate));
    }
}
```

**Explanation**: The `Candidate` class now has separate methods for validation, while the repository handles data persistence, complying with the single responsibility principle.

**Recommendation**: Separate data access logic into a repository layer to adhere more closely to SRP. Use Spring's `@Service` and `@Repository` annotations to clearly separate concerns.

#### Open/Closed Principle (OCP)

Software entities should be open for extension but closed for modification.

**Before:**
```java
// Direct modification of the class to add functionality
public class Candidate {
    public void saveToDatabase() {
        // code to save to database
    }
    // To add new functionality, we modify the class directly
    public void sendEmail() {
        // code to send an email
    }
}
```

**After:**
```java
public class Candidate {
    public void saveToDatabase() {
        // code to save to database
    }
}

// Extend functionality without modifying the existing class
public class CandidateWithEmail extends Candidate {
    private EmailService emailService;
    
    public CandidateWithEmail(EmailService emailService) {
        this.emailService = emailService;
    }
    
    public void sendEmail() {
        // code to send an email
        emailService.sendEmail(this.getEmail());
    }
}
```

**Explanation**: The email sending functionality is extended in a subclass, keeping the original class closed for modifications but open for extensions.

**Recommendation**: Use factory methods or builder pattern to create instances, allowing for easier extension without modifying existing code. Consider using composition over inheritance when appropriate.

#### Liskov Substitution Principle (LSP)

Objects of a derived class should be replaceable with objects of the base class without altering the program's functionality.

**Before:**
```java
// Subclass that cannot completely replace its base class
public class TemporaryCandidate extends Candidate {
    @Override
    public void saveToDatabase() {
        throw new UnsupportedOperationException("Temporary candidates can't be saved.");
    }
}
```

**After:**
```java
public class TemporaryCandidate extends Candidate {
    private TemporaryStorageService storageService;
    
    public TemporaryCandidate(TemporaryStorageService storageService) {
        this.storageService = storageService;
    }
    
    @Override
    public void saveToDatabase() {
        // Appropriate implementation that allows temporary handling
        storageService.saveTemporarily(this);
    }
}
```

**Explanation**: `TemporaryCandidate` now provides an appropriate implementation that respects the base class contract, allowing substitution without errors.

**Recommendation**: Continue using composition to avoid LSP violations and ensure that any future inheritance structures allow derived classes to substitute their base classes without altering how the program works. Prefer interfaces and composition over inheritance when possible.

#### Interface Segregation Principle (ISP)

Many specific interfaces are better than a single general interface.

**Before:**
```java
// A large interface that small clients don't fully use
public interface CandidateOperations {
    void save();
    void validate();
    void sendEmail();
    void generateReport();
}
```

**After:**
```java
public interface SaveOperation {
    void save();
}

public interface EmailOperations {
    void sendEmail();
}

public interface ReportOperations {
    void generateReport();
}

public class Candidate implements SaveOperation, EmailOperations {
    @Override
    public void save() {
        // implementation
    }
    
    @Override
    public void sendEmail() {
        // implementation
    }
}
```

**Explanation**: Interfaces are segregated into smaller operations, allowing classes to implement only the interfaces they need.

**Recommendation**: Define more granular interfaces for service classes to ensure they only implement the methods they need. Use Java's interface segregation to create focused, cohesive interfaces.

#### Dependency Inversion Principle (DIP)

High-level modules should not depend on low-level modules; both should depend on abstractions.

**Before:**
```java
// Direct dependency on a concrete implementation
public class Candidate {
    private CandidateJpaRepository repository = new CandidateJpaRepository();
    
    public void save() {
        repository.save(this);
    }
}
```

**After:**
```java
public interface ICandidateRepository {
    Candidate save(Candidate candidate);
}

public class Candidate {
    private ICandidateRepository repository;
    
    // Constructor injection (preferred in Spring)
    public Candidate(ICandidateRepository repository) {
        this.repository = repository;
    }
    
    public Candidate save() {
        return repository.save(this);
    }
}

// Spring will inject the implementation
@Service
public class CandidateService {
    private final ICandidateRepository repository;
    
    @Autowired
    public CandidateService(ICandidateRepository repository) {
        this.repository = repository;
    }
}
```

**Explanation**: `Candidate` now depends on an abstraction (`ICandidateRepository`), not a concrete implementation, which facilitates flexibility and code testing.

**Recommendation**: Use Spring's dependency injection to invert the dependency, relying on abstractions rather than concrete implementations. Inject dependencies through constructor (preferred) or use `@Autowired` annotation.

### DRY (Don't Repeat Yourself)

The DRY principle focuses on reducing duplication in code. Each piece of knowledge should have a single, unambiguous, and authoritative representation within a system.

**Before:**
```java
// Repeated code to validate emails in multiple functions
public void saveCandidate(Candidate candidate) {
    if (candidate.getEmail() == null || !candidate.getEmail().contains("@")) {
        throw new IllegalArgumentException("Invalid email");
    }
    // save logic
}

public void updateCandidate(Candidate candidate) {
    if (candidate.getEmail() == null || !candidate.getEmail().contains("@")) {
        throw new IllegalArgumentException("Invalid email");
    }
    // update logic
}
```

**After:**
```java
public class Candidate {
    public void validateEmail() {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
    }
    
    public Candidate save() {
        this.validateEmail();
        // save logic
        return this;
    }
    
    public Candidate update() {
        this.validateEmail();
        // update logic
        return this;
    }
}
```

**Explanation**: Email validation is centralized in a single `validateEmail` method, eliminating code duplication in the save and update functions.

**Recommendation**: Abstract common database operation logic into a reusable base repository class or use Spring Data JPA's built-in methods. Consider using Jakarta Bean Validation (`@Email`, `@NotNull`) for validation instead of manual checks.

## Coding Standards

### Naming Conventions

- **Variable Naming**: Use camelCase for variables and methods (e.g., `candidateId`, `findCandidateById()`)
- **Class Naming**: Use PascalCase for classes and interfaces (e.g., `Candidate`, `CandidateRepository`)
- **Constants Naming**: Use UPPER_SNAKE_CASE for constants (e.g., `MAX_CANDIDATES_PER_PAGE`)
- **Package Naming**: Use lowercase with dots (e.g., `com.empresa.app.domain.model`)
- **File Naming**: Match class name exactly (e.g., `Candidate.java` for `Candidate` class)

**Examples:**

```java
// Good: All in English
public class CandidateRepository {
    public Optional<Candidate> findById(Long candidateId) {
        // Find candidate by ID in the database
        return jpaRepository.findById(candidateId)
            .map(mapper::toDomain);
    }
}

// Avoid: Non-English comments or names
public class RepositorioCandidato {
    public Optional<Candidato> buscarPorId(Long idCandidato) {
        // Buscar candidato por ID en la base de datos
        return jpaRepository.findById(idCandidato)
            .map(mapper::toDomain);
    }
}
```

**Error Messages and Logs:**

```java
// Good: English error messages
throw new CandidateNotFoundException("Candidate not found with the provided ID");
logger.error("Failed to create candidate", exception);

// Avoid: Non-English messages
throw new CandidateNotFoundException("Candidato no encontrado con el ID proporcionado");
logger.error("Error al crear candidato", exception);
```

### Java Best Practices

- **Use Java Records**: For immutable DTOs and value objects (Java 14+)
- **Use Optional**: For methods that may return null
- **Avoid `null`**: Use Optional or empty collections instead
- **Immutable Objects**: Prefer immutable objects where possible
- **Use Builder Pattern**: For complex object construction
- **Lombok**: Use `@Data`, `@Builder`, `@Getter`, `@Setter` to reduce boilerplate

```java
// Good: Using Optional and Records
public record CandidateResponse(Long id, String firstName, String lastName, String email) {}

public Optional<Candidate> findById(Long id) {
    return repository.findById(id);
}

// Avoid: Returning null
public Candidate findById(Long id) {
    return repository.findById(id); // May return null
}
```

### Error Handling

- **Custom Exceptions**: Create domain-specific exception classes
- **@ControllerAdvice**: Use global exception handler for consistent error responses
- **Error Messages**: Provide descriptive error messages for debugging

```java
// Custom exception
public class CandidateNotFoundException extends RuntimeException {
    public CandidateNotFoundException(Long id) {
        super("Candidate not found with id: " + id);
    }
}

// Global exception handler
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(CandidateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCandidateNotFound(
            CandidateNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            "NOT_FOUND",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.toList());
        
        ErrorResponse error = new ErrorResponse(
            "VALIDATION_ERROR",
            "Validation failed",
            details,
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
```

### Validation Patterns

- **Bean Validation**: Use Jakarta Bean Validation (`@Valid`, `@NotNull`, `@Email`, etc.)
- **Custom Validators**: Create custom validators for complex validation rules
- **Validate at Controller**: Validate DTOs at controller level

```java
@RestController
@RequestMapping("/api/candidates")
public class CandidateController {
    
    @PostMapping
    public ResponseEntity<CandidateResponse> createCandidate(
            @Valid @RequestBody CreateCandidateRequest request) {
        Candidate candidate = candidateService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(mapper.toResponse(candidate));
    }
}

// DTO with validation
public record CreateCandidateRequest(
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    String firstName,
    
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    String lastName,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email
) {}
```

### Logging Standards

- **SLF4J**: Use SLF4J as logging facade
- **Logback**: Use Logback as logging implementation (default in Spring Boot)
- **Structured Logging**: Use parameterized logging for better performance
- **Log Levels**: Use appropriate log levels (ERROR, WARN, INFO, DEBUG, TRACE)

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CandidateService {
    private static final Logger logger = LoggerFactory.getLogger(CandidateService.class);
    
    public Candidate create(CreateCandidateRequest request) {
        logger.info("Creating candidate with email: {}", request.email());
        try {
            Candidate candidate = // ... create logic
            logger.info("Candidate created successfully with id: {}", candidate.getId());
            return candidate;
        } catch (Exception e) {
            logger.error("Failed to create candidate with email: {}", request.email(), e);
            throw e;
        }
    }
}
```

## API Design Standards

### REST Endpoints

- **RESTful Naming**: Use RESTful conventions for endpoint naming
- **HTTP Methods**: Use appropriate HTTP methods (GET, POST, PUT, DELETE, PATCH)
- **Resource-Based URLs**: URLs should represent resources, not actions
- **@RestController**: Use `@RestController` annotation for REST controllers
- **@RequestMapping**: Use class-level `@RequestMapping` for base paths

```java
@RestController
@RequestMapping("/api/candidates")
public class CandidateController {
    
    @GetMapping
    public ResponseEntity<List<CandidateResponse>> getAllCandidates() {
        // List candidates
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<CandidateResponse> getCandidateById(@PathVariable Long id) {
        // Get candidate by ID
    }
    
    @PostMapping
    public ResponseEntity<CandidateResponse> createCandidate(
            @Valid @RequestBody CreateCandidateRequest request) {
        // Create new candidate
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<CandidateResponse> updateCandidate(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCandidateRequest request) {
        // Update candidate
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCandidate(@PathVariable Long id) {
        // Delete candidate
    }
}
```

### Request/Response Patterns

- **DTOs**: Use DTOs for request/response instead of entities
- **Consistent Structure**: Maintain consistent response structure across all endpoints
- **Status Codes**: Use appropriate HTTP status codes
- **ResponseEntity**: Use `ResponseEntity` for full control over HTTP response

```java
// Success response DTO
public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, LocalDateTime.now());
    }
}

// Error response DTO
public record ErrorResponse(
    String code,
    String message,
    List<String> details,
    LocalDateTime timestamp
) {}

// Usage in controller
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<CandidateResponse>> getCandidate(@PathVariable Long id) {
    Candidate candidate = candidateService.findById(id)
        .orElseThrow(() -> new CandidateNotFoundException(id));
    
    return ResponseEntity.ok(
        ApiResponse.success(mapper.toResponse(candidate), "Candidate retrieved successfully")
    );
}
```

### Error Response Format

- **Consistent Format**: All errors should follow the same response structure
- **Error Codes**: Use meaningful error codes for different error types
- **HTTP Status Codes**: Map errors to appropriate HTTP status codes

```java
// 400 Bad Request
{
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
        "firstName: First name is required",
        "email: Email must be valid"
    ],
    "timestamp": "2024-01-15T10:30:00"
}

// 404 Not Found
{
    "code": "NOT_FOUND",
    "message": "Resource not found",
    "details": [],
    "timestamp": "2024-01-15T10:30:00"
}

// Implementation in GlobalExceptionHandler
@ExceptionHandler(CandidateNotFoundException.class)
public ResponseEntity<ErrorResponse> handleCandidateNotFound(
        CandidateNotFoundException ex) {
    ErrorResponse error = new ErrorResponse(
        "NOT_FOUND",
        ex.getMessage(),
        Collections.emptyList(),
        LocalDateTime.now()
    );
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
}
```

### CORS Configuration

- **@CrossOrigin**: Use `@CrossOrigin` annotation or global CORS configuration
- **CorsConfiguration**: Configure CORS in `WebMvcConfigurer`
- **Secure Configuration**: Only allow specific origins in production
- **Credentials**: Configure credentials handling appropriately

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}

// Or use @CrossOrigin on specific controllers
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/candidates")
public class CandidateController {
    // ...
}
```

## Database Patterns

### JPA Entity Configuration

- **Single Source of Truth**: JPA entities in `src/main/java/com/empresa/app/infrastructure/persistence/entity/` are the single source of truth for database structure
- **Entity Relationships**: Define relationships using JPA annotations (`@OneToMany`, `@ManyToOne`, `@ManyToMany`, `@OneToOne`)
- **Naming Conventions**: Use consistent naming conventions:
  - **Entity Classes**: PascalCase (e.g., `CandidateEntity`, `PositionEntity`)
  - **Table Names**: snake_case (e.g., `candidates`, `positions`)
  - **Column Names**: snake_case (e.g., `first_name`, `created_at`)
  - **Field Names**: camelCase in Java code (e.g., `firstName`, `createdAt`)

**Best Practices:**
- Use `@Entity` and `@Table` annotations explicitly
- Always specify `@Column` names explicitly to avoid naming strategy conflicts
- Use `@CreationTimestamp` and `@UpdateTimestamp` for audit fields
- Implement `equals()` and `hashCode()` based on business identity, not database ID

```java
@Entity
@Table(name = "candidates")
public class CandidateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;
    
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;
    
    @Column(name = "email", unique = true, nullable = false)
    private String email;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EducationEntity> educations = new ArrayList<>();
    
    // Constructors, getters, setters, equals, hashCode
}
```

### Repository Pattern

- **Domain Repository Interfaces**: Define repository interfaces in the domain layer (`src/main/java/com/empresa/app/domain/repository/`)
- **Spring Data JPA Implementation**: Use Spring Data JPA repositories in the infrastructure layer
- **Custom Repository Methods**: Implement custom queries using `@Query` annotations or query methods
- **Dependency Injection**: Use Spring's `@Autowired` or constructor injection

**Architecture:**
1. **Domain Layer**: Define repository interfaces (contracts)
2. **Infrastructure Layer**: Extend Spring Data JPA repositories and implement domain interfaces

```java
// Domain layer interface (src/main/java/com/empresa/app/domain/repository/)
public interface ICandidateRepository {
    Optional<Candidate> findById(Long id);
    Candidate save(Candidate candidate);
    List<Candidate> findAll();
    Optional<Candidate> findByEmail(String email);
    void deleteById(Long id);
}

// Infrastructure layer - Spring Data JPA repository
// (src/main/java/com/empresa/app/infrastructure/persistence/repository/)
@Repository
public interface CandidateJpaRepository extends JpaRepository<CandidateEntity, Long> {
    Optional<CandidateEntity> findByEmail(String email);
    
    @Query("SELECT c FROM CandidateEntity c WHERE c.firstName LIKE %:name% OR c.lastName LIKE %:name%")
    List<CandidateEntity> findByNameContaining(@Param("name") String name);
    
    @Modifying
    @Query("UPDATE CandidateEntity c SET c.email = :email WHERE c.id = :id")
    void updateEmail(@Param("id") Long id, @Param("email") String email);
}

// Infrastructure layer - Repository implementation
// (src/main/java/com/empresa/app/infrastructure/persistence/repository/)
@Component
public class CandidateRepositoryImpl implements ICandidateRepository {
    private final CandidateJpaRepository jpaRepository;
    private final CandidateMapper mapper;
    
    @Autowired
    public CandidateRepositoryImpl(CandidateJpaRepository jpaRepository, CandidateMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Optional<Candidate> findById(Long id) {
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }
    
    @Override
    public Candidate save(Candidate candidate) {
        CandidateEntity entity = mapper.toEntity(candidate);
        CandidateEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
    
    @Override
    public List<Candidate> findAll() {
        return jpaRepository.findAll().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public Optional<Candidate> findByEmail(String email) {
        return jpaRepository.findByEmail(email)
            .map(mapper::toDomain);
    }
    
    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
```

### Database Configuration

- **Configuration File**: Use `application.yml` in `src/main/resources/`
- **Profile-Based Configuration**: Use Spring profiles for different environments (dev, test, prod)
- **Connection Pooling**: Use HikariCP (default in Spring Boot 3.x)
- **Transaction Management**: Use `@Transactional` annotation for service methods

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/app_db
    username: ${DB_USERNAME:app_user}
    password: ${DB_PASSWORD:app_password}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  jpa:
    hibernate:
      ddl-auto: validate  # Never use 'create' or 'update' in production
    show-sql: false  # Set to true only in development
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        use_sql_comments: true
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
  
```

### Best Practices

**Entity Design:**
- Keep entities focused on persistence concerns
- Use DTOs for API communication
- Use domain models for business logic
- Avoid exposing entities directly in controllers

**Query Optimization:**
- Use `@EntityGraph` to avoid N+1 query problems
- Use `JOIN FETCH` in JPQL queries for eager loading
- Use pagination (`Pageable`) for large result sets
- Use `@BatchSize` for collection fetching

```java
// Avoid N+1 queries with EntityGraph
@EntityGraph(attributePaths = {"educations", "workExperiences"})
Optional<CandidateEntity> findById(Long id);

// Or with JOIN FETCH
@Query("SELECT c FROM CandidateEntity c LEFT JOIN FETCH c.educations WHERE c.id = :id")
Optional<CandidateEntity> findByIdWithEducations(@Param("id") Long id);

// Pagination
Page<CandidateEntity> findAll(Pageable pageable);
```

**Transaction Management:**
- Use `@Transactional` at service layer, not repository layer
- Use `readOnly = true` for read-only operations
- Handle transaction rollback properly for exceptions

```java
@Service
@Transactional
public class CandidateService {
    
    @Transactional(readOnly = true)
    public Optional<Candidate> findById(Long id) {
        // Read-only transaction
    }
    
    public Candidate create(Candidate candidate) {
        // Read-write transaction with automatic rollback on exception
    }
}
```

**Audit Fields:**
- Use `@CreatedDate` and `@LastModifiedDate` with `@EntityListeners(AuditingEntityListener.class)`
- Enable JPA Auditing in configuration class

```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
```

## Testing Standards

The project has strict requirements for code quality and maintainability. These are the unit testing standards and best practices that must be applied. 

### Test File Structure
- Use descriptive test file names: `[ComponentName]Test.java` for unit tests, `[ComponentName]IT.java` for integration tests
- Place test files in `src/test/java` mirroring the source structure
- Use JUnit 5 as the testing framework
- Maintain 90% coverage threshold for branches, functions, lines, and statements

### Test Organization Pattern
Template:
```java
@ExtendWith(MockitoExtension.class)
class CandidateServiceTest {
    
    @Mock
    private ICandidateRepository candidateRepository;
    
    @InjectMocks
    private CandidateService candidateService;
    
    @BeforeEach
    void setUp() {
        // Setup test data
    }
    
    @Nested
    @DisplayName("When finding candidate by ID")
    class FindByIdTests {
        @Test
        @DisplayName("Should return candidate when found")
        void shouldReturnCandidateWhenFound() {
            // Arrange
            Long candidateId = 1L;
            Candidate candidate = new Candidate(candidateId, "John", "Doe", "john@example.com");
            when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
            
            // Act
            Optional<Candidate> result = candidateService.findById(candidateId);
            
            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(candidateId);
            verify(candidateRepository).findById(candidateId);
        }
    }
}
```

Real example:
```java
@ExtendWith(MockitoExtension.class)
class CandidateServiceTest {
    
    @Mock
    private ICandidateRepository repository;
    
    @InjectMocks
    private CandidateService service;
    
    @BeforeEach
    void setUp() {
        Mockito.reset(repository);
    }
    
    @Nested
    @DisplayName("findById")
    class FindByIdTests {
        @Test
        @DisplayName("Should return candidate when found")
        void shouldReturnCandidateWhenFound() {
            // Arrange
            Long candidateId = 1L;
            Candidate candidate = new Candidate(candidateId, "John", "Doe", "john@example.com");
            when(repository.findById(candidateId)).thenReturn(Optional.of(candidate));
            
            // Act
            Optional<Candidate> result = service.findById(candidateId);
            
            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(candidateId);
            verify(repository).findById(candidateId);
        }
        
        @Test
        @DisplayName("Should return empty when candidate not found")
        void shouldReturnEmptyWhenCandidateNotFound() {
            // Arrange
            Long candidateId = 999L;
            when(repository.findById(candidateId)).thenReturn(Optional.empty());
            
            // Act
            Optional<Candidate> result = service.findById(candidateId);
            
            // Assert
            assertThat(result).isEmpty();
        }
    }
}
```



### Test Case Naming Convention
- Use `@DisplayName` for descriptive test names
- Use `@Nested` to group related tests
- Follow Given-When-Then or Arrange-Act-Assert pattern
- Use descriptive method names: `shouldReturnCandidateWhenFound()`, `shouldThrowExceptionWhenInvalid()`

### Test Structure (AAA Pattern)
Always follow the Arrange-Act-Assert pattern:
```java
@Test
@DisplayName("Should update candidate stage successfully when valid data provided")
void shouldUpdateCandidateStageSuccessfullyWhenValidDataProvided() {
    // Arrange - Set up test data and mocks
    Long candidateId = 1L;
    Long applicationId = 1L;
    Integer newInterviewStep = 2;
    Candidate candidate = new Candidate(candidateId, "John", "Doe", "john@example.com");
    when(repository.findById(candidateId)).thenReturn(Optional.of(candidate));
    
    // Act - Execute the function under test
    Candidate result = service.updateCandidateStage(candidateId, applicationId, newInterviewStep);
    
    // Assert - Verify the expected behavior
    assertThat(result).isNotNull();
    assertThat(result.getInterviewStep()).isEqualTo(newInterviewStep);
    verify(repository).findById(candidateId);
}
```

Assertion pattern:
- Use AssertJ assertions: `assertThat()`, `isEqualTo()`, `isPresent()`, `isEmpty()`
- Use Mockito verification: `verify()`, `verifyNoMoreInteractions()`
- Verify both successful operations and error conditions
- Check that mocks were called with correct parameters using `ArgumentMatchers`
- Assert on return values and side effects



### Mocking Standards

- Use `@Mock` for mocking dependencies
- Use `@InjectMocks` for the class under test
- Use `@MockBean` for Spring integration tests
- Use `when().thenReturn()` for stubbing
- Use `verify()` for verifying interactions
- Clear all mocks in `@BeforeEach` to ensure test isolation

```java
@ExtendWith(MockitoExtension.class)
class CandidateServiceTest {
    
    @Mock
    private ICandidateRepository repository;
    
    @InjectMocks
    private CandidateService service;
    
    @Test
    void shouldCreateCandidateSuccessfully() {
        // Arrange
        CreateCandidateRequest request = new CreateCandidateRequest(
            "John", "Doe", "john@example.com"
        );
        Candidate savedCandidate = new Candidate(1L, "John", "Doe", "john@example.com");
        when(repository.save(any(Candidate.class))).thenReturn(savedCandidate);
        
        // Act
        Candidate result = service.create(request);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(repository).save(any(Candidate.class));
    }
}
```


### Test Coverage Requirements

- **Comprehensive test coverage**: Include these test categories for each function:
1. **Happy Path Tests**: Valid inputs producing expected outputs
2. **Error Handling Tests**: Invalid inputs, missing data, database errors
3. **Edge Cases**: Boundary values, null/undefined inputs, empty data
4. **Validation Tests**: Input validation, business rule enforcement
5. **Integration Points**: External service calls, database operations

- **Threshold**: 90% for branches, functions, lines, and statements
- **Coverage Reports**: Generate coverage reports with `mvn test jacoco:report`
- **Coverage Files**: Coverage reports in `target/site/jacoco/` directory
- **JaCoCo Configuration**: Configure JaCoCo Maven plugin in `pom.xml`

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <rules>
            <rule>
                <limits>
                    <limit>
                        <counter>BRANCH</counter>
                        <minimum>0.90</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```


### Error Testing
- Test both expected errors and unexpected errors
- Verify error messages are descriptive and helpful
- Test error propagation through service layers
- Ensure proper HTTP status codes in controller tests

### Controller Testing Specifics
- Use `@SpringBootTest` with `@AutoConfigureMockMvc` for web layer tests
- Mock the service layer completely using `@MockBean`
- Test HTTP request/response handling with `MockMvc`
- Verify parameter parsing and validation
- Test error response formatting

```java
@SpringBootTest
@AutoConfigureMockMvc
class CandidateControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private CandidateService candidateService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void shouldCreateCandidate() throws Exception {
        CreateCandidateRequest request = new CreateCandidateRequest(
            "John", "Doe", "john@example.com"
        );
        CandidateResponse response = new CandidateResponse(1L, "John", "Doe", "john@example.com");
        
        when(candidateService.create(any())).thenReturn(response);
        
        mockMvc.perform(post("/api/candidates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.firstName").value("John"));
    }
}
```

### Service Testing Specifics
- Use `@ExtendWith(MockitoExtension.class)` for unit tests
- Mock domain models and repositories using `@Mock`
- Test business logic in isolation
- Verify data transformation and validation
- Test error handling and edge cases
- Mock external dependencies (repositories, validators)

### Database Testing
- Use `@DataJpaTest` for repository layer tests
- Use `Testcontainers` for integration tests with real database
- Test both successful and failed database operations
- Verify correct database queries and parameters
- Test transaction handling and rollback scenarios

```java
@DataJpaTest
class CandidateRepositoryTest {
    
    @Autowired
    private CandidateJpaRepository repository;
    
    @Test
    void shouldSaveAndFindCandidate() {
        CandidateEntity entity = new CandidateEntity();
        entity.setFirstName("John");
        entity.setLastName("Doe");
        entity.setEmail("john@example.com");
        
        CandidateEntity saved = repository.save(entity);
        Optional<CandidateEntity> found = repository.findById(saved.getId());
        
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("john@example.com");
    }
}
```

### Async Testing
- Use `CompletableFuture` for testing async operations
- Use `@Async` annotation in Spring for async methods
- Test timeout scenarios using `assertTimeout()`
- Use `CountDownLatch` for testing concurrent operations

```java
@Test
void shouldProcessAsyncOperation() throws Exception {
    CompletableFuture<String> future = asyncService.processAsync();
    
    String result = future.get(5, TimeUnit.SECONDS);
    
    assertThat(result).isEqualTo("expected");
}
```

### Test Data Management
- Use factory functions for creating test data
- Keep test data consistent and realistic
- Avoid hardcoded values in multiple places
- Use meaningful test data that reflects real-world scenarios

### Integration Testing

- **Controller Testing**: Test HTTP request/response handling with `@SpringBootTest` and `MockMvc`
- **Database Testing**: Test repository implementations with `@DataJpaTest` or `Testcontainers`
- **End-to-End Flow**: Test complete request flows with `@SpringBootTest`

```java
@SpringBootTest
@AutoConfigureMockMvc
class CandidateControllerIT {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void shouldCreateCandidateEndToEnd() throws Exception {
        CreateCandidateRequest request = new CreateCandidateRequest(
            "John", "Doe", "john@example.com"
        );
        
        mockMvc.perform(post("/api/candidates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.firstName").value("John"));
    }
}
```

### Code Quality Standards

#### Java Best Practices
- Use strong typing for all test parameters and return values
- Define proper classes/records for test data
- Use type-safe assertions with AssertJ
- Leverage Java's type system for better test reliability
- Use builder pattern or test data factories for complex test objects

#### Documentation
- Write clear, descriptive test names that explain the scenario
- Add comments for complex test setups
- Document any special test conditions or edge cases
- Keep test code as readable as production code

#### Performance Considerations
- Keep tests fast and focused
- Avoid unnecessary async operations in tests
- Use appropriate mock strategies to avoid real I/O
- Group related tests to minimize setup/teardown overhead

### Integration with Development Workflow
- Run tests before every commit
- Ensure all tests pass before merging
- Use test-driven development when appropriate
- Update tests when modifying existing functionality

### Common Anti-Patterns to Avoid
- Don't test implementation details, test behavior
- Don't create overly complex test setups
- Don't ignore failing tests or skip error scenarios
- Don't use real database connections in unit tests
- Don't create tests that depend on external services
- Don't write tests that are too tightly coupled to implementation

### Example Test Structure



## Performance Best Practices

### Database Query Optimization

- **Select Specific Fields**: Use DTO projections instead of loading full entities
- **Use Indexes**: Ensure proper database indexes for frequently queried fields
- **Avoid N+1 Queries**: Use `@EntityGraph` or `JOIN FETCH` in JPQL queries
- **Pagination**: Always use pagination for list queries

```java
// Good: Using EntityGraph to avoid N+1 queries
@EntityGraph(attributePaths = {"educations", "workExperiences"})
Optional<CandidateEntity> findById(Long id);

// Or with JOIN FETCH
@Query("SELECT c FROM CandidateEntity c " +
       "LEFT JOIN FETCH c.educations " +
       "LEFT JOIN FETCH c.workExperiences " +
       "WHERE c.id = :id")
Optional<CandidateEntity> findByIdWithRelations(@Param("id") Long id);

// Pagination
Page<CandidateEntity> findAll(Pageable pageable);

// Avoid: N+1 queries
Optional<CandidateEntity> candidate = repository.findById(id);
List<EducationEntity> educations = educationRepository.findByCandidateId(id);
```

### Async Operations

- **@Async**: Use `@Async` for long-running operations
- **CompletableFuture**: Use `CompletableFuture` for parallel operations
- **@EnableAsync**: Enable async support in configuration

```java
@Configuration
@EnableAsync
public class AsyncConfig {
}

@Service
public class CandidateService {
    
    @Async
    public CompletableFuture<List<Candidate>> findAllAsync() {
        return CompletableFuture.completedFuture(findAll());
    }
    
    public CompletableFuture<Void> processInParallel() {
        CompletableFuture<List<Candidate>> candidates = findAllAsync();
        CompletableFuture<List<Position>> positions = findAllPositionsAsync();
        
        return CompletableFuture.allOf(candidates, positions);
    }
}
```

### Caching

- **@Cacheable**: Use Spring caching for frequently accessed data
- **@CacheEvict**: Evict cache when data changes
- **@EnableCaching**: Enable caching support

```java
@Configuration
@EnableCaching
public class CacheConfig {
}

@Service
public class CandidateService {
    
    @Cacheable(value = "candidates", key = "#id")
    public Optional<Candidate> findById(Long id) {
        return repository.findById(id);
    }
    
    @CacheEvict(value = "candidates", key = "#candidate.id")
    public Candidate update(Candidate candidate) {
        return repository.save(candidate);
    }
}
```

### Error Handling Performance

- **Early Returns**: Return early to avoid unnecessary processing
- **Error Propagation**: Let errors propagate naturally through the call stack
- **Avoid Over-Wrapping**: Don't wrap errors unnecessarily

## Security Best Practices

### Input Validation

- **Bean Validation**: Use Jakarta Bean Validation annotations
- **Custom Validators**: Create custom validators for complex rules
- **Sanitize Data**: Sanitize user input to prevent injection attacks
- **Type Safety**: Use Java's type system and validation to ensure type safety

```java
// DTO with validation
public record CreateCandidateRequest(
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    String firstName,
    
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    String lastName,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email
) {}
```

### Environment Variables

- **@Value**: Use `@Value` annotation for configuration properties
- **@ConfigurationProperties**: Use type-safe configuration properties
- **Never Commit Secrets**: Never commit `.env` files or secrets to version control
- **Validate Environment**: Validate required environment variables at startup

```java
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String databaseUrl;
    private Integer maxConnections;
    // getters and setters
}

// Validate required properties
@Component
public class EnvironmentValidator implements ApplicationListener<ApplicationReadyEvent> {
    
    @Value("${spring.datasource.url}")
    private String databaseUrl;
    
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalStateException("DATABASE_URL environment variable is required");
        }
    }
}
```

### Dependency Injection

- **Constructor Injection**: Prefer constructor injection over field injection
- **@Autowired**: Use `@Autowired` on constructor
- **Avoid Global State**: Avoid static dependencies and global state
- **Testability**: Use dependency injection to improve testability

```java
@Service
public class CandidateService {
    private final ICandidateRepository repository;
    private final CandidateMapper mapper;
    
    // Constructor injection (preferred)
    @Autowired
    public CandidateService(ICandidateRepository repository, CandidateMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }
}
```

## Development Workflow

### Git Workflow

- **Feature Branches**: Develop features in separate branches, adding descriptive suffix "-backend" to allow working in parallel and avoid conflicts or collisions
- **Descriptive Commits**: Write descriptive commit messages in Spanish
- **Code Review**: Code review before merging
- **Small Branches**: Keep branches small and focused

### Development Scripts

```bash
# Maven commands
./mvn clean install          # Build and run tests
./mvn spring-boot:run       # Run application
./mvn test                  # Run tests
./mvn test -Dtest=CandidateServiceTest  # Run specific test
./mvn verify                # Run all checks including tests
./mvn clean package         # Build JAR file
./mvn test jacoco:report    # Run tests with coverage report

# Docker commands
docker-compose up -d         # Start services
docker-compose down          # Stop services
docker-compose logs -f backend  # View logs
docker-compose exec backend sh  # Execute commands in container
```

### Code Quality

- **Checkstyle**: Code style checking
- **SpotBugs**: Static code analysis
- **PMD**: Code quality analysis
- **JaCoCo**: Code coverage reporting
- **All Tests Passing**: Ensure all tests pass before deployment
- **Code Review**: Review code for adherence to standards

```xml
<!-- Maven plugins for code quality -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <rules>
            <rule>
                <limits>
                    <limit>
                        <counter>BRANCH</counter>
                        <minimum>0.90</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

## Docker Deployment

### Dockerfile Configuration

- **Multi-stage Build**: Use multi-stage builds to optimize image size
- **Base Image**: Use official OpenJDK images or Eclipse Temurin for Java 17+
- **Build Stage**: Compile and package the application using Maven
- **Runtime Stage**: Use minimal runtime image with only necessary dependencies

**Best Practices:**
- Use `.dockerignore` to exclude unnecessary files
- Leverage Docker layer caching for faster builds
- Set appropriate JVM memory limits
- Use non-root user for security

```dockerfile
# Dockerfile for Spring Boot Application
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", \
  "app.jar"]
```

### Docker Compose Configuration

- **Service Definition**: Define all required services (application, database, etc.)
- **Networks**: Use custom networks for service isolation
- **Volumes**: Persist database data and application logs
- **Environment Variables**: Configure via `.env` file or environment section
- **Health Checks**: Configure health checks for all services
- **Dependencies**: Define service startup order

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: app-postgres
    environment:
      POSTGRES_DB: ${DB_NAME:-app_db}
      POSTGRES_USER: ${DB_USER:-app_user}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-app_password}
    ports:
      - "${DB_PORT:-5432}:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./backend/src/main/resources/db/migration:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER:-app_user} -d ${DB_NAME:-app_db}"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - app-network
    restart: unless-stopped

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: app-backend
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-docker}
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${DB_NAME:-app_db}
      SPRING_DATASOURCE_USERNAME: ${DB_USER:-app_user}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-app_password}
      SPRING_JPA_HIBERNATE_DDL_AUTO: validate
      SERVER_PORT: 8080
    ports:
      - "${BACKEND_PORT:-8080}:8080"
    volumes:
      - ./backend/logs:/app/logs
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    networks:
      - app-network
    restart: unless-stopped

volumes:
  postgres_data:
    driver: local

networks:
  app-network:
    driver: bridge
```

### Environment Configuration

- **Environment File**: Use `.env` file for local development
- **Profile-Based Config**: Use Spring profiles for different environments
- **Secrets Management**: Never commit secrets; use environment variables or secrets management

```bash
# .env (example - never commit this file)
DB_NAME=app_db
DB_USER=app_user
DB_PASSWORD=app_password
DB_PORT=5432
BACKEND_PORT=8080
SPRING_PROFILES_ACTIVE=docker
```

### Docker Compose Commands

**Development:**
```bash
# Build and start all services
docker-compose up -d

# Build and start with rebuild
docker-compose up -d --build

# View logs
docker-compose logs -f backend
docker-compose logs -f postgres

# Stop all services
docker-compose down

# Stop and remove volumes (⚠️ deletes database data)
docker-compose down -v

# Execute commands in running container
docker-compose exec backend sh
docker-compose exec postgres psql -U app_user -d app_db
```

**Production:**
```bash
# Use production compose file
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Scale services (if needed)
docker-compose up -d --scale backend=3
```

### Production Docker Compose Override

```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  backend:
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_JPA_HIBERNATE_DDL_AUTO: validate
      SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE: 20
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
    restart: always
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

### Best Practices

**Security:**
- Never commit `.env` files with real credentials
- Use Docker secrets or external secret management in production
- Run containers as non-root user
- Use minimal base images (Alpine Linux)
- Scan images for vulnerabilities

**Performance:**
- Use multi-stage builds to reduce image size
- Leverage Docker layer caching
- Set appropriate JVM memory limits
- Use health checks for proper service orchestration

**Development:**
- Mount source code as volumes for hot reload (development only)
- Use named volumes for database persistence
- Configure proper network isolation
- Use docker-compose override files for environment-specific configs

**Monitoring:**
- Enable Spring Boot Actuator for health checks
- Configure logging to stdout/stderr
- Use Docker logging drivers for log aggregation
- Monitor container resource usage

```yaml
# Development override (docker-compose.dev.yml)
version: '3.8'

services:
  backend:
    build:
      target: build
    volumes:
      - ./backend/src:/app/src
      - ./backend/target:/app/target
    environment:
      SPRING_DEVTOOLS_RESTART_ENABLED: "true"
      SPRING_PROFILES_ACTIVE: dev
```

### .dockerignore File

```dockerignore
# .dockerignore
target/
!.mvn/wrapper/maven-wrapper.jar
!**/src/main/**/target/
!**/src/test/**/target/

.git
.gitignore
.mvn
mvnw
mvnw.cmd
*.md
.env
.idea
.vscode
*.iml
logs/
*.log
```

This document serves as the foundation for maintaining code quality and consistency across the backend application. All team members should follow these practices to ensure a maintainable, scalable, and testable codebase.
