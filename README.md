# 🎓 ScholarTrust — Blockchain-Powered Transparent Scholarship Disbursal System
*(Conference-Paper-Grade Prototype & Empirical Evaluation Suite)*

ScholarTrust is a decentralized, tamper-evident scholarship management and disbursal system designed to solve the critical problem:
> **"Scholarship disbursement lacks transparency, auditability, and traceability."**

It combines a high-performance **Spring Boot** backend with an immutable **Ethereum (EVM / Ganache)** smart contract and a clean **vanilla HTML5/CSS3/JavaScript** frontend.

---

## 🏗️ Architecture Overview (Hybrid Model)

- **Relational Data (MySQL `scholartrust_db`):** User credentials, student profiles, institutional verification records, scholarship definitions, application snapshots, remarks, and audit trails.
- **Off-Chain File Storage (`uploads/documents/`):** Student marksheets, income certificates, and institutional ID proofs stored with magic-byte validation and relative path resolution.
- **On-Chain Immutable Ledger (Ethereum / Ganache):**
  - Cryptographic **SHA-256 hashes** anchored in smart contract (`ScholarshipLedger.sol`).
  - Strict EVM state machine lifecycle (`SUBMITTED` -> `UNDER_REVIEW` -> `APPROVED` -> `DISBURSED`). Direct transitions to `DISBURSED` without prior `APPROVED` status revert at the EVM opcode level.
  - Strict disbursement transactions recorded with block numbers and transaction hashes.

```text
Student Registration (ROLE_STUDENT)
        ↓
Student Login (JWT Authentication)
        ↓
Complete Academic & Wallet Profile (Roll No, GPA, Wallet Address)
        ↓
Institutional Identity Verification (ID Card Upload + SHA-256 Digest)
        ↓
Admin Identity Verification Review (Approve ID)
        ↓
Browse Scholarships & Automatic Eligibility Evaluation
        ↓
Apply & Upload Documents (Magic Byte Signature & MIME Whitelist)
        ↓
SHA-256 Document Hash Calculated
        ↓
Blockchain Application Record Anchored
        ↓
Admin Review & On-Chain Hash Integrity Verification (Tamper Detection)
        ↓
Admin Approval (Blockchain Status Update)
        ↓
On-Chain Disbursement (Strict Ethereum Wallet Validation)
        ↓
Cryptographic Receipt Generation (JSON & Sanitized Printable HTML)
        ↓
Public Blockchain Audit (Zero-Trust Ledger Verification)
```

---

## 🔒 Security Hardening & Defect Rework Summary

1. **Privilege Escalation Prevention (`SEC-02`):**
   - Public registration strictly enforces `ROLE_STUDENT`.
   - Any attempt to self-assign `ROLE_ADMIN` via `/api/auth/register` is rejected with `HTTP 400 Bad Request`.
2. **Insecure Direct Object Reference (IDOR) Protection (`SEC-04`):**
   - Both `/api/applications/documents/{id}/download` and `/api/applications/{id}/verify-hash` enforce strict ownership checks.
   - Non-administrators can only access documents and verification data belonging to their own applications. Unauthorized attempts return `HTTP 403 Forbidden`.
3. **Database & Blockchain Consistency (`CHAIN-01`, `CHAIN-02`):**
   - Applications are never falsely marked `DISBURSED` if the blockchain transaction fails or reverts.
   - Disbursing an unapproved application is strictly rejected on-chain by `ScholarshipLedger.sol`.
   - Removed silent fallback test wallets: disbursement strictly requires a validated 40-hex character Ethereum address (`0x...`).
4. **File Upload Security & Signature Verification (`SEC-01`):**
   - Magic-byte signature verification prevents MIME spoofing (`%PDF` for PDF, `FF D8 FF` for JPEG, `89 50 4E 47` for PNG).
   - Path traversal prevention (`..` rejected) and safe alphanumeric filename sanitization.
5. **XSS Sanitization & Strict CORS (`SEC-03`):**
   - Dynamic user parameters in printable receipts (`getReceiptHtml`) are fully HTML-escaped.
   - CORS is restricted to trusted origins with strict header controls.
6. **Institutional Identity Verification Gate (`IDENT-01`):**
   - Mandatory institutional ID card upload and administrative verification before students can apply for scholarships.

---

## 📊 Empirical Evaluation for Conference Paper (`EXP-01`)

An automated empirical benchmark script (`benchmark/benchmark_experiments.js`) was executed against the smart contract on local EVM RPC to measure latency and gas overhead:

### Experimental Metrics (Trial Sample $N = 20$):

| Operation | Metric | Mean | Std Dev | Gas Consumption |
|---|---|---|---|---|
| **Document Hash Digest** | Execution Time | `0.0550 ms` | `0.0780 ms` | — (Client CPU) |
| **Record Application On-Chain** | Transaction Latency | `26.56 ms` | `7.90 ms` | `255,657 units` |
| **Cryptographic Hash Query** | Call Latency | `7.24 ms` | `2.24 ms` | `0 units` (Read-only call) |
| **Disbursement Transaction** | Transaction Latency | `18.44 ms` | `1.98 ms` | `150,138 units` |

> Full trial dataset synchronized in [`benchmark/benchmark_results.csv`](benchmark/benchmark_results.csv).

To reproduce benchmarks:
```bash
node benchmark/benchmark_experiments.js 20
```

---

## 🚀 Step-by-Step Setup & Running

### Prerequisites
- **Java 17 JDK**
- **Maven 3.8+**
- **Node.js 18+**
- **MySQL 8.0+**

### 1. Database Setup
```sql
CREATE DATABASE IF NOT EXISTS scholartrust_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Start Blockchain Node (Ganache)
```bash
npx ganache --wallet.deterministic --server.port 8545 --chain.chainId 1337
```

### 3. Deploy Smart Contract
```bash
node contracts/deploy.js
```
The deploy script compiles `ScholarshipLedger.sol` and synchronizes the deployed address across `contracts/deployment.json` and `src/main/resources/application.properties`.

### 4. Build and Run Backend
```bash
mvn clean test
mvn spring-boot:run
```

The application runs on `http://localhost:8080`.

---

## 🧪 Unit & Integration Testing

Run all unit and integration tests:
```bash
mvn test
```
**Test Results:** **23/23 tests pass (100% Success)**:
- `ApplicationServiceTest` (8 tests): Snapshot validation, duplicate prevention, IDOR protection, state machine consistency, receipt generation.
- `AuthServiceTest` (6 tests): Role validation, registration safety, credential checks.
- `FileStorageServiceTest` (4 tests): Magic-byte checks, extension whitelisting, SHA-256 consistency.
- `ScholarshipServiceTest` (5 tests): Strict eligibility evaluation, deadline checks.

---

## 🌐 Web Portals & URLs

| Portal | URL | Description |
|---|---|---|
| **Main Landing** | `http://localhost:8080/` | Role-based redirection |
| **Student Register** | `http://localhost:8080/student/register.html` | Account signup (strictly ROLE_STUDENT) |
| **Student Login** | `http://localhost:8080/student/login.html` | Student dashboard access |
| **Student Profile & ID** | `http://localhost:8080/student/profile.html` | Complete profile & upload college ID card |
| **Browse Scholarships**| `http://localhost:8080/student/scholarships.html` | View & apply with file upload |
| **My Applications** | `http://localhost:8080/student/my-applications.html`| Status & cryptographic hash audit |
| **Admin Login** | `http://localhost:8080/admin/login.html` | Administrator access |
| **Admin Dashboard** | `http://localhost:8080/admin/dashboard.html` | System-wide statistics |
| **Admin Verifications**| `http://localhost:8080/admin/verifications.html` | Review and verify student ID cards |
| **Manage Scholarships**| `http://localhost:8080/admin/scholarships.html` | Full CRUD operations |
| **Application Review** | `http://localhost:8080/admin/applications.html` | Approve/Reject & on-chain disburse |
| **Blockchain Status** | `http://localhost:8080/admin/blockchain.html` | Live network & ledger audit |
| **Disbursement Receipt**| `http://localhost:8080/api/applications/{id}/receipt/html` | Printable cryptographic receipt |

---

## 🔑 Test Accounts

- **Administrator:**
  - Email: `admin@college.edu`
  - Password: `Admin@123`

- **Student:**
  - Email: `student@college.edu`
  - Password: `Password@123`
  *(Or register any new student directly through `/student/register.html`)*
