package com.wanfadger.AdministrativeareaApi.service.county;

import com.wanfadger.AdministrativeareaApi.entity.County;
import com.wanfadger.AdministrativeareaApi.repository.CountyRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CountyServiceImpl implements DbCountyService  {

    final CountyRepository countyRepository;

    @Override
    public County dbNew(@NonNull County county) {
        return countyRepository.save(county);
    }

    @Override
    public List<County> dbNew(List<County> counties) {
        return countyRepository.saveAll(counties);
    }

    @Override
    public List<County> dbList() {
        return countyRepository.findAll();
    }

    @Override
    public Page<County> dbList(Pageable pageable) {
        return countyRepository.findAll(pageable);
    }

    @Override
    public List<County> dbAllByLocalGovernmentCode(String code) {
        return countyRepository.findAllByLocalGovernment_Code(code);
    }

    @Override
    public List<County> dbAllByLocalGovernmentCodes(List<String> codes) {
        return countyRepository.findAllByLocalGovernmentCodes(codes);
    }

    @Override
    public Optional<County> dbByName_LocalGovernment_Code(String name, String localGovernmentCode) {
        return countyRepository.findByNameIgnoreCaseAndLocalGovernment_Code( name ,localGovernmentCode);
    }

    @Override
    public Optional<County> dbByCode(String code) {
        return countyRepository.findByCodeIgnoreCase(code);
    }

}
