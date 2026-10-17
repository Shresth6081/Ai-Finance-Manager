# Finance Manager Agent

[![Live Demo](https://img.shields.io/badge/Live_Demo-Render-46E3B7?style=for-the-badge&logo=render&logoColor=white)](https://ai-finance-manager-1-hipp.onrender.com/)
[![Backend](https://img.shields.io/badge/Spring_Boot-3.3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Frontend](https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![LLM Inference](https://img.shields.io/badge/Groq_Cloud-Llama_3.3_70B-F55036?style=for-the-badge&logo=fastapi&logoColor=white)](https://groq.com/)

> 🚀 **Live Demo:** [https://ai-finance-manager-1-hipp.onrender.com/](https://ai-finance-manager-1-hipp.onrender.com/)

**Finance Manager Agent** (FinAgent) is an autonomous, full-stack personal finance management platform powered by **Agentic AI** and **Spring AI**. Rather than relying on simple, stateless prompt-and-answer bots that hallucinate numbers, FinAgent implements an autonomous **ReAct (Reasoning + Acting) Agent** that reasons iteratively over live database records, calls backend analytical tools, streams thoughts and tokens in real time via **Server-Sent Events (SSE)**, and enforces strict **Human-in-the-Loop (HITL)** governance for sensitive database modifications.

---

## 🌐 Live Deployment Overview

* **Frontend:** Hosted on **Render Static Sites** ([https://ai-finance-manager-1-hipp.onrender.com/](https://ai-finance-manager-1-hipp.onrender.com/))
* **Backend API:** Hosted on **Render Web Services** with custom Docker container + Tesseract OCR engine
* **Database:** Managed Cloud MySQL via **Aiven**
* **Cloud AI Inference:** Ultra-low latency inference via **Groq Cloud LPU** (`llama-3.3-70b-versatile` / Spring AI)

---

## 🤖 Core Agentic AI Capabilities

### 1. Autonomous ReAct Reasoning Engine
FinAgent executes an iterative **Reasoning + Acting** loop (up to 4 iterations) with strict protocol tags:
* `THOUGHT:` Internal step-by-step reasoning explaining what data is required.
* `ACTION:` Explicit tool invocation selecting from 5 live database tools.
* `ACTION_INPUT:` JSON parameters passed to the tool.
* `OBSERVATION:` Actual SQL data and financial calculations returned from the database.
* `FINAL_ANSWER:` Complete markdown-formatted financial advice, tables, and metrics.

### 2. Live Database Financial Tools
The agent has direct access to 5 backend analytical tools:
1. **`getMonthlySpendingByCategory`**: Calculates exact category spending totals, income vs. expenses, and net savings rate for any month/year.
2. **`getHistoricalTrends`**: Computes month-over-month income, expense, and savings trends over recent months.
3. **`getRecurringTransactions`**: Identifies subscription charges, recurring vendors, and flags price hikes (e.g., Netflix price increases).
4. **`getBudgetVsActualReport`**: Evaluates user spending against standard 50/30/20 budget benchmarks (Needs, Wants, Savings).
5. **`simulateSavingsTrajectory`**: Projects compound interest milestones and wealth trajectory based on current balance, deposits, and target timeline.

### 3. Real-Time Server-Sent Events (SSE) Streaming
Instead of freezing the UI during multi-step reasoning:
* Custom `ReadableStream` and `TextDecoder` pipeline in React.
* Streams live reasoning steps into a collapsible **Thought Trace Accordion**.
* Real-time tool badges display which tool is running and when it completes.
* Typewriter text animation streams tokens smoothly at 15ms intervals.

### 4. Human-in-the-Loop (HITL) Action Governance
To prevent unauthorized or hallucinated writes:
* The agent is **strictly read-only** during autonomous loops.
* If the user asks to add an expense, bulk re-categorize, or adjust a budget, the agent outputs a `PROPOSAL: {...}`.
* The frontend renders an interactive **Action Proposal Card** with **Approve** and **Dismiss** buttons.
* Database mutations (`CREATE_TRANSACTION`, `BULK_RECATEGORIZE`, `CREATE_CATEGORY`, `REALLOCATE_BUDGET`) only execute inside `@Transactional` blocks upon the user's explicit approval click.

### 5. Proactive Anomaly Hunter Agent (`ScheduledAgentHunterService`)
* An autonomous background service runs scheduled scans across user transactions.
* Detects vendor price increases and spending spikes without user prompting.
* Generates proactive `AgentAlert` records that display in alert banners across the dashboard.

### 6. AI Receipt OCR Scanner
* Upload receipt images (PNG, JPG, JPEG).
* The backend runs **Tesseract OCR** via Tess4J to extract raw text.
* Spring AI parses unstructured text into structured transaction fields (merchant, amount, date, category) automatically.

---

## 🛠️ Tech Stack

### Backend
* **Language & Framework:** Java 17, Spring Boot 3.3.4
* **AI Orchestration:** Spring AI 1.0.0-M1, Groq Cloud API (`llama-3.3-70b-versatile`)
* **Streaming Protocol:** W3C Server-Sent Events (`SseEmitter`, `MediaType.TEXT_EVENT_STREAM_VALUE`)
* **Security:** Spring Security, JWT (JSON Web Tokens)
* **Data Access & ORM:** Spring Data JPA, Hibernate, MySQL 8.x / Aiven Cloud / TiDB
* **OCR Engine:** Tesseract OCR (Tess4J JNA Native Wrapper)
* **Monitoring:** Spring Boot Actuator

### Frontend
* **Framework & Bundler:** React 18, Vite
* **Styling:** Tailwind CSS, PostCSS, Lucide React Icons
* **Data Visualization:** Chart.js, React-Chartjs-2
* **Routing & State:** React Router DOM, React Context API (`AuthContext`)
* **Streaming Client:** Fetch API `ReadableStream` + `TextDecoder` (SSE reader)

---

## 📁 Project Structure

```text
finance_manager_final/
├── backend/                                  # Spring Boot Application
│   ├── src/main/java/com/financemanager/
│   │   ├── config/                           # Security, JWT, Tool Beans & Seeder
│   │   │   ├── AgentToolsConfig.java         # Spring AI Function Beans (5 tools)
│   │   │   ├── DataSeeder.java               # Automatic default categories & demo ledger
│   │   │   ├── DataSourceConfig.java         # Cloud DB URI parsing & SSL configuration
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── JwtService.java
│   │   │   └── SecurityConfig.java
│   │   ├── controller/                       # REST & Streaming Endpoints
│   │   │   ├── AgentController.java          # SSE /stream, /chat, /execute-action, /alerts
│   │   │   ├── AuthenticationController.java # Auth & registration
│   │   │   ├── CategoryController.java       # Category CRUD
│   │   │   ├── ReceiptController.java        # OCR scanning & AI receipt parsing
│   │   │   └── TransactionController.java   # Transaction CRUD & async categorization
│   │   ├── dto/                              # Transfer Objects & Event Payloads
│   │   │   ├── AgentActionProposal.java      # HITL action proposal schema
│   │   │   ├── AgentAlertDTO.java            # Proactive alert payload
│   │   │   ├── AgentChatRequest.java         # User chat prompt & history
│   │   │   └── AgentStreamEvent.java         # SSE event model (thought, tool_call, token, etc.)
│   │   ├── model/                            # JPA Entities (MySQL)
│   │   │   ├── AgentAlert.java               # Anomaly alert entity
│   │   │   ├── Category.java                 # User categories
│   │   │   ├── Transaction.java              # Ledger entries
│   │   │   └── User.java                     # User credentials & Spring Security principal
│   │   ├── repository/                       # Spring Data JPA Repositories
│   │   │   ├── AgentAlertRepository.java
│   │   │   ├── CategoryRepository.java
│   │   │   ├── TransactionRepository.java
│   │   │   └── UserRepository.java
│   │   └── service/                          # Business Logic & Agent Execution
│   │       ├── agent/
│   │       │   ├── AgentOrchestratorService.java   # ReAct reasoning loop & prompt compiler
│   │       │   ├── FinancialToolService.java       # 5 DB tools & transactional action runner
│   │       │   └── ScheduledAgentHunterService.java # Background anomaly hunter cron
│   │       ├── AIService.java                # Fast transaction categorization
│   │       ├── AuthenticationService.java
│   │       ├── CategoryService.java
│   │       └── TransactionService.java
│   └── src/main/resources/
│       └── application.properties            # Groq API configuration & DB properties
│
├── frontend/                                 # React + Vite Application
│   ├── src/
│   │   ├── components/layout/
│   │   │   ├── Layout.jsx                    # Main shell container
│   │   │   └── Sidebar.jsx                   # Collapsible navigation sidebar
│   │   ├── context/
│   │   │   └── AuthContext.jsx               # JWT state & persistent auth session
│   │   ├── pages/
│   │   │   ├── AIChat.jsx                    # ReAct SSE Chat, live thoughts & Action Cards
│   │   │   ├── Categories.jsx                # Category manager & transaction viewer
│   │   │   ├── Dashboard.jsx                 # KPI summaries, hunter alert banner & Chart.js
│   │   │   ├── Login.jsx
│   │   │   ├── Register.jsx
│   │   │   └── Transactions.jsx              # Ledger view, filter, & OCR Scan modal
│   │   ├── services/
│   │   │   └── api.js                        # Axios instance with JWT interceptors
│   │   ├── App.jsx                           # Route configuration
│   │   └── main.jsx
│   ├── tailwind.config.js
│   └── vercel.json                           # SPA client-side rewrite rules
```

---

## ⚙️ Environment Configuration

Ensure the following variables are configured in your local environment or production host (e.g. Render / Docker):

| Variable | Description | Example |
| :--- | :--- | :--- |
| `DB_URL` | JDBC MySQL connection URL | `jdbc:mysql://localhost:3306/financemanager?createDatabaseIfNotExist=true` |
| `DB_USERNAME` | Database username | `root` |
| `DB_PASSWORD` | Database password | `your_secret_password` |
| `SECRET_KEY` | HMAC SHA-256 JWT secret key | `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970` |
| `GROK_API_KEY` | Groq Cloud API Key | `gsk_...` |

---

## 🚀 Local Development Setup

### Prerequisites
* **Java 17+**
* **Node.js (v18+)** & **npm**
* **MySQL Server**
* **Tesseract OCR** binary package installed:
  * Windows: [UB Mannheim Tesseract Installer](https://github.com/UB-Mannheim/tesseract/wiki) (default directory: `C:\Program Files\Tesseract-OCR`)
  * Ubuntu / Debian: `sudo apt-get install tesseract-ocr`

### 1. Database Initialization
Start your MySQL server and create the database:
```sql
CREATE DATABASE financemanager;
```

### 2. Backend Setup
```bash
cd backend
# Set your environment variables (or update application.properties for local testing)
./mvnw spring-boot:run
```
The API server will launch at `http://localhost:8085`.

### 3. Frontend Setup
```bash
cd ../frontend
npm install
npm run dev
```
Open your browser at `http://localhost:5173`.
