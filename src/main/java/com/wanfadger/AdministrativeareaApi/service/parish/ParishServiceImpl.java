package com.wanfadger.AdministrativeareaApi.service.parish;


import com.wanfadger.AdministrativeareaApi.entity.Parish;
import com.wanfadger.AdministrativeareaApi.repository.ParishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ParishServiceImpl implements DbParishService {

    final ParishRepository parishRepository;

    @Override
    public Parish dbNew(Parish parish) {
        return parishRepository.save(parish);
    }

    @Override
    public List<Parish> dbNew(List<Parish> parishes) {
        return parishRepository.saveAll(parishes);
    }

    @Override
    public List<Parish> dbList() {
        return parishRepository.findAll();
    }

    @Override
    public List<Parish> dbBySubCountyCode(String code) {
        return parishRepository.findAllBySubCounty_Code(code);
    }

    @Override
    public List<Parish> dbBySubCountyCodes(List<String> codes) {
        return parishRepository.findAllBySubCountyCodes(codes);
    }

    @Override
    public Optional<Parish> dbByName_SubCountyCode(String name, String subCountyCode) {
        return parishRepository.findByNameIgnoreCaseAndSubCounty_Code(name, subCountyCode);
    }

    @Override
    public Optional<Parish> dbByCode(String subCountyCode) {
        return parishRepository.findByCodeIgnoreCase(subCountyCode);
    }

}
