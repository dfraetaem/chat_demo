package src.softproject.chatdemo.provider;

import src.softproject.chatdemo.entity.ChatMessage;

import java.util.List;
import java.util.Map;

public class ChatMessageSqlProvider {

    public String batchInsertSql(Map<String, Object> params) {
        List<ChatMessage> messages = (List<ChatMessage>) params.get("list");
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO chat_message (chat_id, role, content, created_at) VALUES ");

        for (int i = 0; i < messages.size(); i++) {
            sql.append("(#{list[").append(i).append("].chatId}, ")
                    .append("#{list[").append(i).append("].role}, ")
                    .append("#{list[").append(i).append("].content}, ")
                    .append("#{list[").append(i).append("].createdAt})");
            if (i < messages.size() - 1) {
                sql.append(", ");
            }
        }
        return sql.toString();
    }
}
