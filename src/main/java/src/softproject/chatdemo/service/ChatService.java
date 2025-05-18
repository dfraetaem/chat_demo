package src.softproject.chatdemo.service;

import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Mono;
import src.softproject.chatdemo.entity.ChatMessage;

import java.util.List;

public interface ChatService {
    String initOrUpdateSession(String userId, Integer expireSeconds);
    void saveMessages(List<ChatMessage> messages);
    List<Message> getRecentMessages(String chatId, int maxCount);
    public Mono<Void> saveAssistantMessage(String chatId, String fullContent);
}
