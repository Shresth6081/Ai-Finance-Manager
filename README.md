# AI Finance Manager

[![Live Demo](https://img.shields.io/badge/Live_Demo-Render-46E3B7?style=for-the-badge&logo=render&logoColor=white)](https://ai-finance-manager-1-hipp.onrender.com/)
[![Backend](https://img.shields.io/badge/Spring_Boot-3.3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Frontend](https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)

> 🚀 **Live Application:** [https://ai-finance-manager-1-hipp.onrender.com/](https://ai-finance-manager-1-hipp.onrender.com/)

AI Finance Manager is a full-stack personal finance management application. It features a robust **Spring Boot** backend API and a highly responsive **React** frontend styled with **Tailwind CSS**. The platform securely tracks transactions, structures customized budget categories, offers smart OCR receipt scanning, and features a dedicated financial assistant chat powered by **Spring AI & Ollama** for automated transaction categorization and contextual advice.

---

## Key Features

* **Dual-Engine Architecture:** Clean separation of client UI (`frontend`) and business logic API (`backend`) hosted within a synchronized monorepo.
* **Intelligent Asynchronous Categorization:** Transactions save instantly in a pulsing `"Pending"` state, dispatching categorization tasks to an asynchronous background worker thread so the UI never freezes. The frontend reactively polls the server until Ollama finishes.
* **Dedicated AI Chat Assistant:** Accessible from the sidebar, the assistant uses Spring AI to retrieve contextual prompt logs (feeding the most recent 15 transactions) to serve customized advice, budget analysis, and general chat.
* **AI Receipt OCR Scanner:** Users can click **Scan Receipt** and upload receipt photos (PNG, JPG, JPEG). The backend runs Tesseract OCR locally to extract raw text, and Spring AI maps the unstructured text into a clean transaction form (description, amount, date, and suggested category) automatically.
* **Stateful Security & RBAC:** Complete authentication flow secured by **JSON Web Tokens (JWT)**, featuring route guards on the frontend and method-level access controls on the backend.
* **Actuator Monitoring & Fail-Fast Health Checks:** Native monitoring via **Spring Boot Actuator** combined with a custom Ollama health check circuit breaker that blocks slow LLM requests instantly if the model server is offline.
* **Relational Schema Mapping:** Optimized MySQL database integration utilizing Hibernate/JPA object-relational mapping with automatic schema generation (`update`).

---

## Tech Stack

### Backend
* **Language/Framework:** Java 17, Spring Boot 3.3.x
* **Security:** Spring Security, JWT (Json Web Token)
* **Data Access:** Spring Data JPA, Hibernate
* **Database:** MySQL 8.x
* **AI Orchestration:** Spring AI (Ollama Starter Integration)
* **OCR Integration:** Tesseract OCR (Tess4J JNA Wrapper)
* **Observability:** Spring Boot Actuator

### Frontend
* **Framework/Bundler:** React.js, Vite
* **Styling:** Tailwind CSS, PostCSS
* **Routing/State:** React Router DOM, React Context API (Auth Provider)
* **Visualization:** ChartJS

---

## Project Structure

```text
finance_manager_final/
├── backend/                                  # Spring Boot Maven Project
│   ├── src/main/java/com/financemanager/
│   │   ├── config/                           # Security, JWT & Web Routing Configuration
│   │   │   ├── ApplicationConfig.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── JwtService.java
│   │   │   ├── SecurityConfig.java
│   │   │   └── OllamaHealthIndicator.java    # Custom Actuator endpoint checking Ollama status
│   │   ├── controller/                       # REST Endpoints (Communicates with React frontend)
│   │   │   ├── AIController.java             # Handles prompt chat prompts with transaction context
│   │   │   ├── AuthenticationController.java  # Handles login and registration routing
│   │   │   ├── CategoryController.java
│   │   │   ├── ReceiptController.java        # Handles receipt upload & Tesseract OCR scanning
│   │   │   └── TransactionController.java
│   │   ├── dto/                              # Data Transfer Objects
│   │   │   ├── AuthenticationRequest.java
│   │   │   ├── AuthenticationResponse.java
│   │   │   ├── CategoryDTO.java
│   │   │   ├── RegisterRequest.java
│   │   │   └── TransactionDTO.java
│   │   ├── model/                            # Database Entities (Maps directly to MySQL tables)
│   │   │   ├── Category.java
│   │   │   ├── Transaction.java
│   │   │   ├── TransactionType.java          # Enum class tracking INCOME / EXPENSE variations
│   │   │   └── User.java                     # Implements UserDetails for system authentication
│   │   ├── repository/                       # Database Queries via Spring Data JPA
│   │   │   ├── CategoryRepository.java
│   │   │   └── TransactionRepository.java
│   │   ├── service/                          # Business Logic Layer (Interacts with database/LLM)
│   │   │   ├── AiService.java                # Spring AI & async transaction categorization
│   │   │   ├── AuthenticationService.java    
│   │   │   ├── CategoryService.java
│   │   │   └── TransactionService.java
│   │   └── FinanceManagerApplication.java    # The Main Spring Boot Entry Point
│   └── src/main/resources/
│       └── application.properties            # Contains database configurations & credentials
│
├── frontend/                                 # React Vite Project
│   ├── src/
│   │   ├── components/                       # Shared layout systems
│   │   │   ├── layout/
│   │   │   │   ├── Layout.jsx                # Main interface container wrapper
│   │   │   │   └── Sidebar.jsx               # Left navigation bar layout (AI assistant workspace link)
│   │   ├── context/                          # State management providers
│   │   │   └── AuthContext.jsx               # Tracks user authentication state and JWT storage
│   │   ├── pages/                            # Full-view UI components (Routes)
│   │   │   ├── AIChat.jsx                    # Dedicated full-screen AI chat workspace
│   │   │   ├── Categories.jsx
│   │   │   ├── Dashboard.jsx                 # Financial chart dashboards (Chart.js implementation)
│   │   │   ├── Login.jsx
│   │   │   ├── Register.jsx
│   │   │   └── Transactions.jsx              # Ledger view with smart polling & Scan button
│   │   ├── services/                         # API communication layers
│   │   │   └── api.js                        # Axios wrappers for endpoint fetches
│   │   ├── App.css
│   │   ├── App.jsx                           # Core Router initialization mapping
│   │   ├── index.css                         # Application-wide global styling rules (Tailwind imports)
│   │   └── main.jsx                          # Frontend baseline mount point
│   └── tailwind.config.js                    # Design Tokens & Layout Utility definitions
```

---

## ⚙️ Environment Configuration

To keep production environments secure, database credentials and security signatures are decoupled from the codebase using system environment variables.

### Required Environment Variables

Ensure the following variables are configured on your hosting machine or local terminal environment before launching the application:

* **DB_USERNAME:** Database administrative username (e.g., root)
* **DB_PASSWORD:** Database administrative password
* **SECRET_KEY:** Cryptographic hash used for signing and validating JWTs

---

## Getting Started (Local Setup)

### Prerequisites

* Java Development Kit (JDK) 17 or higher
* Node.js (v18+) & npm
* MySQL Server
* **Ollama** installed locally with the active model pulled (e.g. `llama3` or `qwen2:1.5b`)
* **Tesseract OCR** binary package installed on the system (default Windows directory: `C:\Program Files\Tesseract-OCR`)

### 1. Tesseract Installation (Windows)
1. Download the Windows binary installer from [UB Mannheim Tesseract OCR](https://github.com/UB-Mannheim/tesseract/wiki).
2. Install to the default directory (`C:\Program Files\Tesseract-OCR`).
3. Ensure that the `tessdata` folder contains `eng.traineddata`.

### 2. Database Setup
Ensure your MySQL server is active, then create the database:
```sql
CREATE DATABASE financemanager;
```

### 3. Backend Initialization
Navigate to the backend folder:
```bash
cd backend
```
Configure your environment variables, then run the Spring Boot application using Maven:
```bash
./mvnw spring-boot:run
```
The backend will start hosting endpoints at `http://localhost:8085`.

### 4. Frontend Initialization
Navigate to the frontend folder:
```bash
cd ../frontend
```
Install the necessary node modules and dependencies:
```bash
npm install
```
Boot up the Vite local development server:
```bash
npm run dev
```
Open your browser and navigate to the local development URL (typically `http://localhost:5173`).

