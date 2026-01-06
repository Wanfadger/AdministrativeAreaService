package com.wanfadger.AdministrativeareaApi.shared.beanConfig;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdministrativeAreaBeanConfigurations {

    @Bean
    public ObjectMapper objectMapper(){
        return new ObjectMapper();
    }




}
