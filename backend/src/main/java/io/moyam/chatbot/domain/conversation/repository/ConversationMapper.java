package io.moyam.chatbot.domain.conversation.repository;

import io.moyam.chatbot.domain.conversation.model.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ConversationMapper {
    void insert(Conversation conversation);
    Optional<Conversation> findById(@Param("id") Long id);
    Optional<Conversation> findBySessionId(@Param("sessionId") String sessionId);
    List<Conversation> findByBotId(@Param("botId") Long botId);
    void update(Conversation conversation);
    void deleteById(@Param("id") Long id);
}
