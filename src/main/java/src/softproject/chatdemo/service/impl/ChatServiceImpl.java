package src.softproject.chatdemo.service.impl;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import src.softproject.chatdemo.entity.ChatMessage;
import src.softproject.chatdemo.entity.ChatSession;
import src.softproject.chatdemo.mapper.ChatMessageMapper;
import src.softproject.chatdemo.mapper.ChatSessionMapper;
import src.softproject.chatdemo.service.ChatService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;

    @Autowired
    public ChatServiceImpl(ChatSessionMapper sessionMapper, ChatMessageMapper messageMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
    }

    @Override
    public String initOrUpdateSession(String userId, Integer expireSeconds) {
        String chatId = generateChatId(userId);
        LocalDateTime now = LocalDateTime.now();

        ChatSession existingSession = sessionMapper.findSessionById(chatId);
        if (existingSession == null) {
            // 创建新会话（全字段赋值）
            sessionMapper.upsertSession(new ChatSession(chatId, userId, now, now, expireSeconds));
        } else {
            // 更新会话时保留原有值（避免传递 null）
            sessionMapper.upsertSession(new ChatSession(
                    chatId,
                    existingSession.getUserId(),      // 保留原 userId
                    existingSession.getCreatedAt(),    // 保留原 createdAt
                    now,                               // 更新 lastActive
                    existingSession.getExpireSeconds() // 保留原 expireSeconds
            ));
        }
        return chatId;
    }

    @Override
    @Transactional
    public void saveMessages(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("消息列表不能为空");
        }
        messageMapper.batchInsert(messages);

        String chatId = messages.get(0).getChatId();

        // 检查会话是否存在，不存在则创建
        ChatSession existingSession = sessionMapper.findSessionById(chatId);
        if (existingSession == null) {
            // 默认值创建新会话（需根据业务补充参数）
            sessionMapper.upsertSession(new ChatSession(
                    chatId,
                    "default_user", // 示例：默认用户ID，需替换为实际逻辑
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    3600
            ));
        } else {
            // 更新最后活跃时间
            sessionMapper.upsertSession(new ChatSession(
                    chatId,
                    existingSession.getUserId(),
                    existingSession.getCreatedAt(),
                    LocalDateTime.now(),
                    existingSession.getExpireSeconds()
            ));
        }
    }

    @Override
    public List<Message> getRecentMessages(String chatId, int maxCount) {
        return messageMapper.findRecentMessages(chatId, maxCount).stream()
                .map(m -> {
                    String role = m.getRole().toLowerCase();
                    switch (role) {
                        case "user":
                            return new UserMessage(m.getContent());
                        case "assistant":
                            return new AssistantMessage(m.getContent());
                        case "system":
                            return new SystemMessage(m.getContent());
                        default:
                            throw new IllegalArgumentException("未知角色: " + role);
                    }
                })
                .collect(Collectors.toList());
    }

    private String generateChatId(String userId) {
        // 保持原有生成逻辑，例如：userId + "_" + System.currentTimeMillis()
        return DigestUtils.md5Hex(userId + "_" + System.currentTimeMillis());
    }

    @Transactional
    public Mono<Void> saveAssistantMessage(String chatId, String fullContent) {
        return Mono.fromRunnable(() -> {
            saveMessages(List.of(
                    new ChatMessage(chatId, "assistant", fullContent, LocalDateTime.now())
            ));
        });
    }

}
