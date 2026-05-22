package beto.be.mcpbetobot.infrastructure.agentic;

import beto.be.mcpbetobot.data.rag.RagService;
import org.jspecify.annotations.NullMarked;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CavemanRAGAdvisor implements CallAdvisor {

    @Override
    @NullMarked
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        Prompt originalPrompt = request.prompt();
        List<Message> originalMessages = originalPrompt.getInstructions();
        boolean modified = false;
        List<Message> updatedMessages = new ArrayList<>();
        for (Message message : originalMessages) {
            if (message.getMessageType() == MessageType.SYSTEM) {
                String originalText = message.getText();
                if (originalText != null && originalText.contains("task: retrieval_document:")) {
                    String updatedText = preprocessRAGContext(originalText);
                    updatedMessages.add(new SystemMessage(updatedText));
                    modified = true;
                    continue;
                }
            }
            updatedMessages.add(message);
        }
        if (modified) {
            Prompt updatedPrompt = new Prompt(updatedMessages, originalPrompt.getOptions());
            ChatClientRequest updatedRequest = request.mutate().prompt(updatedPrompt).build();
            return chain.nextCall(updatedRequest);
        }
        return chain.nextCall(request);
    }

    String preprocessRAGContext(String text) {
        if (text == null) {
            return null;
        }
        Pattern pattern = Pattern.compile("(?s)task: retrieval_document: (.*?)(?=(?:\\n--- FILE: )|(?:\\n\\n\\s*[A-Za-z])|\\z)");
        Matcher matcher = pattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String code = matcher.group(1);
            String processedCode = RagService.preprocessCode(code);
            matcher.appendReplacement(sb, Matcher.quoteReplacement("task: retrieval_document: " + processedCode));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @Override
    @NullMarked
    public String getName() {
        return "CavemanRAGAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
