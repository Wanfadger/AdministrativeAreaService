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
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public void seedTestData() {
        if (!seedTestData) {
            log.info("Test data seeding is disabled. Set app.seed.test-data=true to enable.");
            return;
        }

        log.info("Starting test data seeding...");

        try {
            // Check if complete data already exists
            boolean hasRegions = !dbRegionService.dbList().isEmpty();
            boolean hasSubRegions = !dbSubRegionService.dbList().isEmpty();
            boolean hasLocalGovernments = !dbLocalGovernmentService.dbList().isEmpty();
            boolean hasCounties = !dbCountyService.dbList().isEmpty();
            boolean hasSubCounties = !dbSubCountyService.dbList().isEmpty();
            boolean hasParishes = !dbParishService.dbList().isEmpty();
            
            boolean hasCompleteData = hasRegions && hasSubRegions && hasLocalGovernments && 
                                     hasCounties && hasSubCounties && hasParishes;
            
            if (hasCompleteData && !clearExisting) {
                log.info("Complete test data already exists. Skipping seed. Set app.seed.clear-existing=true to clear and reseed.");
                return;
            }
            
            if (hasRegions && !clearExisting) {
                log.info("Partial data exists. Will seed missing levels only.");
            }
            
            if (clearExisting) {
                log.warn("app.seed.clear-existing=true is set. This will add new data but may create duplicates if data already exists.");
            }

            // Seed hierarchical data
            List<Region> regions = seedRegions();
            if (regions.isEmpty()) {
                log.error("No regions found after seeding. Cannot proceed with hierarchical seeding.");
                return;
            }
            log.info("Seeded {} regions, proceeding with sub-regions...", regions.size());
            
            List<SubRegion> subRegions = seedSubRegions(regions);
            if (subRegions.isEmpty()) {
                log.warn("No sub-regions were seeded. Check for errors above.");
            } else {
                log.info("Seeded {} sub-regions, proceeding with local governments...", subRegions.size());
            }
            
            List<LocalGovernment> localGovernments = seedLocalGovernments(subRegions);
            if (localGovernments.isEmpty()) {
                log.warn("No local governments were seeded. Check for errors above.");
            } else {
                log.info("Seeded {} local governments, proceeding with counties...", localGovernments.size());
            }
            
            List<County> counties = seedCounties(localGovernments);
            if (counties.isEmpty()) {
                log.warn("No counties were seeded. Check for errors above.");
            } else {
                log.info("Seeded {} counties, proceeding with sub-counties...", counties.size());
            }
            
            List<SubCounty> subCounties = seedSubCounties(counties);
            if (subCounties.isEmpty()) {
                log.warn("No sub-counties were seeded. Check for errors above.");
            } else {
                log.info("Seeded {} sub-counties, proceeding with parishes...", subCounties.size());
            }
            
            List<Parish> parishes = seedParishes(subCounties);
            if (parishes.isEmpty()) {
                log.warn("No parishes were seeded. Check for errors above.");
            } else {
                log.info("Seeded {} parishes.", parishes.size());
            }

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
        log.info("Seeding regions...");
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
                log.debug("Prepared region: {} ({})", regionNames[i], regionCodes[i]);
            } else {
                log.debug("Region {} already exists, skipping", regionNames[i]);
            }
        }

        if (!regions.isEmpty()) {
            log.info("Saving {} new regions to database...", regions.size());
            dbRegionService.dbNew(regions);
            log.info("Successfully saved {} regions", regions.size());
        } else {
            log.info("No new regions to save (all already exist)");
        }

        List<Region> allRegions = dbRegionService.dbList();
        log.info("Total regions in database: {}", allRegions.size());
        return allRegions;
    }

    private List<SubRegion> seedSubRegions(List<Region> regions) {
        log.info("Seeding sub-regions for {} regions...", regions.size());
        if (regions.isEmpty()) {
            log.warn("No regions provided for sub-region seeding");
            return new ArrayList<>();
        }
        
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
                    log.debug("Prepared sub-region: {} ({}) for region {}", name, code, region.getCode());
                } else {
                    log.debug("Sub-region {} already exists for region {}, skipping", name, region.getCode());
                }
                subRegionIndex++;
            }
        }

        if (!subRegions.isEmpty()) {
            log.info("Saving {} new sub-regions to database...", subRegions.size());
            dbSubRegionService.dbNew(subRegions);
            log.info("Successfully saved {} sub-regions", subRegions.size());
        } else {
            log.info("No new sub-regions to save (all already exist)");
        }

        List<SubRegion> allSubRegions = dbSubRegionService.dbList().stream().filter(sr -> sr.getRegion() != null).toList();
        log.info("Total sub-regions in database: {}", allSubRegions.size());
        return allSubRegions;
    }

    private List<LocalGovernment> seedLocalGovernments(List<SubRegion> subRegions) {
        log.info("Seeding local governments for {} sub-regions...", subRegions.size());
        if (subRegions.isEmpty()) {
            log.warn("No sub-regions provided for local government seeding");
            return new ArrayList<>();
        }
        
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
                    log.debug("Prepared local government: {} ({}) for sub-region {}", name, code, subRegion.getCode());
                } else {
                    log.debug("Local government {} already exists for sub-region {}, skipping", name, subRegion.getCode());
                }
                lgIndex++;
            }
        }

        if (!localGovernments.isEmpty()) {
            log.info("Saving {} new local governments to database...", localGovernments.size());
            dbLocalGovernmentService.dbNew(localGovernments);
            log.info("Successfully saved {} local governments", localGovernments.size());
        } else {
            log.info("No new local governments to save (all already exist)");
        }

        List<LocalGovernment> allLocalGovernments = dbLocalGovernmentService.dbList().stream().filter(lg -> lg.getSubRegion() != null).toList();
        log.info("Total local governments in database: {}", allLocalGovernments.size());
        return allLocalGovernments;
    }

    private List<County> seedCounties(List<LocalGovernment> localGovernments) {
        log.info("Seeding counties for {} local governments...", localGovernments.size());
        if (localGovernments.isEmpty()) {
            log.warn("No local governments provided for county seeding");
            return new ArrayList<>();
        }
        
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
                    log.debug("Prepared county: {} ({}) for local government {}", name, code, localGovernment.getCode());
                } else {
                    log.debug("County {} already exists for local government {}, skipping", name, localGovernment.getCode());
                }
                countyIndex++;
            }
        }

        if (!counties.isEmpty()) {
            log.info("Saving {} new counties to database...", counties.size());
            dbCountyService.dbNew(counties);
            log.info("Successfully saved {} counties", counties.size());
        } else {
            log.info("No new counties to save (all already exist)");
        }

        List<County> allCounties = dbCountyService.dbList().stream().filter(c -> c.getLocalGovernment() != null).toList();
        log.info("Total counties in database: {}", allCounties.size());
        return allCounties;
    }

    private List<SubCounty> seedSubCounties(List<County> counties) {
        log.info("Seeding sub-counties for {} counties...", counties.size());
        if (counties.isEmpty()) {
            log.warn("No counties provided for sub-county seeding");
            return new ArrayList<>();
        }
        
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
                    log.debug("Prepared sub-county: {} ({}) for county {}", name, code, county.getCode());
                } else {
                    log.debug("Sub-county {} already exists for county {}, skipping", name, county.getCode());
                }
                subCountyIndex++;
            }
        }

        if (!subCounties.isEmpty()) {
            log.info("Saving {} new sub-counties to database...", subCounties.size());
            dbSubCountyService.dbNew(subCounties);
            log.info("Successfully saved {} sub-counties", subCounties.size());
        } else {
            log.info("No new sub-counties to save (all already exist)");
        }

        List<SubCounty> allSubCounties = dbSubCountyService.dbList().stream().filter(sc -> sc.getCounty() != null).toList();
        log.info("Total sub-counties in database: {}", allSubCounties.size());
        return allSubCounties;
    }

    private List<Parish> seedParishes(List<SubCounty> subCounties) {
        log.info("Seeding parishes for {} sub-counties...", subCounties.size());
        if (subCounties.isEmpty()) {
            log.warn("No sub-counties provided for parish seeding");
            return new ArrayList<>();
        }
        
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
                    log.debug("Prepared parish: {} ({}) for sub-county {}", name, code, subCounty.getCode());
                } else {
                    log.debug("Parish {} already exists for sub-county {}, skipping", name, subCounty.getCode());
                }
                parishIndex++;
            }
        }

        if (!parishes.isEmpty()) {
            log.info("Saving {} new parishes to database...", parishes.size());
            dbParishService.dbNew(parishes);
            log.info("Successfully saved {} parishes", parishes.size());
        } else {
            log.info("No new parishes to save (all already exist)");
        }

        List<Parish> allParishes = dbParishService.dbList().stream().filter(p -> p.getSubCounty() != null).toList();
        log.info("Total parishes in database: {}", allParishes.size());
        return allParishes;
    }
}
