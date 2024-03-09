package com.wanfadger.AdministrativeareaApi.shared.util;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AdministrativeAreaRunner implements CommandLineRunner {

    @Override
    @Transactional
    public void run(String... args) throws Exception {

    }


}
