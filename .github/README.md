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
            if (call.args().isPresent() && call.name().isPresent()) {  
                String result = githubClient.callTool(call.name().get(), call.args().get()).join();  
                history.add(Content.builder().role("function")  
                        .parts(List.of(Part.builder()  
                                .functionResponse(FunctionResponse.builder()  
                                        .name(call.name().get())  
                                        .response(Map.of("result", result))  
                                        .build())  
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

current state : 

![[Pasted image 20260402014539.png]]

## Continuing on 2/4

![[Pasted image 20260403005342.png]]

Before starting i managed to fix the parser issue.

When i used a forEach look it wouldnt work, only by using a for loop like (int i = ...) i was able to get the jsonNode to get each one and map them to a tool

when it came to the schema's i noticed it crashed there too. So i looked into the Schema class from googles lib and used the fromJson static method there. It worked after that.

#### using the model itself
Some progress made but not much. At first the model i had selected 'Gemini-1.5-pro' wasnt even in the selection anymore. Once i switched to 2.5 i saw in the google cloud interface some requests were being made.

- t is able to fetch the issues correctly
- it passes them on to the agent
- the agent reads the prompt and responds to it
- when it tries to get the context from the repo using get_file_contents, it fails 


i added some logger info on several steps to try and pinpoint the issue
After looking up some stuff and talking with gemini chat, it looks like the culprit might be the npx and commandlinerunner im using.

so first step for next session is to refactor the mcpclient into a local github mcp server, where we can then use simple http to call the tools


### update on 4/4
After a lot of going back and forth trying to get the Github mcp server running locally through SSE,
i finally got it working

issues i ran into: 
```
<dependency>  
    <groupId>org.springframework.ai</groupId>  
    <artifactId>spring-ai-starter-mcp-client-webflux</artifactId>  
</dependency>
```

need a bean of type McpAsyncClient
but nowhere in the documentation its states that its expecting a List<McpAsyncClient>

so i refactored the config with an extra bean to return a list of mcpAsyncClients

```java
@Bean  
public List<McpAsyncClient> customMcpAsyncClientList(McpAsyncClient githubMcpClient) {  
    return List.of(githubMcpClient);  
}
```

after that i still got issues trying to connect to the mcp itself, it kept giving me 405
this was due to spring expecting a 2 way handshake while the mcp server was already serving me after the first initial request, after a lot of trial and error trying to set up the handshake i reverted to a different method where i would go back to placing the server in STDIO ( even though it looks like it can except SSE ( i used a CURL command together with a token and get a response back))

so i made some changes to the docker compose file 
and put a proxy in between to facilitate my application sending SSE and the server talking in STDIO

basically
```
[beto-bot] <--SSE-->[proxy]<--STDIO-->[github mcp]
```

i then simplified the mcpClient bean in my config 
i added a block to make sure we wait on that initial connection with the MCP server before starting our application context fully
and also added a requestTimeOut to the client of 5 min instead of the standard 20s to make sure our Client doesnt assume the application is hanging and closes the stream before the agent has a time to form a response

```java
@Bean  
@Primary  
public McpAsyncClient githubMcpClient() {  
    String mcpUrl = "http://localhost:9090/sse";  
  
    var transport = HttpClientSseClientTransport.builder(mcpUrl)  
            .build();  
  
    var client = McpClient.async(transport)  
            .requestTimeout(Duration.ofMinutes(5)).build();  
    try {  
        logger.info("--- Connecting to GitHub MCP Proxy ---");  
        client.initialize()  
                .retryWhen(Retry.fixedDelay(10, Duration.ofSeconds(2)))  
                .block(Duration.ofSeconds(30));  
        logger.info("--- GITHUB MCP READY ---");  
    } catch (Exception e) {  
        logger.error("Failed to init with MCP Proxy: {}", e.getMessage());  
    }  
    return client;  
}
```
we could then also delete our custom mcpClient we built before
(it was a good learner so it served its purpose to teach us about mcp clients and how they work)

that worked perfectly
now we still need to pass our tools back to gemini since were now using a standard mcpClient we have method at our disposal to do just that

with bits and pieces from our parser i refactored the orchestrator into a cleaner service

the service now follows this path more clearly for functioncalling

![[Pasted image 20260404163105.png]] source: https://ai.google.dev/gemini-api/docs/function-calling?example=weather#mcp-limitations

the first ticket that worked on was this : 

![[Pasted image 20260404174106.png]]

and got handled as such: 

![[Pasted image 20260404174127.png]]

i decided to push the changes i had made to make this a reality before i actually created or edited that ticket

so here is the latest version of the orchestrator

```java
package beto.be.mcpbetobot.orchestrator;  
  
import beto.be.mcpbetobot.events.GithubIssueEvent;  
import beto.be.mcpbetobot.facilitator.Agent;  
import beto.be.mcpbetobot.domain.GithubIssue;  
import com.fasterxml.jackson.core.JsonProcessingException;  
import com.fasterxml.jackson.databind.DeserializationFeature;  
import com.fasterxml.jackson.databind.ObjectMapper;  
import com.google.genai.types.*;  
import io.modelcontextprotocol.client.McpAsyncClient;  
import io.modelcontextprotocol.spec.McpSchema;  
import org.jspecify.annotations.NonNull;  
import org.slf4j.Logger;  
import org.slf4j.LoggerFactory;  
import org.springframework.context.event.EventListener;  
import org.springframework.stereotype.Service;  
  
import java.time.Duration;  
import java.util.*;  
import java.util.stream.Collectors;  
  
@Service  
public class BetoBotOrchestrator {  
  
    private final Logger logger = LoggerFactory.getLogger(BetoBotOrchestrator.class);  
  
    private final McpAsyncClient githubMcpClientImpl;  
    private final Agent agent;  
  
    public BetoBotOrchestrator(List<McpAsyncClient> customMcpAsyncClientList, Agent agent) {  
        this.githubMcpClientImpl = customMcpAsyncClientList.getFirst();  
        this.agent = agent;  
    }  
  
    @EventListener  
    public void processTicket(GithubIssueEvent issueEvent){  
        GithubIssue issue = issueEvent.getGithubIssue();  
  
        logger.info(">>> Orchestrating agent for issue: {} <<<", issue.number());  
  
        githubMcpClientImpl.listTools() // hands agent tools from mcp  
                .timeout(Duration.ofSeconds(60)) // give the agent some time to think  
                .doOnSuccess(toolsList -> {  
                    List<Tool> geminiTools = toolsList != null ? mapGithubToolsToGemini(toolsList.tools()) : Collections.emptyList();  
                    String prompt = buildPrompt(issue);  
                    // start thread to have it non-blocking  
                    Thread.ofVirtual().start(() -> {  
                        try {  
                            startAgent(prompt, geminiTools);  
                        } catch (Exception e) {  
                            logger.error("Virtual Thread with agent failed: {}", e.getMessage());  
                        }  
                    });  
                })  
                .doOnError(error -> logger.error("Orchestration failed: {}", error.getMessage()))  
                .subscribe();  
  
    }  
  
  
    private void startAgent(String prompt, List<Tool> tools) {  
        List<Content> history = new ArrayList<>();  
        //hand it our initial prompt  
        history.add(buildMessage(prompt));  
  
        boolean finished = false;  
        while (!finished) {  
            GenerateContentConfig config = GenerateContentConfig.builder().tools(tools).build();  
            GenerateContentResponse response = agent.askWithTools(history, config);  
  
            Content modelResponse = extractModelResponse(response);  
            history.add(modelResponse);  
  
            Optional<FunctionCall> toolCall = fetchToolCall(modelResponse);  
            if (toolCall.isPresent()) {  
                executeToolAndAddToHistory(toolCall.get(), history);  
            } else {  
                logger.info("---Answer: {}", extractText(modelResponse));  
                finished = true;  
            }  
        }  
    }  
  
    // when the agent uses a tool we want to see which one and add it to the conversation history  
    private void executeToolAndAddToHistory(FunctionCall call, List<Content> history) {  
        if(call.name().isPresent() && call.args().isPresent()){  
            String name = call.name().get();  
            Map<String, Object> args = call.args().orElse(Collections.emptyMap());  
  
            logger.info(" >>> Using mcp-tool: {}", name);  
  
            McpSchema.CallToolResult result = githubMcpClientImpl.callTool(  
                    new McpSchema.CallToolRequest(name, args)).block();  
            if (result != null) {  
                String toolOutput = result.content().stream()  
                        .map(content -> {  
                            if (content instanceof McpSchema.TextContent textContent) {  
                                return textContent.text();  
                            }  
                            return content.toString();  
                        })  
                        .collect(Collectors.joining("\n"));  
                history.add(Content.builder().role("function")  
                        .parts(List.of(Part.builder()  
                                .functionResponse(FunctionResponse  
                                        .builder()  
                                        .name(name)  
                                        .response(Map.of("result",toolOutput))  
                                        .build())  
                                .build()))  
                        .build()  
                );  
            }  
        }  
    }  
  
  
    /* <<< Helper methods >>> */  
  
    private static @NonNull String buildPrompt(GithubIssue issue) {  
        return String.format("""  
                System context:                Repository owner: SilverSurferState                Repository name: beto-bot                you must always provide these owner and repo values when calling tools                                Task:  
                You are senior Java Developer. You need to fix or implement the following issue:                                Title: %s  
                Description: %s                                Todo:  
                1. Use 'get_file_contents' with path='.' to list the root directory and to understand the project                2. Once you understand the project, implement or fix the issue                3. Create a new branch named 'feature/issue-%d'                4. Use 'push_files' to commit your changes and to that branch you just created                5. Finish by using 'create_pull_request' to create a new pull request and                summarizing what you changed in the 'body' section of the 'create_pull_request' function.                """, issue.title(), issue.body(), issue.number());  
    }  
  
    private Content buildMessage(String text) {  
        return Content.builder()  
                .role("user")  
                .parts(List.of(Part.builder()  
                        .text(text)  
                        .build()))  
                .build();  
    }  
  
    // response has candidates -> has content -> has parts ( all optional )  
    private Content extractModelResponse(GenerateContentResponse response) {  
        return response.candidates()  
                .flatMap(list -> list.stream().findFirst())  
                .flatMap(Candidate::content)  
                .orElseThrow(() -> new RuntimeException("Agent returned and empty response"));  
    }  
  
    // checks if the agent requires a tool to proceed  
    private Optional<FunctionCall> fetchToolCall(Content content) {  
        return content.parts().stream()  
                .flatMap(List::stream)  
                .map(Part::functionCall)  
                .flatMap(Optional::stream)  
                .findFirst();  
    }  
  
    // gathers the parts to form answer  
    private String extractText(Content content) {  
        return content.parts().stream()  
                .flatMap(List::stream)  
                .map(Part::text)  
                .flatMap(Optional::stream)  
                .collect(Collectors.joining("\n"));  
    }  
  
    // map tools from our mcp so agent knows its there  
    private List<Tool> mapGithubToolsToGemini(List<McpSchema.Tool> githubTools){  
        List<FunctionDeclaration> declarations = githubTools.stream()  
                .map(githubTool -> FunctionDeclaration.builder()  
                        .name(githubTool.name())  
                        .description(githubTool.description())  
                        .parameters(githubToGeminiSchema(githubTool.inputSchema())  
                        ).build())  
                .toList();  
  
        return List.of(Tool.builder().functionDeclarations(declarations).build());  
    }  
  
    // method to parse the github jsonSchema to the geminiSchema TODO check if works for other agents  
    private Schema githubToGeminiSchema(McpSchema.JsonSchema githubSchema) {  
        try {  
            ObjectMapper mapper = new ObjectMapper()  
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);  
  
            String schemaJson = mapper.writeValueAsString(githubSchema);  
            return Schema.fromJson(schemaJson);  
        } catch (JsonProcessingException e) {  
            logger.error("Error parsing githubSchema <-> geminiSchema");  
        }  
        return null;  
    }  
}
```

