package com.wanfadger.AdministrativeareaApi.shared.beanConfig;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanfadger.AdministrativeareaApi.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdministrativeAreaBeanConfigurations {
    private final CompanyRepository companyRepository;

//    @Bean
//    public UserDetailsService userDetailsService(){
//        return email -> {
//            Company company = new Company();
//            company.setEmail("test");
//            company.setName("test");
//            //companyRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new NotFoundException("Email profile not found"));
//            return new User(company.getEmail() , "", company.isEnabled() , company.isAccountNonExpired() , company.isCredentialsNonExpired() , company.isAccountNonLocked() , Collections.EMPTY_LIST);
//        }; //load user;
//    }

    @Bean
    public ObjectMapper objectMapper(){
        return new ObjectMapper();
    }




}
