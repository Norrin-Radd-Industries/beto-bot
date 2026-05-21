
# 🤖 Beto-Bot: Complete Orchestration, Configuration & Production Guide

This all-in-one document combines the comprehensive architecture overview, local development onboarding rules, and multi-environment infrastructure configurations into a single, cohesive reference manual.

---

## 🏛️ 1. Architecture & Execution Overview

Beto-Bot is an automated development lifecycle orchestrator built with **Spring Boot** and **Spring AI**. It leverages the **Model Context Protocol (MCP)** to provide autonomous, LLM-driven agents with real-time GitHub integration capabilities, transforming abstract issue backlogs into verified, deployed Pull Requests.

The system is designed to be highly non-blocking, scaling agent tasks horizontally across lightweight Java Virtual Threads while syncing continuous code state context using an integrated Vector RAG layer.

Unlike traditional LLM setups that overflow your context window with entire repositories, Beto-Bot uses a hybrid event-driven mechanism. It targets specific issues from a central board, queries a local vectorized codebase for hyper-focused context, and delegates actions to narrow, specialized agent personas.

### The Core Lifecycle Components
* **The Task Fetcher:** A scheduled background manager that polls GitHub Projects (V2) using specialized GraphQL queries, handles task dependency resolution, and screens out blocked issues.
* **The Orchestrator:** An event-driven application layer that consumes pending jobs and dynamically forks isolated agent runners inside lightweight **Java Virtual Threads**.
* **The RAG Synchronization Engine:** An automated vector alignment service linked to a live **GitHub Webhook Listener**, keeping an internal `pgvector` store up to date with code updates (`modified`, `removed`, `added`) in real-time.
* **The Agents:** Autonomous problem-solvers (Analyst and Coder) that interact with the live repository context using direct Model Context Protocol (MCP) toolkits.

### Pipeline Execution Flow
The workflow relies on tracking state columns inside your GitHub Project:

```
[GitHub Project V2] ──► (To Analyze Column) ──► [Fetcher] ──► [Analyst Agent]
                                                                   │
┌──────────────────────────────────────────────────────────────────┘
▼
(Analyzed Column) ──► {Optional Human Approval} ──► (To Todo / Develop Column)
                                                                   │
┌──────────────────────────────────────────────────────────────────┘
▼
[Fetcher] ──► [Coding Agent]  ──► [Creates Feature Branch] ──► [Pushes Fixes & PR]
                                                                   │
───────────────────────────────────────────────────────────────────┴──► [Move to Developed]
```

1. **The Analyst Persona:** A structural analyzer. It searches matching repository files, assesses dependency code trees, maps the code problem, and writes a concrete technical implementation proposal directly back into the GitHub issue before updating its status.
2. **The Coder Persona:** A senior Java engineering persona. It receives the architectural implementation plan, claims a new feature branch, updates code lines using safe file modifications via MCP, opens the Pull Request, links the original issue, and moves the task to completion.

---

## 🚀 2. Quickstart Prerequisites

Before booting the infrastructure via Docker or Kubernetes, you must configure your external dependencies. The application will fail to initialize if these steps are skipped.

### Step A: Provision your GitHub App
Instead of traditional personal access tokens, Beto-Bot leverages high-security GitHub Apps.
1. Navigate to your GitHub Organization (`Norrin-Radd-Industries`) **Settings > Developer Settings > GitHub Apps** and click **New GitHub App**.
2. Configure the following **Repository Permissions** explicitly:
   * **Repository Contents:** `Read & Write` (To parse codebase files and push automated branch fixes).
   * **Repository Issues:** `Read & Write` (To consume backlogs and write technical functional analyses).
   * **Pull Requests:** `Read & Write` (To compile code improvements and initialize automated PR reviews).
   * **Organization Projects:** `Read & Write` (To manipulate columns across the project board).
3. Scroll down to **Webhooks**, set your Webhook URL to `https://<your-domain>/webhooks/github`, and check the box to subscribe to **Push** events.
4. Save the application, note your generated **Client ID** and **Installation ID**, and generate a new **RSA Private Key**. Download the `.pem` file directly to your local workspace root directory.

### Step B: Build your GitHub Project Board (V2)
The task fetcher monitors a **Single Select** metadata field strictly named **Status**. The application evaluates column strings case-insensitively, routing pipeline actions using this target topology:

| Target Status Column | Routed Component | Resulting Automation Action |
| :--- | :--- | :--- |
| **`To analyze`** | `AnalystAgent` | Resolves code files, attaches functional analysis text to the issue, and moves the item to `Analyzed`. |
| **`To develop`** | `CodingAgent` | Generates a custom feature branch, implements code modifications via MCP, opens a PR, links the original issue, and moves the item to `Developed`. |

> ℹ️ **Task Dependency Gatekeeping:** The scheduler natively screens issue relationships. If an item contains references in its `blockedBy` array that remain in an `OPEN` state, processing is bypassed automatically to preserve development order.

---

## 📝 3. Local Environment Blueprint (`.env`)

Create a file named `.env` in your project root directory where your deployment configurations reside. Populate it with your active secrets matching this structure:

```env
# --- Core Security & LLM Reasoning Engine ---
GOOGLE_API_KEY=AIzaSyYourSecretGeminiStudioApiKeyHere

# --- GitHub Application Integration Tokens ---
GITHUB_CLIENT_ID=Iv1.YourAppClientIdHex
GITHUB_APP_INSTALL_ID=12345678
GITHUB_WEBHOOK_SECRET=your_configured_webhook_signing_secret

# --- Target Project Board Settings ---
GITHUB_ORG_OWNER=Norrin-Radd-Industries
GITHUB_PROJECT_NUMBER=1

# --- Vector Database Persistence Keys ---
SPRING_DATASOURCE_PASSWORD=your_secure_db_password

# --- LLM Model Mappings ---
AGENT_MODEL_ANALYST=models/gemini-2.5-pro
AGENT_MODEL_CODER=models/gemini-2.5-flash

# --- MCP Toolset Authentication (Temporary Loop) ---
# Note: The official GitHub MCP standalone server currently relies on a PAT for tool initialization
GITHUB_PERSONAL_ACCESS_TOKEN=ghp_yourClassicPersonalAccessTokenHere
```

---

## 🛠️ 4. Deployment Topologies & Diagnostics

### Option A: Local Docker Compose Deployment (With Key Mounting)

Save your private key `.pem` file as `github-app-private-key.pem` in your project root directory. The configuration below actively mounts this local key into the runtime container filesystem so the application can authorize its API tokens dynamically.

```yaml
services:
  beto-bot-db:
    image: pgvector/pgvector:pg16
    container_name: beto-bot-db
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_DB=beto_bot_db
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=${SPRING_DATASOURCE_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data

  github-mcp:
    image: ghcr.io/github/github-mcp-server:latest
    container_name: github-mcp
    ports:
      - "9090:8082"
    environment:
      - GITHUB_PERSONAL_ACCESS_TOKEN=${GITHUB_PERSONAL_ACCESS_TOKEN}
      - GITHUB_TOOLSETS=repos,issues,pull_requests,contents
    command: http

  beto-bot:
    image: ghcr.io/norrin-radd-industries/beto-bot:latest
    container_name: beto-bot
    depends_on:
      - beto-bot-db
      - github-mcp
    ports:
      - "9812:9812"
    volumes:
      # Mounts the private key file securely into the application container filesystem
      - ./github-app-private-key.pem:/app/secrets/github-app-private-key.pem
    environment:
      - GOOGLE_API_KEY=${GOOGLE_API_KEY}
      - GITHUB_CLIENT_ID=${GITHUB_CLIENT_ID}
      - GITHUB_APP_INSTALL_ID=${GITHUB_APP_INSTALL_ID}
      # Instructs Spring to read the key out of the container volume rather than the jar classpath
      - GITHUB_APP_KEY_PATH=file:/app/secrets/github-app-private-key.pem
      - GITHUB_WEBHOOK_SECRET=${GITHUB_WEBHOOK_SECRET}
      - GITHUB_ORG_OWNER=${GITHUB_ORG_OWNER}
      - GITHUB_PROJECT_NUMBER=${GITHUB_PROJECT_NUMBER}
      - SPRING_DATASOURCE_URL=jdbc:postgresql://beto-bot-db:5432/beto_bot_db
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD}
      - GITHUB_MCP_SERVER_ADDRESS=http://github-mcp:8082
      - AGENT_MODEL_ANALYST=${AGENT_MODEL_ANALYST}
      - AGENT_MODEL_CODER=${AGENT_MODEL_CODER}

volumes:
  pgdata:

```

To execute this stack, run:

```bash
docker compose --env-file .env up -d

```

### Option B: Enterprise Kubernetes Architecture (K3s)

Production environments map secrets securely into container execution rings using the Doppler Operator.

#### Step 1: Initialize Doppler Secret Store Injection

Inject your organization config authorization token directly into your cluster namespace:

```bash
kubectl create secret generic doppler-token-auth \
  --from-literal=serviceToken=dp.st.norrin_industries_token_here

```

#### Step 2: Target Mapping Specification Reference

Deploy your pods using our standardized **GHCR** image address space: `ghcr.io/norrin-radd-industries/beto-bot:latest`.

Ensure your production application deployment manifests (e.g., `beto-bot-deployment.yaml` and `postgres-vector-deployment.yaml`) reference these precise cluster networking endpoints:

* **Relational Database Cluster Location:** `jdbc:postgresql://beto-bot-db:5432/beto_bot_db`
* **Model Context Protocol Service Target:** `http://github-mcp-service:9090`

### 🔍 5. GraphQL Core Diagnostic Helper

If you ever need to inspect raw GraphQL metadata nodes or confirm individual single-select metadata token IDs directly via your console, execute this standard curl snippet:

```bash
curl --request POST \
  --url [https://api.github.com/graphql](https://api.github.com/graphql) \
  --header 'Authorization: Bearer YOUR_PERSONAL_ACCESS_TOKEN' \
  --data '{"query":"query { organization(login: \"Norrin-Radd-Industries\") { projectV2(number: 1) { id title field(name: \"Status\") { ... on ProjectV2SingleSelectField { id options { id name } } } } } }"}'

```
