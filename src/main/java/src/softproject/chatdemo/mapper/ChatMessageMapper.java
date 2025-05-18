package src.softproject.chatdemo.mapper;

import org.apache.ibatis.annotations.*;
import src.softproject.chatdemo.entity.ChatMessage;
import src.softproject.chatdemo.provider.ChatMessageSqlProvider;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    // 批量插入消息（使用动态 SQL Provider）
    @InsertProvider(type = ChatMessageSqlProvider.class, method = "batchInsertSql")
    @Options(useGeneratedKeys = true, keyProperty = "messageId")
    void batchInsert(List<ChatMessage> messages);

    // 查询最近的 N 条消息（保持原逻辑）
    @Select("SELECT * FROM chat_message WHERE chat_id = #{chatId} ORDER BY created_at DESC LIMIT #{maxCount}")
    List<ChatMessage> findRecentMessages(@Param("chatId") String chatId, @Param("maxCount") int maxCount);

}
