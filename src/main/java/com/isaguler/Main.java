package com.isaguler;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import io.javelit.core.Jt;
import io.javelit.core.JtContainer;
import io.javelit.core.Server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {

    private static final String OLLAMA_BASE_URL = "http://localhost:11434";
    private static final String OLLAMA_LLM_LLAMA3P2 = "llama3.2";
    private static final ChatModel OLLAMA_CHAT_MODEL =
            OllamaChatModel.builder()
                    .baseUrl(OLLAMA_BASE_URL)
                    .modelName(OLLAMA_LLM_LLAMA3P2)
                    .build();

    private static final McpTransport transport = new StdioMcpTransport.Builder()
            .command(List.of("npx", "-y", "@modelcontextprotocol/server-brave-search"))
            .environment(Map.of("BRAVE_API_KEY", "YOUR_API_KEY_HERE!"))
            .logEvents(true)
            .build();

    private static final McpClient mcpClient = new DefaultMcpClient.Builder()
            .transport(transport)
            .build();

    private static final ToolProvider toolProvider = McpToolProvider.builder()
            .mcpClients(List.of(mcpClient))
            .build();

    private static final Assistant ASSISTANT =
            AiServices.builder(Assistant.class)
                    .chatModel(OLLAMA_CHAT_MODEL)
                    .toolProvider(toolProvider)
                    .build();

    public static void main(String[] args) {
        final var server = Server.builder(Main::app, 8080).build();
        server.start();
    }

    @SuppressWarnings("unchecked")
    public static void app() {
        List<ChatMessage> chatHistory = (List<ChatMessage>) Jt.sessionState()
                .computeIfAbsent("chatHistory", (key) -> new ArrayList<>());

        Jt.title(":parrot: AIChat :speech_balloon:").use();

        JtContainer msgContainer = Jt.container().use();

        for (ChatMessage message : chatHistory) {
            switch (message.type()) {
                case USER -> Jt.markdown( ":speech_balloon: " +
                        ((UserMessage) message).singleText()).use(msgContainer);
                case AI -> Jt.markdown(":robot: " +
                        ((AiMessage) message).text()).use(msgContainer);
            }
        }

        String inputMessage = Jt.textInput("Your message:").use();

        if (inputMessage != null && !inputMessage.trim().isEmpty()) {
            chatHistory.add(UserMessage.from(inputMessage));
            Jt.markdown(":speech_balloon: " + inputMessage).use(msgContainer);

            // [for MCP usage via AiServices]
            String response = ASSISTANT.chat(chatHistory);
            chatHistory.add(new AiMessage(response));
            Jt.markdown(":robot: " + response).use(msgContainer);

            // [for direct chat model usage]
            //ChatResponse response = OLLAMA_CHAT_MODEL.chat(chatHistory);
            //chatHistory.add(response.aiMessage());
            //Jt.markdown(":robot: " + response.aiMessage().text()).use(msgContainer);
        }

        Jt.subheader("⬇️ Tech Stack ⬇️").use();

        record TechStack(String MODULE, Object TECH) {}

        List<Object> data = List.of(
                new TechStack("UI", "Javelit"),
                new TechStack("Backend", "Java 25"),
                new TechStack("AI integration", "Langchain4j"),
                new TechStack("AI inference", "Ollama (Llama3.2)"),
                new TechStack("Web search", "BraveSearch MCP")
        );

        Jt.table(data).use();
    }
}