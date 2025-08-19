package io.moyam.chatbot.domain.conversation.repository;

import io.moyam.chatbot.domain.conversation.model.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper {
    void insert(Message message);
    List<Message> findByConversationId(@Param("conversationId") Long conversationId);
    void deleteById(@Param("id") Long id);
}
