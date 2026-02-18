package com.wanfadger.AdministrativeareaApi.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BeanConfig {

    @Bean
    public ObjectMapper objectMapper(){
        return new ObjectMapper();
    }




}
