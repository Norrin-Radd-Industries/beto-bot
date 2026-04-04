package beto.be.mcpbetobot.facilitator;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Agent {

    //TODO try to implement other agents
    private final Client client = new Client();
    private final String GEMINI_PRO_2_5 = "gemini-2.5-pro";

    public GenerateContentResponse askWithTools(List<Content> history, GenerateContentConfig config) {
        // config for tools
        return client.models.generateContent(GEMINI_PRO_2_5, history, config);
    }
}
