package src.softproject.chatdemo.provider;

import src.softproject.chatdemo.entity.ChatSession;

import java.util.Map;

public class ChatSessionSqlProvider {

    // 生成 upsert SQL（插入或更新）
    public String upsertSessionSql(Map<String, Object> params) {
        ChatSession session = (ChatSession) params.get("session");
        return new StringBuilder()
                .append("INSERT INTO chat_session (chat_id, user_id, created_at, last_active, expire_seconds) ")
                .append("VALUES (")
                .append("#{session.chatId}, ")
                .append("#{session.userId}, ")
                .append("#{session.createdAt}, ")
                .append("#{session.lastActive}, ")
                .append("#{session.expireSeconds}) ")
                .append("ON DUPLICATE KEY UPDATE last_active = #{session.lastActive}")
                .toString();
    }

    // 生成根据 ID 查询的 SQL
    public String findSessionByIdSql(Map<String, Object> params) {
        return "SELECT * FROM chat_session WHERE chat_id = #{chatId}";
    }

    // 生成删除过期会话的 SQL
    public String deleteExpiredSessionsSql(Map<String, Object> params) {
        return "DELETE FROM chat_session WHERE last_active < #{threshold}";
    }
}
