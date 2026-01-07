package com.wanfadger.AdministrativeareaApi.service.localgovernment;


import com.wanfadger.AdministrativeareaApi.entity.LocalGovernment;
import com.wanfadger.AdministrativeareaApi.repository.LocalGovernmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocalGovernmentServiceImpl implements DbLocalGovernmentService  {

    final LocalGovernmentRepository localGovernmentRepository;

    @Override
    public LocalGovernment dbNew(LocalGovernment localGovernment) {
        return localGovernmentRepository.save(localGovernment);
    }

    @Override
    public List<LocalGovernment> dbNew(List<LocalGovernment> localGovernments) {
        return localGovernmentRepository.saveAll(localGovernments);
    }

    @Override
    public List<LocalGovernment> dbList() {
        return localGovernmentRepository.findAll();
    }

    @Override
    public Page<LocalGovernment> dbList(Pageable pageable) {
        return localGovernmentRepository.findAll(pageable);
    }

    @Override
    public List<LocalGovernment> dbBySubRegionCode(String subRegionCode) {
        return localGovernmentRepository.findAllBySubRegion_Code(subRegionCode);
    }

    @Override
    public List<LocalGovernment> dbBySubRegionCodes(List<String> subRegionCodes) {
        return localGovernmentRepository.findAllBySubRegionCodes(subRegionCodes);
    }

    @Override
    public Optional<LocalGovernment> dbByName_SubRegionCode(String name, String subRegionCode) {
        return localGovernmentRepository.findByNameIgnoreCaseAndSubRegion_Code(name, subRegionCode);
    }

    @Override
    public Optional<LocalGovernment> dbByCode(String code) {
        return localGovernmentRepository.findByCodeIgnoreCase(code);
    }

}
