package beto.be.mcpbetobot.messages.request.buildingblocks;

import java.util.Map;

public record CallToolParams(String name, Map<String, Object> arguments) {}
