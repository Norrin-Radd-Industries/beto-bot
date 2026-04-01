package beto.be.mcpbetobot.facilitator;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GeminiAgent {

    private final Logger logger = LoggerFactory.getLogger(GeminiAgent.class);
    private final Client client = new Client();
    private final String PRO = "gemini-1.5-pro";

    public String ask(String prompt) {
        GenerateContentResponse response =
                client.models.generateContent(PRO, prompt, null);
        return response.text();
    }

    public GenerateContentResponse askWithTools(List<Content> history, GenerateContentConfig config) {
        // config for tools
        return client.models.generateContent(PRO, history, config);
    }
}
