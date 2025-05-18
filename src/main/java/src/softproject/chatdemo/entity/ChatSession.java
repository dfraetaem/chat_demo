package src.softproject.chatdemo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor // 确保生成全参数公共构造函数
public class ChatSession {
    private String chatId;
    private String userId;
    private LocalDateTime createdAt;
    private LocalDateTime lastActive;
    private Integer expireSeconds;

}
