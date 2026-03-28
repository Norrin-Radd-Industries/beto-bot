package beto.be.mcpbetobot.messages.request.buildingblocks;

public record InitializeParams(
        String protocolVersion,
        ClientInfo clientInfo,
        Capabilities capabilities
) {
}
