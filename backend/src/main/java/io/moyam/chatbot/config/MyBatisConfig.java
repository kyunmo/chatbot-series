package io.moyam.chatbot.config;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;

import java.util.Map;

@org.springframework.context.annotation.Configuration
public class MyBatisConfig {

    @Bean
    public ConfigurationCustomizer mybatisConfigCustomizer() {
        return (Configuration configuration) -> {
            configuration.getTypeHandlerRegistry()
                    .register(Map.class, JdbcType.OTHER, JsonTypeHandler.class);
        };
    }
}
