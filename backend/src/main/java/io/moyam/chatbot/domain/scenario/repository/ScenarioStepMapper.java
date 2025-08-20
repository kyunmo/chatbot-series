package io.moyam.chatbot.domain.scenario.repository;

import io.moyam.chatbot.domain.scenario.model.ScenarioStep;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ScenarioStepMapper {
    // 기본 CRUD
    void insert(ScenarioStep scenarioStep);
    Optional<ScenarioStep> findById(@Param("id") Long id);
    List<ScenarioStep> findByScenarioId(@Param("scenarioId") Long scenarioId);
    void update(ScenarioStep scenarioStep);
    void deleteById(@Param("id") Long id);

    // 시나리오 실행용
    Optional<ScenarioStep> findStartStep(@Param("scenarioId") Long scenarioId);
    Optional<ScenarioStep> findNextStep(@Param("currentStepId") Long currentStepId);
}
