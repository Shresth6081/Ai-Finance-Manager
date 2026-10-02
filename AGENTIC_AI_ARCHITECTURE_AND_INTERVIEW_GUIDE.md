# Agentic AI Architecture & Interview Preparation Guide

This document breaks down the entire transition of the **AI Finance Manager** from a standard LLM wrapper into a production-grade **Agentic AI Financial System**. Use this guide to understand the engineering design, architecture patterns, and how to articulate these concepts clearly in technical interviews.

---

## 1. The 30-Second Interview Elevator Pitch

> *"Most AI finance apps are just passive chatbots that stuff raw transaction dumps into a prompt and ask an LLM to generate generic advice. In this upgrade, I re-architected the system into an **autonomous Agentic Financial Assistant (FinAgent)** using the **ReAct (Reason + Act)** pattern and **Spring AI**.*
> 
> *Instead of hallucinating numbers, FinAgent has access to a dedicated **Java Tool Registry** to run precise database aggregations, detect recurring subscription price hikes, and run wealth forecasts.*
> 
> *For real-time responsiveness, I implemented a **Server-Sent Events (SSE)** streaming pipeline that displays the agent’s live 'thought trace' and tool executions. For safety, mutations require explicit **Human-in-the-Loop** confirmation via interactive approval cards. Finally, a background **`@Scheduled` Hunter Agent** runs autonomously to proactively alert users of subscription creep and budget spikes without requiring user prompts."*

---

## 2. Core Architecture & Mental Model

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           React Frontend UI                             │
│  • Collapsible Thought Trace Accordion                                  │
│  • Interactive Action Approval Cards (Approve / Dismiss)                │
│  • SSE Stream Listener (Real-time Token & Event Parsing)                │
│  • Autonomous Hunter Alerts Banner (Dashboard)                          │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │ HTTP SSE Stream (GET /stream)
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                 Spring Boot Controller & Security Layer                 │
│  • AgentController (SseEmitter with 180s async lifecycle)               │
│  • SecurityConfig (Allows DispatcherType.ASYNC + JWT Query Param)       │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│               Agent Orchestrator Engine (ReAct Reasoning Loop)           │
│  • Multi-step loop: THOUGHT -> ACTION -> OBSERVATION -> FINAL_ANSWER    │
│  • Balanced JSON Depth Parser for complex nested payloads               │
│  • Event Dispatcher (thought, tool_call, token, action_required, done)  │
└──────────────┬───────────────────────────────────────────┬──────────────┘
               │ Tool Call Plan                            │ Tool Result Data
               ▼                                           ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     Spring AI Java Tool Registry                        │
│                                                                         │
│  [Query & Analytical Tools]         [Mutation & Execution Tools]         │
│  • getMonthlySpendingByCategory     • executeReallocateBudget           │
│  • getHistoricalTrends              • executeBulkRecategorize           │
│  • getRecurringTransactions         • executeCreateTransaction          │
│  • getBudgetVsActualReport          • executeCreateCategory             │
│                                                                         │
│  [Math & Forecasting Tools]         [Autonomous Scheduled Hunter]       │
│  • simulateSavingsTrajectory        • ScheduledAgentHunterService       │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │ User-Scoped JPA Queries
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     Spring Data JPA & MySQL Database                    │
│  • transactions | categories | users | agent_alerts                     │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Detailed Breakdown of All Changes Made

### **Backend Components**

| File | Purpose & Engineering Responsibility |
| :--- | :--- |
| [`model/AgentAlert.java`](file:///c:/Users/Shres/OneDrive/Documents/Full_Stack_Projects/finance_manager_final/backend/src/main/java/com/financemanager/model/AgentAlert.java) | Entity storing autonomous background discoveries (`PRICE_HIKE`, `SAVINGS_OPPORTUNITY`, `BUDGET_OVERRUN`) with suggested action JSON. |
| [`repository/AgentAlertRepository.java`](file:///c:/Users/Shres/OneDrive/Documents/Full_Stack_Projects/finance_manager_final/backend/src/main/java/com/financemanager/repository/AgentAlertRepository.java) | Indexed repository queries for active, non-dismissed alerts by `userId`. |
| [`dto/AgentActionProposal.java`](file:///c:/Users/Shres/OneDrive/Documents/Full_Stack_Projects/finance_manager_final/backend/src/main/java/com/financemanager/dto/AgentActionProposal.java) | Structured payload representing proposed database changes that require human approval before execution. |
| [`dto/AgentStreamEvent.java`](file:///c:/Users/Shres/OneDrive/Documents/Full_Stack_Projects/finance_manager_final/backend/src/main/java/com/financemanager/dto/AgentStreamEvent.java) | Envelope for SSE streaming events (`thought`, `tool_call`, `tool_result`, `token`, `action_required`, `done`, `error`). |
| [`service/agent/FinancialToolService.java`](file:///c:/Users/Shres/OneDrive/Documents/Full_Stack_Projects/finance_manager_final/backend/src/main/java/com/financemanager/service/agent/FinancialToolService.java) | **The Tool Execution Engine**: Enforces strict tenancy (`userId` verification) and computes category spend, historical trends, recurring subscriptions, 50/30/20 benchmarks, and compound wealth projections. |
| [`config/AgentToolsConfig.java`](file:///c:/Users/Shres/OneDrive/Documents/Full_Stack_Projects/finance_manager_final/backend/src/main/java/com/financemanager/config/AgentToolsConfig.java) | Registers Spring AI Function `@Bean` callbacks with JSON schema descriptions for LLM function calling. |
| [`service/agent/AgentOrchestratorService.java`](file:///c:/Users/Shres/OneDrive/Documents/Full_Stack_Projects/finance_manager_final/backend/src/main/java/com/financemanager/service/agent/AgentOrchestratorService.java) | **The ReAct Orchestration Engine**: Runs the multi-turn reasoning loop, executes tools dynamically based on LLM outputs, parses nested proposal JSON using a balanced-brace parser, and emits real-time SSE events. |
| [`service/agent/ScheduledAgentHunterService.java`](file:///c:/Users/Shres/OneDrive/Documents/Full_Stack_Projects/finance_manager_final/backend/src/main/java/com/financemanager/service/agent/ScheduledAgentHunterService.java) | Background `@Scheduled` worker that scans user transaction history for vendor price increases and savings opportunities without user prompting. |
| [`controller/AgentController.java`](file:///c:/Users/Shres/OneDrive/Documents/Full_Stack_Projects/finance_manager_final/backend/src/main/java/com/financemanager/controller/AgentController.java) | Exposes `GET /api/v1/ai/agent/stream` (SSE), `POST /execute-action`, and alert dismiss endpoints. |
| [`config/SecurityConfig.java`](file:///c:/Users/Shres/OneDrive/Documents/Full_Stack_Projects/finance_manager_final/backend/src/main/java/com/financemanager/config/SecurityConfig.java) | Updated with `.dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.FORWARD).permitAll()` so asynchronous SSE completions do not trigger `AccessDeniedException`. |
| [`config/JwtAuthenticationFilter.java`](file:///c:/Users/Shres/OneDrive/Documents/Full_Stack_Projects/finance_manager_final/backend/src/main/java/com/financemanager/config/JwtAuthenticationFilter.java) | Supports extracting JWT tokens from query parameters (`?token=...`) for SSE connections and catches expired token exceptions gracefully. |
| [`config/DataSeeder.java`](file:///c:/Users/Shres/OneDrive/Documents/Full_Stack_Projects/finance_manager_final/backend/src/main/java/com/financemanager/config/DataSeeder.java) | Automated idempotent seeder providing test user `shresth` (`1234`) with 3 months of realistic financial transactions (including an engineered Netflix price hike). |

### **Frontend Components**

| File | Upgrades Made |
| :--- | :--- |
| [`pages/AIChat.jsx`](file:///c:/Users/Shres/OneDrive/Documents/Full_Stack_Projects/finance_manager_final/frontend/src/pages/AIChat.jsx) | Replaced static REST call with an **SSE Stream Reader**, added the **Collapsible Thought Trace Accordion** (`⚡ Agent Reasoning & Tools`), and rendered **Interactive Action Approval Cards** with real-time `Approve & Execute` / `Dismiss` lifecycle states. |
| [`pages/Dashboard.jsx`](file:///c:/Users/Shres/OneDrive/Documents/Full_Stack_Projects/finance_manager_final/frontend/src/pages/Dashboard.jsx) | Integrated the **Autonomous Hunter Insights Banner** at the top of the dashboard displaying active alerts with 1-click dismiss and direct chat links. |

---

## 4. Top Technical Interview Questions & Answers

### **Q1: Why move to an Agentic ReAct pattern instead of standard prompt engineering?**
> **Answer:** *"Standard prompt engineering is single-shot and read-only. In personal finance, you cannot dump all years of user transactions into a context window without hitting context limits, paying high token costs, or causing LLM hallucinations.
> 
> With the ReAct pattern, the LLM acts as a reasoning engine that breaks user requests into explicit steps: it asks for specific data using structured tools (e.g. querying only August 2026 transactions), observes the tool's structured JSON return, and reasons about the next step before producing an answer. This guarantees 100% mathematical accuracy grounded in the database."*

### **Q2: How do you handle safety with AI making database modifications?**
> **Answer:** *"I separated operations into **Query Tools (Safe)** and **Mutation Actions (Destructive)**.
> 
> When the agent wants to modify data (e.g. create a transaction, reclassify categories, or shift budgets), the Orchestrator does not execute it immediately. Instead, it emits an `action_required` event containing an `AgentActionProposal` payload. The React frontend renders an interactive **Action Approval Card**. The database mutation only executes when the human explicitly clicks `Approve & Execute`, hitting a secure `POST /execute-action` endpoint."*

### **Q3: Why Server-Sent Events (SSE) instead of WebSockets or HTTP Polling?**
> **Answer:** *"Because agentic reasoning is unidirectional (server streaming events and tokens down to the client over an ongoing task), **SSE** is simpler, lighter, and more robust than WebSockets. It operates over standard HTTP, natively handles reconnection, requires no complex WebSocket handshake/duplex protocol, and allows us to stream distinct event types (`thought`, `tool_call`, `token`, `action_required`) seamlessly into the React UI."*

### **Q4: How did you solve Spring Security 6 async dispatch issues with SSE?**
> **Answer:** *"In Spring Boot 3 / Spring Security 6, when an `SseEmitter` completes asynchronously, Tomcat performs an internal `ASYNC` dispatch to clean up the request. By default, `AuthorizationFilter` intercepts this second dispatch and throws an `AccessDeniedException` on an already-committed response.
> 
> I resolved this by adding `.dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.FORWARD).permitAll()` in `SecurityConfig.java` and resolving user authentication before spawning async worker threads."*

### **Q5: How do you ensure multi-tenant data isolation in tools?**
> **Answer:** *"The LLM is never trusted to supply raw `userId`s in a way that could access another user's data. In `FinancialToolService`, all query methods accept the `userId` resolved directly from the authenticated `SecurityContextHolder` (via validated JWT claims). Even if a prompt attempts prompt-injection, the JPA repository queries are strictly bound to the authenticated user's ID."*

---

## 5. Step-by-Step Study Checklist to Master the Codebase

1. **Step 1: Inspect the ReAct Loop**
   - Open [`AgentOrchestratorService.java`](file:///c:/Users/Shres/OneDrive/Documents/Full_Stack_Projects/finance_manager_final/backend/src/main/java/com/financemanager/service/agent/AgentOrchestratorService.java) and read `executeReActLoop(...)`. Notice how it loops up to 4 iterations, matching `THOUGHT:`, `ACTION:`, and `FINAL_ANSWER:`.
2. **Step 2: Understand the Tool Registry**
   - Open [`FinancialToolService.java`](file:///c:/Users/Shres/OneDrive/Documents/Full_Stack_Projects/finance_manager_final/backend/src/main/java/com/financemanager/service/agent/FinancialToolService.java) and see how `getMonthlySpendingByCategory`, `getRecurringTransactions`, and `simulateSavingsTrajectory` calculate exact metrics.
3. **Step 3: Review the SSE Event Emitter**
   - Open [`AgentController.java`](file:///c:/Users/Shres/OneDrive/Documents/Full_Stack_Projects/finance_manager_final/backend/src/main/java/com/financemanager/controller/AgentController.java) to see how `SseEmitter` streams structured `AgentStreamEvent`s.
4. **Step 4: Understand the Frontend React Stream Parser**
   - Open [`AIChat.jsx`](file:///c:/Users/Shres/OneDrive/Documents/Full_Stack_Projects/finance_manager_final/frontend/src/pages/AIChat.jsx) and inspect lines 70–160 to see how `fetch` with `reader.read()` parses SSE chunks, updates the live thought accordion, and handles Action Approval Cards.
5. **Step 5: Review the Autonomous Hunter**
   - Open [`ScheduledAgentHunterService.java`](file:///c:/Users/Shres/OneDrive/Documents/Full_Stack_Projects/finance_manager_final/backend/src/main/java/com/financemanager/service/agent/ScheduledAgentHunterService.java) to understand how background `@Scheduled` cron jobs scan for anomalies without human initiation.

---

*(This document is stored at [`AGENTIC_AI_ARCHITECTURE_AND_INTERVIEW_GUIDE.md`](file:///c:/Users/Shres/OneDrive/Documents/Full_Stack_Projects/finance_manager_final/AGENTIC_AI_ARCHITECTURE_AND_INTERVIEW_GUIDE.md) in your project root for easy reference).*
