package io.moyam.chatbot.domain.scenario.repository;

import io.moyam.chatbot.domain.scenario.model.Scenario;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ScenarioMapper {
    void insert(Scenario scenario);
    Optional<Scenario> findById(@Param("id") Long id);
    List<Scenario> findByBotId(@Param("botId") Long botId);
    void update(Scenario scenario);
    void deleteById(@Param("id") Long id);
}
