package io.moyam.chatbot.domain.scenario.repository;

import io.moyam.chatbot.domain.scenario.model.ScenarioStep;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ScenarioStepMapper {
    void insert(ScenarioStep scenarioStep);
    Optional<ScenarioStep> findById(@Param("id") Long id);
    List<ScenarioStep> findByScenarioId(@Param("scenarioId") Long scenarioId);
    void update(ScenarioStep scenarioStep);
    void deleteById(@Param("id") Long id);
}
