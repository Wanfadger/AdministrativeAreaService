package com.wanfadger.AdministrativeareaApi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class BeanConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }
}
