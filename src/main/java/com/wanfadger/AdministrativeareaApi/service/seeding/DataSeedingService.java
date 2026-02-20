package com.wanfadger.AdministrativeareaApi.service.seeding;

import com.github.javafaker.Faker;
import com.wanfadger.AdministrativeareaApi.entity.*;
import com.wanfadger.AdministrativeareaApi.repository.*;
import com.wanfadger.AdministrativeareaApi.shared.SharedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

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

    private final SharedService sharedService;
    private final Faker faker = new Faker();
    private final Random random = new Random();

    private static final String[] UGANDA_PLACES = {
            "Kampala", "Entebbe", "Jinja", "Mbarara", "Gulu", "Lira", "Mbale", "Arua", "Fort Portal",
            "Masaka", "Hoima", "Soroti", "Mukono", "Kasese", "Kabale", "Tororo", "Iganga", "Rukungiri",
            "Bushenyi", "Ntungamo", "Kitgum", "Moroto", "Kapchorwa", "Ibanda", "Lugazi", "Wakiso",
            "Mityana", "Mubende", "Masindi", "Kumi", "Nebbi", "Apac", "Kiboga", "Kamuli", "Pallisa"
    };

    private String randomUgandaPlace() {
        return UGANDA_PLACES[random.nextInt(UGANDA_PLACES.length)];
    }

    private double randomUgandaLatitude() {
        return -1.5 + (4.2 - (-1.5)) * random.nextDouble();
    }

    private double randomUgandaLongitude() {
        return 29.5 + (35.0 - 29.5) * random.nextDouble();
    }

    private double randomLatitudeNear(Double parentLat) {
        if (parentLat == null)
            return randomUgandaLatitude();
        return parentLat + (random.nextDouble() - 0.5) * 0.1; // roughly 5.5km variance
    }

    private double randomLongitudeNear(Double parentLon) {
        if (parentLon == null)
            return randomUgandaLongitude();
        return parentLon + (random.nextDouble() - 0.5) * 0.1;
    }

    @Value("${app.seed.test-data:false}")
    private boolean seedTestData;

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
        if (regionRepository.count() > 0) {
            log.info("Data already exists. Skipping seeding...");
            return;
        }

        log.info("Starting high-volume data seeding ({}} records per level)...", TARGET_COUNT);

        CompletableFuture.supplyAsync(this::seedRegions)
                .thenApplyAsync(this::seedSubRegions)
                .thenApplyAsync(this::seedLocalGovernments)
                .thenApplyAsync(this::seedCounties)
                .thenApplyAsync(this::seedSubCounties)
                .thenAcceptAsync(this::seedParishes)
                .join();

        log.info("Seeding completed successfully.");
    }

    private List<Region> seedRegions() {
        log.info("Seeding Regions...");
        List<Region> regions = new ArrayList<>();
        for (int i = 0; i < TARGET_COUNT; i++) {
            double lat = randomUgandaLatitude();
            double lon = randomUgandaLongitude();
            Region region = Region.builder()
                    .name(randomUgandaPlace() + " Region " + i + " " + faker.random().hex(4))
                    .code(sharedService.generateUniqueCode(AdministrativeAreaType.REGION,
                            code -> regionRepository.findByCodeIgnoreCase(code).isPresent()))
                    .areaType(AdministrativeAreaType.REGION)
                    .latitude(lat)
                    .longitude(lon)
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
            Region parent = regions.get(random.nextInt(regions.size()));
            SubRegion subRegion = SubRegion.builder()
                    .name(randomUgandaPlace() + " SubRegion " + i + " " + faker.random().hex(4))
                    .code(sharedService.generateUniqueCode(AdministrativeAreaType.SUBREGION,
                            code -> subRegionRepository.findByCodeIgnoreCase(code).isPresent()))
                    .areaType(AdministrativeAreaType.SUBREGION)
                    .region(parent)
                    .latitude(randomLatitudeNear(parent.getLatitude()))
                    .longitude(randomLongitudeNear(parent.getLongitude()))
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
            SubRegion parent = subRegions.get(random.nextInt(subRegions.size()));
            LocalGovernment lg = LocalGovernment.builder()
                    .name(randomUgandaPlace() + " LG " + i + " " + faker.random().hex(4))
                    .code(sharedService.generateUniqueCode(AdministrativeAreaType.LOCALGOVERNMENT,
                            code -> localGovernmentRepository.findByCodeIgnoreCase(code).isPresent()))
                    .areaType(AdministrativeAreaType.LOCALGOVERNMENT)
                    .subRegion(parent)
                    .latitude(randomLatitudeNear(parent.getLatitude()))
                    .longitude(randomLongitudeNear(parent.getLongitude()))
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
            LocalGovernment parent = lgs.get(random.nextInt(lgs.size()));
            County county = County.builder()
                    .name(randomUgandaPlace() + " County " + i + " " + faker.random().hex(4))
                    .code(sharedService.generateUniqueCode(AdministrativeAreaType.COUNTY,
                            code -> countyRepository.findByCodeIgnoreCase(code).isPresent()))
                    .areaType(AdministrativeAreaType.COUNTY)
                    .localGovernment(parent)
                    .latitude(randomLatitudeNear(parent.getLatitude()))
                    .longitude(randomLongitudeNear(parent.getLongitude()))
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
            County parent = counties.get(random.nextInt(counties.size()));
            SubCounty sc = SubCounty.builder()
                    .name(randomUgandaPlace() + " SC " + i + " " + faker.random().hex(4))
                    .code(sharedService.generateUniqueCode(AdministrativeAreaType.SUBCOUNTY,
                            code -> subCountyRepository.findByCodeIgnoreCase(code).isPresent()))
                    .areaType(AdministrativeAreaType.SUBCOUNTY)
                    .county(parent)
                    .latitude(randomLatitudeNear(parent.getLatitude()))
                    .longitude(randomLongitudeNear(parent.getLongitude()))
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
            SubCounty parent = subCounties.get(random.nextInt(subCounties.size()));
            Parish parish = Parish.builder()
                    .name(randomUgandaPlace() + " Parish " + i + " " + faker.random().hex(4))
                    .code(sharedService.generateUniqueCode(AdministrativeAreaType.PARISH,
                            code -> parishRepository.findByCodeIgnoreCase(code).isPresent()))
                    .areaType(AdministrativeAreaType.PARISH)
                    .subCounty(parent)
                    .latitude(randomLatitudeNear(parent.getLatitude()))
                    .longitude(randomLongitudeNear(parent.getLongitude()))
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
