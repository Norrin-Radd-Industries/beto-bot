package beto.be.mcpbetobot.messages.buildingblocks;

public record InitializeParams(
        String protocolVersion,
        ClientInfo clientInfo,
        Capabilities capabilities
) {
}
