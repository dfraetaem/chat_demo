package src.softproject.chatdemo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private Long messageId; // 数据库自增字段，不参与构造
    private String chatId;
    private String role;
    private String content;
    private LocalDateTime createdAt;

    // 自定义构造函数（仅包含4个字段）
    public ChatMessage(String chatId, String role, String content, LocalDateTime createdAt) {
        this.chatId = chatId;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
    }
}
