# 🔐 Secure Password Generator

A production-grade, cryptographically secure password generator.
**Java 21 + Spring Boot 3.2 backend · React 18 + Vite frontend**

---

## ✅ Prerequisites

Install these before running:

| Tool     | Version  | Download |
|----------|----------|----------|
| Java JDK | 21+      | https://adoptium.net |
| Maven    | 3.9+     | https://maven.apache.org/download.cgi |
| Node.js  | 20+      | https://nodejs.org |

### Verify installations (open a NEW terminal after installing):
```cmd
java -version
mvn -version
node -version
npm -version
```

---

## 🚀 Run the Backend (Spring Boot)

Open **Terminal / Command Prompt**:

```cmd
cd C:\path\to\secure-password-generator\backend

mvn clean install -DskipTests

mvn spring-boot:run
```

Wait for this message:
```
Started SecurePasswordGeneratorApplication in X.XXX seconds
Tomcat started on port(s): 8080
```

### ✅ Verify backend is running:
Open browser → http://localhost:8080/api/v1/password/health

Expected response:
```json
{ "status": "UP", "version": "1.0.0" }
```

**Swagger UI:** http://localhost:8080/swagger-ui.html

---

## 🎨 Run the Frontend (React + Vite)

Open a **SECOND terminal** (keep backend running):

```cmd
cd C:\path\to\secure-password-generator\frontend

npm install

npm run dev
```

Wait for:
```
VITE v5.x.x  ready in XXX ms
➜  Local:   http://localhost:5173/
```

Open browser → **http://localhost:5173**

---

## 🐳 Run with Docker (Alternative)

Requires Docker Desktop installed and running.

```cmd
cd C:\path\to\secure-password-generator

docker compose up --build
```

- Frontend → http://localhost:3000
- Backend  → http://localhost:8080

---

## 📡 API Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| POST | `/api/v1/password/generate` | Generate password(s) |
| POST | `/api/v1/password/generate-with-hashes` | Generate with BCrypt + Argon2id |
| POST | `/api/v1/password/breach-check` | HIBP k-anonymity check |
| POST | `/api/v1/password/save-history` | Save to encrypted history |
| GET  | `/api/v1/password/export` | Export encrypted history |
| GET  | `/api/v1/password/health` | Health check |
| GET  | `/api/v1/history` | List history |
| GET  | `/api/v1/history/{id}/decrypt` | Decrypt entry |
| DELETE | `/api/v1/history/{id}` | Delete entry |

### Example request:
```bash
curl -X POST http://localhost:8080/api/v1/password/generate \
  -H "Content-Type: application/json" \
  -d '{
    "length": 20,
    "useUppercase": true,
    "useLowercase": true,
    "useNumbers": true,
    "useSpecialChars": true,
    "count": 1
  }'
```

---

## 🧪 Run Tests

```cmd
cd backend
mvn test
```

---

## 📁 Project Structure

```
secure-password-generator/
├── backend/
│   ├── src/main/java/com/securepwgen/
│   │   ├── SecurePasswordGeneratorApplication.java
│   │   ├── config/
│   │   │   ├── AppConfig.java          BCrypt + Argon2id + rate-limit filter
│   │   │   └── SecurityConfig.java     OWASP headers, CORS, stateless
│   │   ├── controller/
│   │   │   ├── PasswordController.java REST endpoints
│   │   │   └── HistoryController.java  History CRUD
│   │   ├── service/
│   │   │   ├── PasswordGeneratorService.java  SecureRandom CSPRNG + Fisher-Yates
│   │   │   ├── EntropyCalculatorService.java  Shannon entropy, crack time
│   │   │   ├── BreachCheckService.java        HIBP k-anonymity
│   │   │   └── HistoryService.java            AES-256-GCM history
│   │   ├── security/
│   │   │   ├── EncryptionService.java  AES-256-GCM (IV+tag per encrypt)
│   │   │   └── RateLimitFilter.java   30 req/min per IP (no external deps)
│   │   ├── repository/
│   │   │   └── PasswordHistoryRepository.java
│   │   ├── model/
│   │   │   ├── PasswordRequest.java
│   │   │   ├── PasswordResponse.java
│   │   │   └── PasswordHistory.java
│   │   └── exception/
│   │       ├── GlobalExceptionHandler.java
│   │       └── PasswordGenerationException.java
│   ├── src/main/resources/application.yml
│   ├── src/test/java/com/securepwgen/
│   │   ├── PasswordGeneratorServiceTest.java
│   │   └── SecurityServiceTest.java
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── App.jsx     Full UI (cyberpunk terminal aesthetic)
│   │   └── main.jsx
│   ├── index.html
│   ├── package.json
│   ├── vite.config.js  (proxy /api → localhost:8080)
│   ├── nginx.conf
│   └── Dockerfile
├── docker-compose.yml
└── README.md
```

---

## 🔒 Security Features

| Feature | Implementation |
|---------|---------------|
| Random number gen | `java.security.SecureRandom` (OS entropy) |
| Modulo bias fix | `SecureRandom.nextInt(bound)` rejection sampling |
| Shuffle | Fisher-Yates algorithm |
| History encryption | AES-256-GCM (authenticated, fresh IV per call) |
| Password hashing | Argon2id (m=64MB, t=3, p=4) + BCrypt(12) |
| Rate limiting | 30 req/min per IP, sliding window |
| Breach checking | HIBP k-anonymity (5-char SHA-1 prefix only) |
| Security headers | HSTS, CSP, X-Frame-Options, Referrer-Policy |
| Error handling | OWASP-safe (no stack traces to client) |

---

## ❗ Common Issues

**Port 8080 already in use:**
```cmd
netstat -ano | findstr :8080
taskkill /PID <pid> /F
```

**`mvn` not recognized:**
→ Install Maven and add to PATH. Restart terminal. See Prerequisites.

**Frontend can't connect to backend:**
→ Make sure backend is running on port 8080 first.
→ Check vite.config.js has proxy: `'/api' → 'http://localhost:8080'`
