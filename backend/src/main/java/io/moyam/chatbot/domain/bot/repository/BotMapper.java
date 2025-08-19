package io.moyam.chatbot.domain.bot.repository;

import io.moyam.chatbot.domain.bot.model.Bot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface BotMapper {
    void insert(Bot bot);
    Optional<Bot> findById(@Param("id") Long id);
    List<Bot> findByUserId(@Param("userId") Long userId);
    void update(Bot bot);
    void deleteById(@Param("id") Long id);
}
