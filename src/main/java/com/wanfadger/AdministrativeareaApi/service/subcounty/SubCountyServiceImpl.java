package com.wanfadger.AdministrativeareaApi.service.subcounty;


import com.wanfadger.AdministrativeareaApi.entity.SubCounty;
import com.wanfadger.AdministrativeareaApi.repository.SubCountyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubCountyServiceImpl implements DbSubCountyService  {

    final SubCountyRepository subCountyRepository;

    @Override
    public SubCounty dbNew(SubCounty subCounty) {
        return subCountyRepository.save(subCounty);
    }

    @Override
    public List<SubCounty> dbNew(List<SubCounty> subCounties) {
        return subCountyRepository.saveAll(subCounties);
    }

    @Override
    public List<SubCounty> dbList() {
        return subCountyRepository.findAll();
    }

    @Override
    public List<SubCounty> dbByCountyCode(String countyCode) {
        return subCountyRepository.findAllByCounty_Code(countyCode);
    }

    @Override
    public List<SubCounty> dbByCountyCodes(List<String> countyCodes) {
        return subCountyRepository.findAllByCountyCodes(countyCodes);
    }

    @Override
    public Optional<SubCounty> dbByName_CountyCode(String name, String countyCode) {
        return subCountyRepository.findByNameIgnoreCaseAndCounty_Id(name , countyCode);
    }

    @Override
    public Optional<SubCounty> dbByCode(String code) {
        return subCountyRepository.findByCodeIgnoreCase(code);
    }


}
