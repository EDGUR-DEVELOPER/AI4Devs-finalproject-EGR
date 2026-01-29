# Data Model Documentation — DocFlow

This document describes the data model for **DocFlow**, an Intelligent Document Management System (DMS). It includes entity descriptions, field definitions, relationships, validation rules, and an entity-relationship diagram.

## Overview

DocFlow implements a **multi-organization, role-based access control (RBAC)** architecture for managing document lifecycle, versioning, and permissions. The system is built on:

- **Technology**: Java 21, Spring Boot 3.x, PostgreSQL 16, MongoDB
- **Architecture**: Hexagonal Architecture (Ports & Adapters) with Domain-Driven Design (DDD)
- **Storage**: PostgreSQL for relational data, MinIO (S3-compatible) for binary documents
- **Core Concepts**: Multi-tenancy, document versioning, soft deletes, audit logging

**Total Entities**: 10 (Organization, User, UserOrganization, Role, Permission, Folder, Document, NivelAcceso, DocumentVersion, AuditLog)

---

## Model Descriptions

### 1. Organization
Represents a tenant/organization within the DocFlow system. Supports multi-organization context.

**Fields:**
- `id`: Long - Unique organization identifier (Primary Key)
- `name`: String (max 255 characters) - Organization name
- `description`: String (max 500 characters, nullable) - Organization description
- `status`: Enum [ACTIVE, INACTIVE, SUSPENDED] - Organization status
- `createdAt`: DateTime - Organization creation timestamp
- `updatedAt`: DateTime - Last organization update timestamp
- `deletedAt`: DateTime (nullable) - Soft delete timestamp

**Validation Rules:**
- Name is required, must be unique per organization, 1-255 characters
- Status defaults to ACTIVE
- Supports soft deletes (deletedAt for compliance and recovery)

**Relationships:**
- `users`: One-to-many with User (organization members)
- `roles`: One-to-many with Role (organization-specific roles)
- `documents`: One-to-many with Document (documents owned by organization)
- `folders`: One-to-many with Folder (folder hierarchy)
- `permissions`: One-to-many with Permission (permission matrix)
- `auditLogs`: One-to-many with AuditLog (compliance tracking)

---

### 2. User
Represents a system user who can authenticate and access documents within their organization(s).

**Fields:**
- `id`: Long - Unique user identifier (Primary Key)
- `email`: String (max 255 characters) - User email address
- `password`: String - BCrypt-hashed password
- `firstName`: String (max 100 characters) - User's first name
- `lastName`: String (max 100 characters) - User's last name
- `isActive`: Boolean - Whether user account is active
- `lastLoginAt`: DateTime (nullable) - Last login timestamp
- `createdAt`: DateTime - User account creation timestamp
- `updatedAt`: DateTime - Last user update timestamp
- `deletedAt`: DateTime (nullable) - Soft delete timestamp

**Validation Rules:**
- Email is required, must be unique globally, must follow RFC 5322 format
- Password is required (minimum 8 characters, salted and hashed with BCrypt)
- First and last names are required, 1-100 characters each
- Email and password are immutable after creation (update requires special flow)
- Supports soft deletes (deletedAt for GDPR compliance)

**Relationships:**
- `userOrganizations`: Many-to-many with Organization (through UserOrganization)
- `roles`: Many-to-many with Role (user roles within organizations)
- `documents`: One-to-many with Document (documents created by user)
- `auditLogs`: One-to-many with AuditLog (user activity tracking)

---

### 3. UserOrganization
Join table for many-to-many relationship between User and Organization, tracking membership and default organization.

**Fields:**
- `id`: Long - Unique identifier (Primary Key)
- `userId`: Long - Foreign key to User
- `organizationId`: Long - Foreign key to Organization
- `isDefault`: Boolean - Whether this is user's default organization
- `joinedAt`: DateTime - When user joined organization
- `leftAt`: DateTime (nullable) - When user left organization (if soft-deleted)

**Validation Rules:**
- Each user can have exactly one default organization per organization they belong to
- isDefault defaults to false
- A user without any organizations cannot log in
- A user with multiple organizations and no default gets 409 CONFLICT on login

**Relationships:**
- `user`: Many-to-one with User
- `organization`: Many-to-one with Organization

---

### 4. Role
Represents a role within an organization (e.g., Admin, Editor, Viewer). Defines sets of permissions.

**Fields:**
- `id`: Long - Unique role identifier (Primary Key)
- `organizationId`: Long - Foreign key to Organization
- `name`: String (max 100 characters) - Role name (e.g., "Administrator", "Editor")
- `description`: String (max 500 characters, nullable) - Role description
- `isSystem`: Boolean - Whether role is system-defined (cannot be deleted)
- `createdAt`: DateTime - Role creation timestamp
- `deletedAt`: DateTime (nullable) - Soft delete timestamp

**Validation Rules:**
- Name must be unique per organization, 1-100 characters
- System roles (Admin, Editor, Viewer) cannot be deleted or modified
- Cannot delete a role if users are assigned to it
- isSystem defaults to false

**System Roles:**
- `ADMIN`: Full access to all documents and permissions in organization
- `EDITOR`: Can create, read, update documents; manage own documents
- `VIEWER`: Read-only access to documents based on permissions

**Relationships:**
- `organization`: Many-to-one with Organization
- `users`: Many-to-many with User
- `permissions`: One-to-many with Permission

---

### 5. Permission
Represents a granular permission granted to a role for a specific resource (document or folder).

**Fields:**
- `id`: Long - Unique permission identifier (Primary Key)
- `roleId`: Long - Foreign key to Role
- `resourceId`: Long - ID of document or folder
- `resourceType`: Enum [DOCUMENT, FOLDER] - Type of resource
- `action`: Enum [READ, CREATE, UPDATE, DELETE, SHARE, MANAGE_PERMISSIONS] - Permitted action
- `grantedAt`: DateTime - When permission was granted
- `grantedBy`: Long (nullable) - User ID who granted permission
- `expiresAt`: DateTime (nullable) - Optional expiration date for temporary permissions

**Validation Rules:**
- Role, resource, and action combination must be unique
- Cannot grant permission for non-existent resources
- DELETE and SHARE actions can only be granted to Admin and Editor roles
- Cascading permissions supported for folders (apply to all children)

**Action Definitions:**
- `READ`: View document content and metadata
- `CREATE`: Create new documents/folders within a folder
- `UPDATE`: Modify document metadata and content
- `DELETE`: Delete documents (soft-delete by default)
- `SHARE`: Share documents with other users
- `MANAGE_PERMISSIONS`: Modify permissions for this resource

**Relationships:**
- `role`: Many-to-one with Role
- `auditLogs`: One-to-many with AuditLog

---

### 6. Folder
Represents a hierarchical folder structure for organizing documents.

**Fields:**
- `id`: Long - Unique folder identifier (Primary Key)
- `organizationId`: Long - Foreign key to Organization
- `name`: String (max 255 characters) - Folder name
- `description`: String (max 500 characters, nullable) - Folder description
- `parentId`: Long (nullable) - Foreign key to parent Folder (null for root folders)
- `createdBy`: Long - Foreign key to User who created folder
- `createdAt`: DateTime - Folder creation timestamp
- `updatedAt`: DateTime - Last folder update timestamp
- `deletedAt`: DateTime (nullable) - Soft delete timestamp

**Validation Rules:**
- Name is required, 1-255 characters
- Must not have circular parent relationships
- Folders can be nested to any depth
- Deleting a folder soft-deletes all child documents and folders
- Folder names must be unique per parent folder (within organization)

**Relationships:**
- `organization`: Many-to-one with Organization
- `documents`: One-to-many with Document
- `subfolders`: One-to-many with Folder (self-referential for children)
- `parentFolder`: Many-to-one with Folder (self-referential for parent)
- `createdByUser`: Many-to-one with User

---

### 7. Document
Represents a document with versioning support. Current version is tracked; previous versions are archived.

**Fields:**
- `id`: Long - Unique document identifier (Primary Key)
- `organizationId`: Long - Foreign key to Organization
- `folderId`: Long (nullable) - Foreign key to parent Folder
- `name`: String (max 255 characters) - Document name
- `description`: String (max 500 characters, nullable) - Document description
- `contentType`: String (max 100 characters) - MIME type (e.g., "application/pdf")
- `fileSize`: Long - Current file size in bytes
- `currentVersionId`: Long - Foreign key to current DocumentVersion
- `versionCount`: Integer - Total number of versions
- `isLocked`: Boolean - Whether document is locked from editing
- `lockedBy`: Long (nullable) - User ID who locked document
- `lockedAt`: DateTime (nullable) - When document was locked
- `tags`: Array of String - Tags for categorization and search
- `metadata`: JSON/Map - Custom metadata key-value pairs
- `createdBy`: Long - Foreign key to User who created document
- `createdAt`: DateTime - Document creation timestamp
- `updatedAt`: DateTime - Last document update timestamp
- `deletedAt`: DateTime (nullable) - Soft delete timestamp

**Validation Rules:**
- Name is required, 1-255 characters, unique per folder
- ContentType must be whitelisted (PDF, DOCX, XLSX, PNG, JPG, etc.)
- FileSize must not exceed organization quota (default 500MB per document)
- Cannot modify locked documents
- Cannot delete without DELETE permission
- Supports soft deletes with compliance tracking

**Relationships:**
- `organization`: Many-to-one with Organization
- `folder`: Many-to-one with Folder
- `versions`: One-to-many with DocumentVersion (document history)
- `createdByUser`: Many-to-one with User
- `auditLogs`: One-to-many with AuditLog

---

### 8. NivelAcceso (Access Level)
Represents a catalog of access levels for Role-Based Access Control (RBAC). Defines standard permission levels: LECTURA (Read), ESCRITURA (Write), and ADMINISTRACION (Administration).

**Fields:**
- `id`: Long - Unique access level identifier (Primary Key)
- `codigo`: String (max 50 characters) - Invariable code [LECTURA, ESCRITURA, ADMINISTRACION] (Unique)
- `nombre`: String (max 100 characters) - Display name of the access level
- `descripcion`: Text (nullable) - Detailed description of permissions granted
- `accionesPermitidas`: JSONB/Array - List of allowed actions (e.g., ["ver", "listar", "descargar"])
- `orden`: Integer - Display order for UI listings
- `activo`: Boolean - Whether this level is currently active (default: true)
- `createdAt`: DateTime - Access level creation timestamp
- `updatedAt`: DateTime - Last access level update timestamp

**Validation Rules:**
- Codigo must be one of: LECTURA, ESCRITURA, ADMINISTRACION
- Codigo is unique and invariable (cannot be changed after creation)
- Nombre is required, 1-100 characters
- accionesPermitidas must be a valid JSON array of strings
- Standard levels are seeded via database migrations (idempotent)

**Standard Access Levels:**
1. **LECTURA**: View, list, and download documents (read-only)
   - Actions: `["ver", "listar", "descargar"]`
2. **ESCRITURA**: Upload new versions, rename, modify metadata
   - Actions: `["ver", "listar", "descargar", "subir", "modificar", "crear_version"]`
3. **ADMINISTRACION**: Full control including delete and permission management
   - Actions: `["ver", "listar", "descargar", "subir", "modificar", "crear_version", "eliminar", "administrar_permisos", "cambiar_version_actual"]`

**Relationships:**
- `permissions`: One-to-many with Permission (roles/users with this access level)

---

### 9. DocumentVersion
Represents a specific version of a document. Supports complete versioning history with linear progression.

**Fields:**
- `id`: Long - Unique version identifier (Primary Key)
- `documentId`: Long - Foreign key to Document
- `versionNumber`: Integer - Sequential version number (1, 2, 3, ...)
- `fileSize`: Long - File size for this version
- `storagePath`: String - Path in MinIO/S3 for this version
- `contentHash`: String - SHA256 hash of file content (for deduplication)
- `changeComment`: String (max 500 characters, nullable) - What changed in this version
- `createdBy`: Long - Foreign key to User who created this version
- `createdAt`: DateTime - Version creation timestamp
- `downloadCount`: Integer - How many times this version was downloaded
- `lastDownloadedAt`: DateTime (nullable) - When version was last downloaded

**Validation Rules:**
- Version numbers must be sequential per document
- Cannot modify or delete versions (immutable audit trail)
- Storage path must be unique in MinIO
- Content hash enables deduplication and integrity checking
- Version count can grow indefinitely; old versions can be archived

**Relationships:**
- `document`: Many-to-one with Document
- `createdByUser`: Many-to-one with User
- `auditLogs`: One-to-many with AuditLog

---

### 10. AuditLog
Represents immutable audit trail entries for compliance, tracking all user actions on documents and system operations.

**Fields:**
- `id`: Long - Unique audit log entry ID (Primary Key)
- `organizationId`: Long - Foreign key to Organization (for multi-tenancy filtering)
- `userId`: Long - Foreign key to User who performed action
- `action`: Enum [CREATE, READ, UPDATE, DELETE, DOWNLOAD, SHARE, LOGIN, LOGOUT, PERMISSION_CHANGE, LOCK, UNLOCK] - Action performed
- `resourceType`: Enum [DOCUMENT, FOLDER, USER, ROLE, PERMISSION, ORGANIZATION] - Type of resource
- `resourceId`: Long - ID of affected resource
- `resourceName`: String - Name of affected resource (for human readability)
- `changeDetails`: JSON/Map (nullable) - Before/after values for updates
- `ipAddress`: String (nullable) - IP address of user
- `userAgent`: String (nullable) - User agent string
- `timestamp`: DateTime - When action occurred (immutable)
- `status`: Enum [SUCCESS, FAILURE] - Whether action succeeded
- `errorMessage`: String (nullable) - Error message if action failed

**Validation Rules:**
- All fields are immutable (no updates after creation)
- Timestamp is set at creation time, not modifiable
- Used for compliance, security auditing, and forensics
- Should be retained per legal requirements (typically 7+ years)
- organizationId enables GDPR-compliant data export/deletion per organization

**Relationships:**
- `organization`: Many-to-one with Organization
- `user`: Many-to-one with User (nullable for system actions)

---

## Entity Relationship Diagram

```mermaid
erDiagram
    Organization {
        Long id PK
        String name UK
        String description
        String status
        DateTime createdAt
        DateTime updatedAt
        DateTime deletedAt
    }
    
    User {
        Long id PK
        String email UK
        String password
        String firstName
        String lastName
        Boolean isActive
        DateTime lastLoginAt
        DateTime createdAt
        DateTime updatedAt
        DateTime deletedAt
    }
    
    UserOrganization {
        Long id PK
        Long userId FK
        Long organizationId FK
        Boolean isDefault
        DateTime joinedAt
        DateTime leftAt
    }
    
    Role {
        Long id PK
        Long organizationId FK
        String name UK
        String description
        Boolean isSystem
        DateTime createdAt
        DateTime deletedAt
    }
    
    Permission {
        Long id PK
        Long roleId FK
        Long resourceId
        String resourceType
        String action
        DateTime grantedAt
        Long grantedBy
        DateTime expiresAt
    }
    
    Folder {
        Long id PK
        Long organizationId FK
        Long parentId
        String name UK
        String description
        Long createdBy FK
        DateTime createdAt
        DateTime updatedAt
        DateTime deletedAt
    }
    
    Document {
        Long id PK
        Long organizationId FK
        Long folderId FK
        String name
        String description
        String contentType
        Long fileSize
        Long currentVersionId FK
        Integer versionCount
        Boolean isLocked
        Long lockedBy
        DateTime lockedAt
        String[] tags
        JSON metadata
        Long createdBy FK
        DateTime createdAt
        DateTime updatedAt
        DateTime deletedAt
    }
    
    NivelAcceso {
        Long id PK
        String codigo UK
        String nombre
        String descripcion
        JSONB accionesPermitidas
        Integer orden
        Boolean activo
        DateTime createdAt
        DateTime updatedAt
    }
    
    DocumentVersion {
        Long id PK
        Long documentId FK
        Integer versionNumber UK
        Long fileSize
        String storagePath UK
        String contentHash
        String changeComment
        Long createdBy FK
        DateTime createdAt
        Integer downloadCount
        DateTime lastDownloadedAt
    }
    
    AuditLog {
        Long id PK
        Long organizationId FK
        Long userId FK
        String action
        String resourceType
        Long resourceId
        String resourceName
        JSON changeDetails
        String ipAddress
        String userAgent
        DateTime timestamp
        String status
        String errorMessage
    }

    Organization ||--o{ User : "employs via UserOrganization"
    Organization ||--o{ Role : "defines"
    Organization ||--o{ Folder : "contains root"
    Organization ||--o{ Document : "contains"
    Organization ||--o{ AuditLog : "tracks"

    User ||--o{ UserOrganization : "belongs to"
    User ||--o{ Role : "assigned via UserOrganization"
    User ||--o{ Document : "creates"
    User ||--o{ Folder : "creates"
    User ||--o{ DocumentVersion : "creates"
    User ||--o{ AuditLog : "performs"

    Role ||--o{ Permission : "grants"
    
    Permission }o--|| NivelAcceso : "uses access level"
    
    Folder ||--o{ Folder : "contains subfolders"
    Folder ||--o{ Document : "contains documents"
    
    Document ||--o{ DocumentVersion : "has versions"
    Document ||--o{ AuditLog : "tracked in"
    
    Permission ||--o{ AuditLog : "changes tracked in"
    
    DocumentVersion ||--o{ AuditLog : "tracked in"
```

---

## Key Design Principles

### 1. Multi-Tenancy
- Every resource is tagged with `organizationId`
- Queries automatically filtered by organization context from JWT token
- Complete data isolation per organization

### 2. Soft Deletes
- All entities support `deletedAt` field
- Deleted data remains in database for compliance and recovery
- Queries exclude soft-deleted records by default
- GDPR-compliant data export/deletion possible by organization

### 3. Audit Trail
- `AuditLog` is immutable and captures every action
- Enables compliance, forensics, and accountability
- Includes IP address and user agent for security analysis
- Tracks before/after values for data changes

### 4. Role-Based Access Control (RBAC)
- Fine-grained permissions per role
- Supports system roles (ADMIN, EDITOR, VIEWER) and custom roles
- Permissions can expire (temporary access)
- Cascading permissions for folders

### 5. Document Versioning
- Linear versioning: version numbers increment sequentially (1, 2, 3, ...)
- Each version is immutable with complete history
- Content hash enables deduplication and integrity verification
- All versions stored in MinIO; metadata in PostgreSQL

### 6. Hexagonal Architecture
- Entities live in `domain` layer (business logic only)
- Repositories as output ports in `application` layer
- REST controllers as input adapters in `infrastructure` layer
- External dependencies (DB, S3) in infrastructure adapters

### 7. Validation & Constraints
- Email uniqueness enforced at database and application level
- Foreign key constraints maintain referential integrity
- Unique constraints on sensitive identifiers (email, role name per org)
- Check constraints for enum values

---

## Database-Specific Notes

### PostgreSQL (Relational Data)
- Used for: Users, Organizations, Roles, Permissions, Folders, Documents, DocumentVersions, AuditLogs
- Connection: `postgresql://docflow:docflow_secret@localhost:5432/docflow`
- Supports: Full ACID transactions, JSON fields for metadata, Long types
- Indexes on: `email`, `organizationId`, `createdAt`, `deletedAt` for query performance

### MongoDB (Audit Logs - Optional)
- Can optionally store `AuditLog` in MongoDB for time-series optimization
- Collections: `audit_logs`
- TTL indexes for automatic archival after retention period
- Connection: `mongodb://localhost:27017/auditlog`

### MinIO / S3 (Document Storage)
- Stores actual document files (binaries)
- Bucket: `docflow-documents`
- Path structure: `/{organizationId}/{documentId}/{versionId}/{filename}`
- Supports: Versioning, lifecycle policies, server-side encryption

---

## Conventions & Standards

### Naming Conventions
- **Entities**: PascalCase (User, Organization, Document)
- **Fields**: camelCase (createdAt, organizationId, isActive)
- **Enums**: UPPER_SNAKE_CASE (ACTIVE, READ, DELETE)
- **Databases**: lowercase with underscores (docflow_db, audit_log)
- **Columns**: snake_case in SQL (created_at, organization_id)

### Timestamp Standards
- All timestamps in UTC/ISO 8601 format
- `createdAt`: Immutable, set at entity creation
- `updatedAt`: Mutable, tracks last modification
- `deletedAt`: Set when soft-deleted, not cleared on undelete
- `timestamp` (in AuditLog): Immutable, matches action time

### ID Strategy
- **Primary Keys**: Long v4 (immutable, distributed-friendly)
- **Foreign Keys**: Long references
- **No sequential IDs**: Enables decentralized generation

### Error Handling
- Specific exceptions for business logic (PermissionDeniedException, ResourceNotFoundException)
- OWASP-compliant validation (no SQL injection, XSS, CSRF)
- Error responses include: error code, message, timestamp, HTTP status

---

## Migration & Evolution

- **Database**: Use Flyway for versioned migrations
- **Backward Compatibility**: New fields are nullable; old consumers unaffected
- **Adding Permissions**: Extend Permission.action enum and update RBAC logic
- **Archiving**: Implement retention policies on AuditLog and DocumentVersion

---

## Security Considerations

- Passwords hashed with BCrypt (min cost factor 10)
- JWT tokens include organization context and roles
- Audit logs track all permission changes
- Soft deletes prevent accidental data loss
- Stateless authentication (no server-side sessions)
- Document files encrypted at rest in MinIO
- Email fields have unique constraints to prevent duplicate accounts 