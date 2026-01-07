package com.wanfadger.AdministrativeareaApi.shared.util;

import com.wanfadger.AdministrativeareaApi.entity.*;
import com.wanfadger.AdministrativeareaApi.service.county.DbCountyService;
import com.wanfadger.AdministrativeareaApi.service.localgovernment.DbLocalGovernmentService;
import com.wanfadger.AdministrativeareaApi.service.parish.DbParishService;
import com.wanfadger.AdministrativeareaApi.service.region.DbRegionService;
import com.wanfadger.AdministrativeareaApi.service.subcounty.DbSubCountyService;
import com.wanfadger.AdministrativeareaApi.service.subRegion.DbSubRegionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Test Data Seeder
 * 
 * Seeds test data for Swagger testing and load testing.
 * Can be enabled/disabled via application properties.
 * 
 * Creates a hierarchical structure:
 * - 5 Regions
 * - 15 Sub-Regions (3 per region)
 * - 45 Local Governments (3 per sub-region)
 * - 135 Counties (3 per local government)
 * - 405 Sub-Counties (3 per county)
 * - 1215 Parishes (3 per sub-county)
 * 
 * Total: 1,820 administrative areas
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TestDataSeeder {

    private final DbRegionService dbRegionService;
    private final DbSubRegionService dbSubRegionService;
    private final DbLocalGovernmentService dbLocalGovernmentService;
    private final DbCountyService dbCountyService;
    private final DbSubCountyService dbSubCountyService;
    private final DbParishService dbParishService;

    @Value("${app.seed.test-data:false}")
    private boolean seedTestData;

    @Value("${app.seed.clear-existing:false}")
    private boolean clearExisting;

    @EventListener(ApplicationReadyEvent.class)
    public void seedTestData() {
        if (!seedTestData) {
            log.info("Test data seeding is disabled. Set app.seed.test-data=true to enable.");
            return;
        }

        log.info("Starting test data seeding...");

        try {
            // Check if data already exists
            if (!dbRegionService.dbList().isEmpty() && !clearExisting) {
                log.info("Data already exists. Skipping seed. Set app.seed.clear-existing=true to clear and reseed.");
                return;
            }

            // Seed hierarchical data
            List<Region> regions = seedRegions();
            List<SubRegion> subRegions = seedSubRegions(regions);
            List<LocalGovernment> localGovernments = seedLocalGovernments(subRegions);
            List<County> counties = seedCounties(localGovernments);
            List<SubCounty> subCounties = seedSubCounties(counties);
            List<Parish> parishes = seedParishes(subCounties);

            log.info("Test data seeding completed successfully:");
            log.info("  - {} Regions", regions.size());
            log.info("  - {} Sub-Regions", subRegions.size());
            log.info("  - {} Local Governments", localGovernments.size());
            log.info("  - {} Counties", counties.size());
            log.info("  - {} Sub-Counties", subCounties.size());
            log.info("  - {} Parishes", parishes.size());
            log.info("Total: {} administrative areas", 
                    regions.size() + subRegions.size() + localGovernments.size() + 
                    counties.size() + subCounties.size() + parishes.size());

        } catch (Exception e) {
            log.error("Error seeding test data: {}", e.getMessage(), e);
        }
    }

    private List<Region> seedRegions() {
        log.debug("Seeding regions...");
        List<Region> regions = new ArrayList<>();

        // Create 5 regions with realistic Ugandan region data
        String[] regionNames = {
            "Central Region", 
            "Northern Region", 
            "Eastern Region",
            "Western Region",
            "Southern Region"
        };
        String[] regionCodes = {"REG001", "REG002", "REG003", "REG004", "REG005"};
        Double[] latitudes = {0.3476, 2.5000, 1.3733, 0.2833, -0.6167};
        Double[] longitudes = {32.5825, 32.5000, 33.2041, 30.2667, 30.6667};

        for (int i = 0; i < regionNames.length; i++) {
            if (dbRegionService.dbByName(regionNames[i]).isEmpty()) {
                Region region = new Region();
                region.setCode(regionCodes[i]);
                region.setName(regionNames[i]);
                region.setDescription("Test " + regionNames[i] + " for testing purposes");
                region.setLatitude(latitudes[i]);
                region.setLongitude(longitudes[i]);
                regions.add(region);
            }
        }

        if (!regions.isEmpty()) {
            dbRegionService.dbNew(regions);
        }

        return dbRegionService.dbList();
    }

    private List<SubRegion> seedSubRegions(List<Region> regions) {
        log.debug("Seeding sub-regions...");
        List<SubRegion> subRegions = new ArrayList<>();

        int subRegionIndex = 1;
        for (Region region : regions) {
            // Create 3 sub-regions per region
            for (int i = 1; i <= 3; i++) {
                String name = region.getName() + " Sub-Region " + i;
                String code = "SR" + String.format("%03d", subRegionIndex);

                if (dbSubRegionService.dbByName_RegionCode(name, region.getCode()).isEmpty()) {
                    SubRegion subRegion = new SubRegion();
                    subRegion.setCode(code);
                    subRegion.setName(name);
                    subRegion.setDescription("Test " + name + " - maintains hierarchical reference to " + region.getName());
                    subRegion.setRegion(region);
                    subRegion.setLatitude(region.getLatitude() + (i * 0.1));
                    subRegion.setLongitude(region.getLongitude() + (i * 0.1));
                    subRegions.add(subRegion);
                }
                subRegionIndex++;
            }
        }

        if (!subRegions.isEmpty()) {
            dbSubRegionService.dbNew(subRegions);
        }

        return dbSubRegionService.dbList().stream().filter(sr -> sr.getRegion() != null).toList();
    }

    private List<LocalGovernment> seedLocalGovernments(List<SubRegion> subRegions) {
        log.debug("Seeding local governments...");
        List<LocalGovernment> localGovernments = new ArrayList<>();

        int lgIndex = 1;
        for (SubRegion subRegion : subRegions) {
            // Create 3 local governments per sub-region
            for (int i = 1; i <= 3; i++) {
                String name = subRegion.getName() + " Local Government " + i;
                String code = "LG" + String.format("%03d", lgIndex);

                if (dbLocalGovernmentService.dbByName_SubRegionCode(name, subRegion.getCode()).isEmpty()) {
                    LocalGovernment localGovernment = new LocalGovernment();
                    localGovernment.setCode(code);
                    localGovernment.setName(name);
                    localGovernment.setDescription("Test " + name + " - maintains hierarchical reference to " + subRegion.getName());
                    localGovernment.setSubRegion(subRegion);
                    localGovernment.setLatitude(subRegion.getLatitude() + (i * 0.05));
                    localGovernment.setLongitude(subRegion.getLongitude() + (i * 0.05));
                    localGovernments.add(localGovernment);
                }
                lgIndex++;
            }
        }

        if (!localGovernments.isEmpty()) {
            dbLocalGovernmentService.dbNew(localGovernments);
        }

        return dbLocalGovernmentService.dbList().stream().filter(lg -> lg.getSubRegion() != null).toList();
    }

    private List<County> seedCounties(List<LocalGovernment> localGovernments) {
        log.debug("Seeding counties...");
        List<County> counties = new ArrayList<>();

        int countyIndex = 1;
        for (LocalGovernment localGovernment : localGovernments) {
            // Create 3 counties per local government
            for (int i = 1; i <= 3; i++) {
                String name = localGovernment.getName() + " County " + i;
                String code = "CT" + String.format("%03d", countyIndex);

                if (dbCountyService.dbByName_LocalGovernment_Code(name, localGovernment.getCode()).isEmpty()) {
                    County county = new County();
                    county.setCode(code);
                    county.setName(name);
                    county.setDescription("Test " + name + " - maintains hierarchical reference to " + localGovernment.getName());
                    county.setLocalGovernment(localGovernment);
                    county.setLatitude(localGovernment.getLatitude() + (i * 0.02));
                    county.setLongitude(localGovernment.getLongitude() + (i * 0.02));
                    counties.add(county);
                }
                countyIndex++;
            }
        }

        if (!counties.isEmpty()) {
            dbCountyService.dbNew(counties);
        }

        return dbCountyService.dbList().stream().filter(c -> c.getLocalGovernment() != null).toList();
    }

    private List<SubCounty> seedSubCounties(List<County> counties) {
        log.debug("Seeding sub-counties...");
        List<SubCounty> subCounties = new ArrayList<>();

        int subCountyIndex = 1;
        for (County county : counties) {
            // Create 3 sub-counties per county
            for (int i = 1; i <= 3; i++) {
                String name = county.getName() + " Sub-County " + i;
                String code = "SC" + String.format("%03d", subCountyIndex);

                if (dbSubCountyService.dbByName_CountyCode(name, county.getCode()).isEmpty()) {
                    SubCounty subCounty = new SubCounty();
                    subCounty.setCode(code);
                    subCounty.setName(name);
                    subCounty.setDescription("Test " + name + " - maintains hierarchical reference to " + county.getName());
                    subCounty.setCounty(county);
                    subCounty.setLatitude(county.getLatitude() + (i * 0.01));
                    subCounty.setLongitude(county.getLongitude() + (i * 0.01));
                    subCounties.add(subCounty);
                }
                subCountyIndex++;
            }
        }

        if (!subCounties.isEmpty()) {
            dbSubCountyService.dbNew(subCounties);
        }

        return dbSubCountyService.dbList().stream().filter(sc -> sc.getCounty() != null).toList();
    }

    private List<Parish> seedParishes(List<SubCounty> subCounties) {
        log.debug("Seeding parishes...");
        List<Parish> parishes = new ArrayList<>();

        int parishIndex = 1;
        for (SubCounty subCounty : subCounties) {
            // Create 3 parishes per sub-county
            for (int i = 1; i <= 3; i++) {
                String name = subCounty.getName() + " Parish " + i;
                String code = "PR" + String.format("%03d", parishIndex);

                if (dbParishService.dbByName_SubCountyCode(name, subCounty.getCode()).isEmpty()) {
                    Parish parish = new Parish();
                    parish.setCode(code);
                    parish.setName(name);
                    parish.setDescription("Test " + name + " - maintains hierarchical reference to " + subCounty.getName());
                    parish.setSubCounty(subCounty);
                    parish.setLatitude(subCounty.getLatitude() + (i * 0.005));
                    parish.setLongitude(subCounty.getLongitude() + (i * 0.005));
                    parishes.add(parish);
                }
                parishIndex++;
            }
        }

        if (!parishes.isEmpty()) {
            dbParishService.dbNew(parishes);
        }

        return dbParishService.dbList().stream().filter(p -> p.getSubCounty() != null).toList();
    }
}
