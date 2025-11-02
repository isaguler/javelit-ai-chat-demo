package com.isaguler;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

public interface Assistant {

    String chat(List<ChatMessage> userMessage);

}
