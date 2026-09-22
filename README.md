# 🎓 ScholarTrust — Blockchain-Powered Transparent Scholarship Disbursal System

ScholarTrust is a decentralized and tamper-evident scholarship management and disbursal system. It combines a high-performance **Spring Boot** backend with an immutable **Ethereum (Ganache)** smart contract and a clean **vanilla HTML5/CSS3/JavaScript** frontend.

---

## 🏗️ Architecture Overview (Hybrid Model)

- **Relational Data (MySQL `scholartrust_db`):** User credentials, student profiles, scholarship definitions, application records, remarks, and audit trails.
- **Off-Chain File Storage (`uploads/documents/`):** Student marksheets, income certificates, and ID proofs stored in portable relative paths.
- **On-Chain Immutable Ledger (Ethereum / Ganache):**
  - Cryptographic **SHA-256 hashes** of uploaded documents anchored in smart contract.
  - Lifecycle state changes (`PENDING` -> `APPROVED` -> `DISBURSED`).
  - Strict disbursement transactions recorded with block numbers and transaction hashes.

```text
Student Registration (ROLE_STUDENT)
        ↓
Student Login (JWT Authentication)
        ↓
Student Profile Setup (Roll No, GPA, Wallet Address)
        ↓
Browse Scholarships & Check Eligibility
        ↓
Apply & Upload Documents (Strict MIME & Extension Whitelist)
        ↓
SHA-256 Document Hash Calculated
        ↓
Blockchain Application Record Anchored
        ↓
Admin Review & On-Chain Hash Integrity Verification (Tamper Detection)
        ↓
Admin Approval
        ↓
On-Chain Disbursement (Strict Ethereum Wallet Validation)
        ↓
Cryptographic Receipt Generation (JSON & Printable HTML)
        ↓
Public Blockchain Audit (Zero-Trust Ledger Verification)
```

---

## 🔒 Security Hardening & Enhancements

1. **Privilege Escalation Prevention:**
   - Public registration strictly enforces `ROLE_STUDENT`.
   - Any attempt to self-assign `ROLE_ADMIN` via `/api/auth/register` is rejected with `HTTP 400 Bad Request`.
   - Administrator accounts are strictly provisioned and maintained via database migration/seeding.
2. **Insecure Direct Object Reference (IDOR) Protection:**
   - Both `/api/applications/documents/{id}/download` and `/api/applications/{id}/verify-hash` enforce strict ownership checks.
   - Non-administrators can only access documents and verification data belonging to their own applications. Unauthorized attempts return `HTTP 403 Forbidden`.
3. **Database & Blockchain Consistency:**
   - Applications are never falsely marked `DISBURSED` if the blockchain transaction fails or reverts.
   - Removed silent fallback test wallets: disbursement strictly requires a validated 40-hex character Ethereum address (`0x...`).
4. **File Upload Security & Privacy:**
   - Strict content-type and extension validation (`.pdf`, `.jpg`, `.jpeg`, `.png`, `.txt`).
   - Path traversal prevention (`..` rejected) and safe alphanumeric filename sanitization.
   - Maximum upload limit enforced (10MB).
   - Real identity files purged and replaced with fictional test fixtures.
   - Portable relative paths (`uploads/documents/...`) replacing machine-specific absolute paths.
5. **Configuration Externalization:**
   - Sensitive credentials externalized to environment variables with fallback defaults (`.env.example` provided).

---

## 📂 Project Structure

```
V:\Projects\scholartrust\
├── contracts\                   # Solidity smart contract & deployment scripts
│   ├── ScholarshipLedger.sol   # Core smart contract
│   ├── deploy.js               # Web3 deployment script
│   └── test_contract.js        # Contract unit test script
├── src\
│   ├── main\
│   │   ├── java\com\scholarship\scholartrust\
│   │   │   ├── config\         # Security & filter configuration
│   │   │   ├── controller\     # REST Controllers (Auth, Student, Admin, Blockchain, Profile)
│   │   │   ├── dto\            # Data Transfer Objects (Requests, Responses, Receipts)
│   │   │   ├── entity\         # JPA Entities (User, Application, Document, Disbursement, etc.)
│   │   │   ├── repository\     # Spring Data Repositories
│   │   │   ├── security\       # JWT Filters, JwtUtil, CustomUserDetails
│   │   │   └── service\        # Business logic & Web3j blockchain services
│   │   └── resources\
│   │       ├── application.properties # Externalized configurations with env vars
│   │       └── static\         # Clean vanilla Frontend
│   │           ├── index.html  # Landing portal with auto-redirect
│   │           ├── css\style.css # Clean design system
│   │           ├── js\api.js   # JWT handling & API client
│   │           ├── student\    # Student Portal (Login, Register, Dashboard, Apply, My Applications)
│   │           └── admin\      # Admin Portal (Login, Dashboard, Scholarships, Applications, Audit)
│   └── test\                   # Comprehensive Automated Test Suite
│       └── java\com\scholarship\scholartrust\service\
│           ├── AuthServiceTest.java          # Registration & Login Security Tests
│           ├── ScholarshipServiceTest.java  # Scholarship & Eligibility Tests
│           ├── ApplicationServiceTest.java  # IDOR, Application & Consistency Tests
│           └── FileStorageServiceTest.java  # File Validation & SHA-256 Tests
├── uploads\documents\          # Uploaded student documents (relative storage)
├── scholartrust_dump.sql       # Clean MySQL database export
├── .env.example                # Environment variables template
├── .gitignore                  # Git exclusion rules
├── pom.xml                     # Maven configuration (Java 17, Spring Boot 3.2.4)
├── package.json                # Node dependencies (Ganache, Solc, Web3)
├── start_all.bat               # One-click launcher for Ganache, Contract & Spring Boot
├── start_ganache.bat           # Starts local blockchain node on port 8545
├── deploy_contract.bat         # Compiles and deploys smart contract
├── start_backend.bat           # Starts Spring Boot server on port 8080
└── test_hardened_e2e.ps1       # Automated 16-point End-to-End PowerShell test suite
```

---

## ⚡ Quick Start Instructions

### 1. Prerequisites
- **Java:** OpenJDK 17 or higher
- **Maven:** Apache Maven 3.9+
- **Node.js:** v18+ with `npx`
- **MySQL:** Port 3306 with database `scholartrust_db` (password: `venkat123`)

### 2. Launch Everything (One Click)
Double-click:
```
V:\Projects\scholartrust\start_all.bat
```
This automatically:
1. Launches the Ganache blockchain node on `http://127.0.0.1:8545`
2. Deploys the `ScholarshipLedger.sol` contract
3. Starts the Spring Boot backend on `http://localhost:8080`

### 3. Or Launch Manually:
**Terminal 1 — Ganache:**
```powershell
npx ganache --wallet.deterministic --server.port 8545
```
**Terminal 2 — Spring Boot:**
```powershell
mvn spring-boot:run
```

---

## 🧪 Automated Testing

### Unit Test Suite (21 Tests)
Execute all unit tests using Maven:
```powershell
mvn test
```
- **AuthServiceTest (6 tests):** Enforces `ROLE_STUDENT`, rejects public `ROLE_ADMIN`, validates duplicate emails and wallet format, tests login and bad credential handling.
- **ScholarshipServiceTest (5 tests):** Validates scholarship creation, deadline checks, and academic GPA/income eligibility calculations.
- **ApplicationServiceTest (6 tests):** Validates duplicate submission rejection, IDOR prevention on documents/hashes, strict wallet validation, blockchain consistency upon revert, and receipt creation.
- **FileStorageServiceTest (4 tests):** Tests file uploads, extension whitelisting, path traversal protection, and SHA-256 consistency.

### End-to-End Test Suite (16 Verification Points)
Run the automated PowerShell E2E test script against the running backend:
```powershell
powershell -ExecutionPolicy Bypass -File .\test_hardened_e2e.ps1
```
Expected output: **16 PASSED, 0 FAILED (100% Success)**.

---

## 🌐 Web Portals & URLs

| Portal | URL | Description |
|---|---|---|
| **Main Landing** | `http://localhost:8080/` | Role-based redirection |
| **Student Register** | `http://localhost:8080/student/register.html` | Account signup (strictly ROLE_STUDENT) |
| **Student Login** | `http://localhost:8080/student/login.html` | Student dashboard access |
| **Browse Scholarships**| `http://localhost:8080/student/scholarships.html` | View & apply with file upload |
| **My Applications** | `http://localhost:8080/student/my-applications.html`| Status & cryptographic hash audit |
| **Admin Login** | `http://localhost:8080/admin/login.html` | Administrator access |
| **Admin Dashboard** | `http://localhost:8080/admin/dashboard.html` | System-wide statistics |
| **Manage Scholarships**| `http://localhost:8080/admin/scholarships.html` | Full CRUD operations |
| **Application Review** | `http://localhost:8080/admin/applications.html` | Approve/Reject & on-chain disburse |
| **Blockchain Status** | `http://localhost:8080/admin/blockchain.html` | Live network & ledger audit |
| **Disbursement Receipt**| `http://localhost:8080/api/applications/{id}/receipt/html` | Printable cryptographic receipt |

---

## 🔑 Test Accounts

- **Administrator:**
  - Email: `admin@college.edu` or `admin2@scholartrust.com`
  - Password: `Admin@123`

- **Student:**
  - Email: `student@college.edu`
  - Password: `Password@123`
  *(Or register any new student directly through `/student/register.html`)*

---

## 🎓 Academic Prototype Limitations

1. **Local Blockchain (Ganache):**
   - The project utilizes a local deterministic Ganache node (`127.0.0.1:8545`) for demonstrability and zero gas fees. In a national production rollout, an enterprise consortium blockchain (such as Polygon PoS, Hyperledger Besu, or an Ethereum L2) would be employed.
2. **Disbursement Simulation:**
   - The smart contract records the immutable disbursement grant and transaction hash on the blockchain ledger. In actual production, fiat bank APIs (e.g., Direct Benefit Transfer / NPCI Aadhaar Payment Bridge) or Central Bank Digital Currencies (CBDCs) would be wired into the disbursal oracle.
3. **Document Verification Scope:**
   - The SHA-256 hash guarantees that the uploaded document has not been altered or tampered with after submission. It verifies **document integrity** (tamper evidence), not external institutional authenticity (which requires integration with DigiLocker or university registrar issuing authorities).
