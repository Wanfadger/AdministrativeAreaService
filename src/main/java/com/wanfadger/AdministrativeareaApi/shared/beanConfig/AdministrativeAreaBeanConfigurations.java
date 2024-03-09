package com.wanfadger.AdministrativeareaApi.shared.beanConfig;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanfadger.AdministrativeareaApi.entity.Company;
import com.wanfadger.AdministrativeareaApi.repository.CompanyRepository;
import com.wanfadger.AdministrativeareaApi.shared.administrativeareaexceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class AdministrativeAreaBeanConfigurations {
    private final CompanyRepository companyRepository;

    @Bean
    public UserDetailsService userDetailsService(){
        return email -> {
            Company company = companyRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new NotFoundException("Email profile not found"));
            return new User(company.getEmail() , "", company.isEnabled() , company.isAccountNonExpired() , company.isCredentialsNonExpired() , company.isAccountNonLocked() , Collections.EMPTY_LIST);
        }; //load user;
    }

    @Bean
    public ObjectMapper objectMapper(){
        return new ObjectMapper();
    }




}
