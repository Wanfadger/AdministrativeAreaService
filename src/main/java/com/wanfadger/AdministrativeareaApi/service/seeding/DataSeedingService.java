package com.wanfadger.AdministrativeareaApi.service.seeding;

import com.github.javafaker.Faker;
import com.wanfadger.AdministrativeareaApi.entity.*;
import com.wanfadger.AdministrativeareaApi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataSeedingService implements CommandLineRunner {

    private final RegionRepository regionRepository;
    private final SubRegionRepository subRegionRepository;
    private final LocalGovernmentRepository localGovernmentRepository;
    private final CountyRepository countyRepository;
    private final SubCountyRepository subCountyRepository;
    private final ParishRepository parishRepository;

    private final Faker faker = new Faker();
    private final Random random = new Random();

    @Value("${app.seed.test-data:false}")
    private boolean seedTestData;

    @Value("${app.seed.clear-existing:false}")
    private boolean clearExisting;

    private static final int TARGET_COUNT = 10000;
    private static final int BATCH_SIZE = 500;

    @Override
    public void run(String... args) {
        if (seedTestData) {
            seed();
        }
    }

    @Transactional
    public void seed() {
        if (clearExisting) {
            log.info("Clearing existing data...");
            parishRepository.deleteAllInBatch();
            subCountyRepository.deleteAllInBatch();
            countyRepository.deleteAllInBatch();
            localGovernmentRepository.deleteAllInBatch();
            subRegionRepository.deleteAllInBatch();
            regionRepository.deleteAllInBatch();
        }

        if (regionRepository.count() >= TARGET_COUNT) {
            log.info("Data already seeded. Skipping...");
            return;
        }

        log.info("Starting high-volume data seeding ({} records per level)...", TARGET_COUNT);

        List<Region> regions = seedRegions();
        List<SubRegion> subRegions = seedSubRegions(regions);
        List<LocalGovernment> localGovernments = seedLocalGovernments(subRegions);
        List<County> counties = seedCounties(localGovernments);
        List<SubCounty> subCounties = seedSubCounties(counties);
        seedParishes(subCounties);

        log.info("Seeding completed successfully.");
    }

    private List<Region> seedRegions() {
        log.info("Seeding Regions...");
        List<Region> regions = new ArrayList<>();
        for (int i = 0; i < TARGET_COUNT; i++) {
            Region region = Region.builder()
                    .name(faker.address().state() + " " + i + " " + faker.random().hex(4))
                    .code("REG-" + i)
                    .build();
            regions.add(region);
            if (regions.size() >= BATCH_SIZE) {
                regionRepository.saveAll(regions);
                regions.clear();
            }
        }
        if (!regions.isEmpty()) {
            regionRepository.saveAll(regions);
        }
        return regionRepository.findAll();
    }

    private List<SubRegion> seedSubRegions(List<Region> regions) {
        log.info("Seeding SubRegions...");
        List<SubRegion> subRegions = new ArrayList<>();
        for (int i = 0; i < TARGET_COUNT; i++) {
            SubRegion subRegion = SubRegion.builder()
                    .name(faker.address().cityName() + " " + i + " " + faker.random().hex(4))
                    .code("SRG-" + i)
                    .region(regions.get(random.nextInt(regions.size())))
                    .build();
            subRegions.add(subRegion);
            if (subRegions.size() >= BATCH_SIZE) {
                subRegionRepository.saveAll(subRegions);
                subRegions.clear();
            }
        }
        if (!subRegions.isEmpty()) {
            subRegionRepository.saveAll(subRegions);
        }
        return subRegionRepository.findAll();
    }

    private List<LocalGovernment> seedLocalGovernments(List<SubRegion> subRegions) {
        log.info("Seeding LocalGovernments...");
        List<LocalGovernment> lgs = new ArrayList<>();
        for (int i = 0; i < TARGET_COUNT; i++) {
            LocalGovernment lg = LocalGovernment.builder()
                    .name(faker.address().city() + " LG " + i + " " + faker.random().hex(4))
                    .code("LGO-" + i)
                    .subRegion(subRegions.get(random.nextInt(subRegions.size())))
                    .build();
            lgs.add(lg);
            if (lgs.size() >= BATCH_SIZE) {
                localGovernmentRepository.saveAll(lgs);
                lgs.clear();
            }
        }
        if (!lgs.isEmpty()) {
            localGovernmentRepository.saveAll(lgs);
        }
        return localGovernmentRepository.findAll();
    }

    private List<County> seedCounties(List<LocalGovernment> lgs) {
        log.info("Seeding Counties...");
        List<County> counties = new ArrayList<>();
        for (int i = 0; i < TARGET_COUNT; i++) {
            County county = County.builder()
                    .name(faker.address().cityName() + " " + i + " " + faker.random().hex(4))
                    .code("CNY-" + i)
                    .localGovernment(lgs.get(random.nextInt(lgs.size())))
                    .build();
            counties.add(county);
            if (counties.size() >= BATCH_SIZE) {
                countyRepository.saveAll(counties);
                counties.clear();
            }
        }
        if (!counties.isEmpty()) {
            countyRepository.saveAll(counties);
        }
        return countyRepository.findAll();
    }

    private List<SubCounty> seedSubCounties(List<County> counties) {
        log.info("Seeding SubCounties...");
        List<SubCounty> subCounties = new ArrayList<>();
        for (int i = 0; i < TARGET_COUNT; i++) {
            SubCounty sc = SubCounty.builder()
                    .name(faker.address().cityPrefix() + " SC " + i + " " + faker.random().hex(4))
                    .code("SCN-" + i)
                    .county(counties.get(random.nextInt(counties.size())))
                    .build();
            subCounties.add(sc);
            if (subCounties.size() >= BATCH_SIZE) {
                subCountyRepository.saveAll(subCounties);
                subCounties.clear();
            }
        }
        if (!subCounties.isEmpty()) {
            subCountyRepository.saveAll(subCounties);
        }
        return subCountyRepository.findAll();
    }

    private void seedParishes(List<SubCounty> subCounties) {
        log.info("Seeding Parishes...");
        List<Parish> parishes = new ArrayList<>();
        for (int i = 0; i < TARGET_COUNT; i++) {
            Parish parish = Parish.builder()
                    .name(faker.address().streetName() + " Parish " + i + " " + faker.random().hex(4))
                    .code("PAR-" + i)
                    .subCounty(subCounties.get(random.nextInt(subCounties.size())))
                    .build();
            parishes.add(parish);
            if (parishes.size() >= BATCH_SIZE) {
                parishRepository.saveAll(parishes);
                parishes.clear();
            }
        }
        if (!parishes.isEmpty()) {
            parishRepository.saveAll(parishes);
        }
    }
}
