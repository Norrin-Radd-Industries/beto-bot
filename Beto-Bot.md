# 🤖 Beto-Bot: Custom Java Multi-Agent Orchestrator

  

This project is a custom Multi-Agent Orchestrator built in Java (Spring Boot). It connects to a Model Context Protocol (MCP) server to allow AI agents to interact with external tools (specifically, GitHub).

  

## 🏛️Architecture Overview


The system consists of three main components:

1.  **MCP Client (The Bridge):** A Java process manager that spawns and communicates with the Node.js MCP server via JSON-RPC over standard I/O streams.

2.  **LLM Client:** The integration with an LLM (like Gemini or Claude) to handle reasoning and tool calling.

3.  **The Orchestrator Loop:** The ReAct (Reason + Act) loop that manages the agent's state, routes tool calls, and handles sub-agent delegation.

  

---

  

## 1. The MCP Bridge (Process & I/O)

  
The foundation of the project is the `ProcessBuilder` that launches the MCP server and captures its input/output streams.

  

*   **Process:** `npx -y @modelcontextprotocol/server-github`

*   **Communication:** We wrap the raw process streams in `BufferedReader` (to read from the server) and `BufferedWriter` (to write to the server).

  

### Code Snippet: Process & Stream Setup

[[Logic]]

#### Edit on 1/4
i moved the processBuilder into a McpConfig file where i created a bean for the githubClient()

```java
@Bean  
public McpClient githubClient() throws IOException {  
    ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c",  
            "npx -y @modelcontextprotocol/server-github@latest mcp-server-github");  
    processBuilder.environment().put("GITHUB_PERSONAL_ACCESS_TOKEN", githubApiKey);  
    processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);  
    return new McpClient(processBuilder);  
}
```


---

  

## 2. JSON-RPC Communication Models

The MCP protocol uses JSON-RPC 2.0. We use Java Records to model these messages and Jackson to serialize/deserialize them.

The aggregate : [[Requests (28-03)#Json RCP message]]
The Params: [[Requests (28-03)#Initialize Params]]
The Client info: [[Requests (28-03)#Client info]]
The Capabilities: [[Requests (28-03)#Capabilities]]

handshake example: [[Requests (28-03)#Sending the Handshake]]
  


---


### More on why the queue and completeableFuture

### The requestQueue is the "Ticket Counter"
  The requestQueue is where you store the "Unfilled Promises" while you wait for the server to reply.

   * When you call sendRequest("list_issues"):
       1. Java creates a "Ticket" (the CompletableFuture).
       2. Java puts that Ticket in the requestQueue Map with the label "2".
       3. Java then just sits there (non-blockingly) waiting for that Ticket to be stamped.

   * In the Background Thread (the Mail Sorter):
       1. The thread is constantly reading lines of text from the server.
       2. It sees: {"id": "2", "result": [...]}.
       3. It asks the requestQueue: "Hey, do we have a waiting ticket for ID 2?"
       4. It finds the Ticket and calls .complete(data).

 ### Without the requestQueue...
  Your Java code would have no idea which response belongs to which request. It would just see a random string of JSON and not
  know if it's the tools list, an issue list, or an error message.

 #### Why use CompletableFuture?
  The CompletableFuture is the "Magic Portal" that lets your main code (the run() method) stay clean and readable:

   1 githubClient.callTool("list_issues") // This returns the 'Ticket' from the queue
   2     .thenAccept(data -> {
   3         // This code ONLY runs when the background thread
   4         // finds the 'id' and completes the ticket!
   5     });




## Continuing with the integration of Gemini API

First, i read the docs on https://ai.google.dev/gemini-api/docs/quickstart#java

This calls for a simple dependency to be added
While i was tinkering around i though it would be better to change the approach a bit

instead of trying to chat with the LLM, i would try to make the loop more autonomous.

First thing i did here is trying to include the GithubMCP client i already made in Gemini's tools

i created a parser for this and first got the available tools from the githubMcpResponse from my listTools method and mapped the schema to the a new GeminiSchema.
I then used the FunctionDeclation builder to create a new function for each available tool
and exported the list of tools

```java
private List<Tool> parseGithubMcpToolsToGeminiTools(String tools) throws JsonProcessingException {  
    ObjectMapper mapper = new ObjectMapper();  
  
    // explore the tree structure of the toolsJson  
    JsonNode root = mapper.readTree(tools);  
    JsonNode toolsNode = root.get("result").get("tools");  
  
    // map to hold the gemini functionDeclarations  
    List<FunctionDeclaration> declarations = new ArrayList<>();  
  
    toolsNode.forEach(tool -> {  
        String name = tool.get("name").asText();  
        String description = tool.get("description").asText();  
  
        // both use same json schema so we can map it easily  
        JsonNode githubSchema = tool.get("inputSchema");  
        Schema geminiSchema = mapper.convertValue(githubSchema, Schema.class);  
  
        declarations.add(  
                FunctionDeclaration.builder()  
                        .name(name)  
                        .description(description)  
                        .parameters(geminiSchema)  
                        .build()  
        );  
    });  
    // call the builder for google's Tool to inject the declarations into  
    return List.of(Tool.builder().functionDeclarations(declarations).build());  
}
```


### Next level with Gemini

Now that i had all the tools mapped to Gemini i want to see if i could
implement gemini more as a standalone agent inside the application

i wanted to : 
- fetch issues on a schedule
- make sure gemini could read them using the tools
- create a correct prompt if it did find an issue to work on

i first started with the fetcher

```java
package beto.be.mcpbetobot.orchestrator;  
  
import beto.be.mcpbetobot.events.GithubIssueEvent;  
import beto.be.mcpbetobot.messages.response.GithubIssue;  
import beto.be.mcpbetobot.process.github.McpClient;  
import beto.be.mcpbetobot.util.Parser;  
import org.slf4j.Logger;  
import org.slf4j.LoggerFactory;  
import org.springframework.context.ApplicationEventPublisher;  
import org.springframework.scheduling.annotation.Scheduled;  
import org.springframework.stereotype.Service;  
  
import java.util.List;  
import java.util.Map;  
  
/**  
 * A scheduler to run a fetch every 30 min for issues on a specific github repo * This doesnt leverage any LLM, so its cheap in that sense */@Service  
public class BetoBotIssueFetcher {  
  
    private final Logger logger = LoggerFactory.getLogger(BetoBotIssueFetcher.class);  
    private final McpClient githubClient;  
    private final ApplicationEventPublisher applicationEventPublisher;  
  
    public BetoBotIssueFetcher(McpClient githubClient, ApplicationEventPublisher applicationEventPublisher) {  
        this.githubClient = githubClient;  
        this.applicationEventPublisher = applicationEventPublisher;  
    }  
    //TODO make it more generic instead of limiting it one repo  
    @Scheduled(fixedRate = 18000000)  
    public void checkForAvailableWork() {  
        logger.info(" --Checking for available work-- ");  
        githubClient.callTool("list_issues", Map.of("owner", "SilverSurferState",  
                        "repo", "beto-bot",  
                        "state", "open"))  
                .thenAccept(issues -> {  
                    List<GithubIssue> newIssues = Parser.parseIssues(issues);  
                // send an event to the coder tool when new issues are found  
                    newIssues.forEach(issue -> {  
                        applicationEventPublisher.publishEvent(new GithubIssueEvent("Fetcher", issue));  
                    });  
                });  
    }  
}
```

i used an applicationEvent that i created to hand it off to the orchestrator later

```java
package beto.be.mcpbetobot.events;  
  
import beto.be.mcpbetobot.messages.response.GithubIssue;  
import org.springframework.context.ApplicationEvent;  
  
public class GithubIssueEvent extends ApplicationEvent {  
  
    private final GithubIssue githubIssue;  
  
    public GithubIssueEvent(Object source, GithubIssue issue) {  
        super(source);  
        this.githubIssue = issue;  
    }  
  
    public GithubIssue getGithubIssue() {  
        return this.githubIssue;  
    }  
}
```


I could then move on to the Orchestrator again

first thing i did was create a map so that when i fetched the tools when the application ran, i could save them in a list that i could later hand off to the LLM
that made the run method look alot cleaner 

```java
/**  
 * Initial handshake and tool fetch + cache */@Override  
public void run(String... args) {  
    githubClient.connect()  
            .thenCompose(v -> githubClient.listTools())  
            .thenAccept(tools -> {  
                try {  
                   cachedGeminiTools = Parser.parseGithubMcpToolsToGeminiTools(tools);  
                } catch (JsonProcessingException e) {  
                    logger.error("Error parsing Tools");  
                    throw new RuntimeException(e);  
                }  
            });  
}
```

i created the processTicket first with the eventListener 

this would get the issue from the event, give a prompt to the llm and then start the agent
```java
@EventListener  
public void processTicket(GithubIssueEvent issueEvent){  
    GithubIssue issue = issueEvent.getGithubIssue();  
    logger.info(">>> Agent starting on issue: {} <<<", issue.number());  
  
    String prompt = String.format("""  
            System context:            Repository owner: SilverSurferState            Repository name: beto-bot            you must always provide these owner and repo values when calling tools                        Task:  
            You are senior Java Developer. You need to fix or implement the following issue:                        Title: %s  
            Description: %s                        Todo:  
            1. Use 'get_file_contents' to understand the project            2. Once you understand the project, implement or fix the issue            3. Create a new branch named 'feature/issue-%d'            4. Use 'push_files' to commit your changes and to that branch you just created            5. Finish by summarizing what you changed            """, issue.title(), issue.body(), issue.number());  
  
    if (!this.cachedGeminiTools.isEmpty()){  
        startAgent(prompt, this.cachedGeminiTools);  
    } else {  
        logger.error("no tools found, check handshake logic");  
    }  
}
```

the startAgent method was quite difficult to figure out because google has made pretty much every part of the response optional
so i had to adjust alot of code here and there to make it look as clean as possible

```java
private void startAgent(String prompt, List<Tool> tools) {  
    List<Content> history = new ArrayList<>();  
    history.add(Content.builder()  
            .role("user")  
            .parts(List.of(Part.builder()  
                    .text(prompt)  
                    .build()))  
            .build());  
  
    boolean finished = false;  
    while (!finished) {  
        GenerateContentConfig config = GenerateContentConfig.builder().tools(tools).build();  
        GenerateContentResponse response = geminiAgent.askWithTools(history, config);  
  
        // add the llm's response to the history too  
        Content modelResponse = response.candidates()  
                .flatMap(list -> list.stream().findFirst())  
            if (call.args().isPresent() && call.name().isPresent()) {  
                String result = githubClient.callTool(call.name().get(), call.args().get()).join();  
                history.add(Content.builder().role("function")  
                        .parts(List.of(Part.builder()  
                                .functionResponse(FunctionResponse.builder()  
                                        .name(call.name().get())  
                                        .response(Map.of("result", result))  
                                        .build())  
                .flatMap(Candidate::content)  
                .orElse(Content.builder().build());  
  
        history.add(modelResponse);  
  
        List<Part> parts = response.candidates()  
                .flatMap(list -> list.stream().findFirst())  
                .flatMap(Candidate::content)  
                .flatMap(Content::parts)  
                .orElse(Collections.emptyList());  
  
        if (parts.isEmpty()) {  
            logger.error("Empty response or gated response by LLM");  
            finished = true;  
            continue;  
        }  
  
        Optional<FunctionCall> toolCall = parts.stream()  
                .map(Part::functionCall)  
                .flatMap(Optional::stream)  
                .findFirst();  
  
        if (toolCall.isPresent()) {  
            FunctionCall call = toolCall.get();  
            logger.info("---Using {}", call.name());  
                                .build()))  
                        .build());  
            }  
        } else {  
            String answer = parts.stream()  
                    .map(Part::text)  
                    .flatMap(Optional::stream)  
                    .findFirst()  
                    .orElse("No answer given");  
  
            logger.info("---Answer: {}", answer);  
            finished = true;  
        }  
    }  
}
```
