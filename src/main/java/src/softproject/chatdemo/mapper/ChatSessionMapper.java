package src.softproject.chatdemo.mapper;

import org.apache.ibatis.annotations.*;
import src.softproject.chatdemo.entity.ChatSession;

import java.time.LocalDateTime;

@Mapper
public interface ChatSessionMapper {

    // 插入或更新会话（存在则更新 last_active）
    @Insert({
            "INSERT INTO chat_session (chat_id, user_id, created_at, last_active, expire_seconds)",
            "VALUES (#{chatId}, #{userId}, #{createdAt}, #{lastActive}, #{expireSeconds})",
            "ON DUPLICATE KEY UPDATE ",
            "user_id = COALESCE(#{userId}, user_id),",  // 保留原值（如果新值为 null）
            "last_active = #{lastActive}"
    })
    void upsertSession(ChatSession session);

    // 根据 chatId 查询会话
    @Select("SELECT * FROM chat_session WHERE chat_id = #{chatId}")
    ChatSession findSessionById(@Param("chatId") String chatId);
}
