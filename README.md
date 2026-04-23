# 🤖 Beto-Bot: Custom Java Multi-Agent Orchestrator
Beto-Bot is an orchestration system built with Spring Boot and Spring AI. It leverages the Model Context Protocol (MCP) to provide LLM agents with a "toolbox" of GitHub capabilities, transforming abstract issues into completed pull requests.

I started this project to learn more about MCP and Agentic flows. Along the way, Beto-Bot came to life.
A detailed feature in steps can also be found on my blog: [umbertorighetti.dev](https://umbertorighetti.dev)

For detailed setup instructions, see the [Infrastructure Guide](./docs/infra.md).

## 🏛️ What is Beto-Bot?
The core mission of Beto-Bot is to automate the software development lifecycle by coordinating specialized AI agents. Instead of a single model trying to do everything, the system splits work between focused personas that interact with your GitHub projects in real-time.

### The Core Components
**The Fetcher:** A scheduled service that monitors your GitHub Projects for new work using GraphQL.

**The Orchestrator:** A non-blocking event listener that assigns tasks to the appropriate agent using Java virtual threads.

**LLM:** Personas (Analyst and Coder) powered by Gemini that reason through tasks and call tools.

**Streamable HTTP MCP Client:** To create the connection to the Github mcp server, either remote or local.

**Github MCP Server:** Github's MCP server with tools for anything github related more here: https://github.com/github/github-mcp-server?tab=readme-ov-file#tools

## 🧑‍💻 How It Works
Beto-Bot takes a hybrid approach. Instead of handing everything off to agents, increasing the token input, we fetch issues and information
from a Github Project on a scheduled basis, inject those issues into designated, fine-tuned prompts and **only then** hand them over to agents.
Github project acts as our frontend where we can create new tasks (issues) that will be picked up automatically.

The full flow is :

```java
		[project]-[issue in backlog column]-[fetcher]-[analyst]-[finishes analysis, moves issue to analyzed column]
		{human reviews analysis}-{move to Todo column}
		[project]-[issue in Todo column]-[fetcher]-[coder]-[finishes tasks, moves issue to in progress column, creates PR]
```

### The Agents
**The Analyst:** Specializes in "Functional Analysis". It explores the repository tree, reads relevant source files, and updates the GitHub issue with a technical implementation plan.

**The Coder:** A Senior Java Developer persona. It takes the analyst's recommendations, implements the fix on a new feature branch, and opens a Pull Request.

## 🛠️ The Technology Stack
By using Spring AI, the project remains model-agnostic while utilizing professional-grade patterns for tool integration.

**Spring AI Model GenAI:** Standardized connection to Google's Gemini models via ChatClient.

**Streamable HTTP Transport:** Uses HttpClientStreamableHttpTransport for bidirectional communication with the MCP server, providing a more robust connection than traditional SSE.

**GitHub GraphQL API:** Used for deep integration with GitHub Projects v2 to manage task states and metadata.