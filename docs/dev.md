# Development & Configuration Guide

This guide provides the specific details needed to configure the **Beto-Bot** development environment, including GitHub permissions, project structures, and local secrets.

---

## 1. Environment Template (`.env example`)
If you're using K8s and Doppler, you need to paste these in there.
Otherwise, create a file named `.env` in your project root and populate it with your specific credentials. This file is used by both Docker Compose, you can also setup intelliJ to use it.

```bash
# --- Security & LLM ---
# Your Google AI Studio API Key
GOOGLE_API_KEY=your_gemini_api_key_here

# --- GitHub Integration ---
# Classic Personal Access Token (PAT)
GITHUB_PERSONAL_ACCESS_TOKEN=your_github_pat_here
# The GraphQL ID of your GitHub Project (not the number in the URL)
GITHUB_PROJECT_ID=PVT_kwDOBySampleID

# --- Agent Configuration ---
# Models used by the specialized agents ( you can choose here from models based on your provider)
AGENT_MODEL_ANALYST=gemini-3.1-pro-preview
AGENT_MODEL_CODER=gemini-3-flash-preview

# --- Networking ---
# URL for the MCP server (Use the K8s service name or Docker container name, or the github-remote server: https://api.githubcopilot.com/mcp/)
GITHUB_MCP_SERVER_ADDRESS=http://github-mcp:8082
```

---

## 2. GitHub Token Permissions (Classic)

To allow the agents to read issues, write code, and manage projects, your **Personal Access Token (Classic)** must have the following scopes selected:

* **`repo`**: Full control of private repositories (Required for `push_files` and `create_pull_request`).
* **`read:user`**: Required for the bot to identify itself and repo owners.
* **`project`**: Full control of user and organization projects (Required to move tasks between columns).

> **Tip:** If you are working within an Organization, ensure you grant the token SSO authorization if required by your org's security policy.

---

## 3. GitHub Project Layout

Beto-Bot uses a **GitHub Project (V2)** as its task source. The orchestrator and fetcher expect a specific column (Status) layout to move items through the agentic pipeline.



### Required Columns
Your project must have a **Single Select** field named **"Status"** with the following options:

1.  **Backlog / Todo**: The **Fetcher** scans these columns for new issues.
2.  **Analyzed**: The **Analyst Agent** moves the issue here after appending its technical analysis to the description.
3.  **In Progress**: The **Coding Agent** moves the issue here once it has created a branch and submitted a Pull Request.
4.  **Done**: Manual or automated column for completed work.

### Task Routing
* Items in **Backlog/Todo** are picked up and assigned a type (ANALYSIS or CODER) based on their current state.
* The **Orchestrator** triggers the virtual threads for the corresponding agent based on these column placements.

---

## 4. Useful GraphQL Snippet

To find your `GITHUB_PROJECT_ID` (the string starting with `PVT_`), run this in your terminal:

```bash
curl --request POST \
  --url https://api.github.com/graphql \
  --header 'Authorization: Bearer YOUR_TOKEN' \
  --data '{"query":"query{user(login: \\"YOUR_USERNAME\\") {projectV2(number: YOUR_PROJECT_NUMBER){id title}}}"}'
```