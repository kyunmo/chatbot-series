package io.moyam.chatbot.domain.bot.repository;

import io.moyam.chatbot.domain.bot.model.BotOption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface BotOptionMapper {
    void insert(BotOption botOption);
    Optional<BotOption> findById(@Param("id") Long id);
    List<BotOption> findByBotId(@Param("botId") Long botId);
    void update(BotOption botOption);
    void deleteById(@Param("id") Long id);
}
