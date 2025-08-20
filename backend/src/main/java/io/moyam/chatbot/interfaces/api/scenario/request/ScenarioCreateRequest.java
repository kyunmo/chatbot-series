package io.moyam.chatbot.interfaces.api.scenario.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScenarioCreateRequest {
    private Long botId;
    private String name;
    private String description;
}
