package com.wanfadger.AdministrativeareaApi.cache;

import com.wanfadger.AdministrativeareaApi.beanConfig.CacheValueKeyConfig;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Eviction must cascade <b>downward and only downward</b>.
 *
 * <p>The old code evicted {@code allEntries = true} on both cache regions for every write, so renaming
 * one parish threw away every cached page at all six levels. On a read-almost-only dataset that is a
 * cache which is coldest exactly when it is most needed — right after the monthly data load.
 *
 * <p>The direction matters and is not symmetric. Every DTO embeds its <b>ancestry</b>: a
 * {@code ParishDTO} carries its sub-county, county, local government, sub-region and region. So
 * renaming a region really does change the bytes of every cached parish, and those caches must go.
 * Nothing embeds its <b>children</b> — a {@code SubCountyDTO} has no parish list — so a parish write
 * cannot change a single cached sub-county, and clearing them would be pure waste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:cascadedb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class EvictionCascadeTest {

    private static final String BASE = "/api/v1/administrative-areas";

    @Autowired MockMvc mockMvc;
    @Autowired TwoLevelCacheManager cacheManager;

    /** One area per level, each the child of the one above. Rebuilt per test — JUnit makes a fresh
     *  test instance per method, while the Spring context (and its H2 database) is shared. */
    private final Map<AdministrativeAreaType, String> codes = new EnumMap<>(AdministrativeAreaType.class);

    /** Names must not collide with the previous test method's hierarchy: the duplicate check is real. */
    private final String tag = java.util.UUID.randomUUID().toString().substring(0, 8);

    @BeforeEach
    void seedAndWarm() throws Exception {
        String parent = null;
        for (AdministrativeAreaType type : AdministrativeAreaType.values()) {
            parent = create(type, "Cascade " + type.name() + " " + tag, parent);
            codes.put(type, parent);
        }
        // Warm AFTER seeding: the creates themselves evict.
        warmEveryRegion();
    }

    private String create(AdministrativeAreaType type, String name, String partOfCode) throws Exception {
        String body = partOfCode == null
                ? "[{\"name\":\"" + name + "\"}]"
                : "[{\"name\":\"" + name + "\",\"partOfCode\":\"" + partOfCode + "\"}]";

        String response = mockMvc.perform(post(BASE).param("type", type.name())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return response.replaceAll("(?s).*\"data\"\\s*:\\s*\\[\\s*\"([^\"]+)\".*", "$1");
    }

    /** Populate all twelve regions: one search and one getOne per level. */
    private void warmEveryRegion() throws Exception {
        for (AdministrativeAreaType type : AdministrativeAreaType.values()) {
            mockMvc.perform(get(BASE + "/search").param("type", type.name()));
            mockMvc.perform(get(BASE + "/" + codes.get(type)).param("type", type.name()));
        }
        assertThat(populatedRegions())
                .as("warm-up should have filled every region")
                .hasSize(12);
    }

    /** Regions currently holding at least one entry, read straight off the Caffeine tier. */
    private java.util.Set<String> populatedRegions() {
        return cacheManager.allCaches().stream()
                .filter(c -> {
                    c.getNativeCache().cleanUp();   // settle pending eviction work before measuring
                    return c.getNativeCache().estimatedSize() > 0;
                })
                .map(TwoLevelCache::getName)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private void rename(AdministrativeAreaType type, String newName) throws Exception {
        AdministrativeAreaType[] all = AdministrativeAreaType.values();
        String partOfCode = type.ordinal() == 0 ? null : codes.get(all[type.ordinal() - 1]);

        String body = partOfCode == null
                ? "{\"name\":\"" + newName + "\"}"
                : "{\"name\":\"" + newName + "\",\"partOfCode\":\"" + partOfCode + "\"}";

        mockMvc.perform(put(BASE + "/" + codes.get(type)).param("type", type.name())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** The leaf. Nothing embeds a parish, so nothing else can be stale. */
    @Test
    void writingAParish_clearsOnlyTheTwoParishRegions() throws Exception {
        rename(AdministrativeAreaType.PARISH, "Renamed Parish");

        assertThat(populatedRegions())
                .as("the other ten regions must survive a parish rename")
                .containsExactlyInAnyOrderElementsOf(
                        remainingAfterClearing(AdministrativeAreaType.PARISH))
                .hasSize(10);
    }

    /** The root. Every level embeds the region, so every level is now stale. */
    @Test
    void writingARegion_clearsAllTwelveRegions() throws Exception {
        rename(AdministrativeAreaType.REGION, "Renamed Region");

        assertThat(populatedRegions())
                .as("a region rename changes the bytes of every cached DTO at every level")
                .isEmpty();
    }

    /** The interesting middle case: three levels below it go, two levels above it stay. */
    @Test
    void writingACounty_clearsCountyAndBelow_butNotItsAncestors() throws Exception {
        rename(AdministrativeAreaType.COUNTY, "Renamed County");

        assertThat(populatedRegions())
                .as("REGION, SUBREGION and LOCALGOVERNMENT are unaffected by a county rename")
                .containsExactlyInAnyOrder(
                        CacheValueKeyConfig.item(AdministrativeAreaType.REGION),
                        CacheValueKeyConfig.search(AdministrativeAreaType.REGION),
                        CacheValueKeyConfig.item(AdministrativeAreaType.SUBREGION),
                        CacheValueKeyConfig.search(AdministrativeAreaType.SUBREGION),
                        CacheValueKeyConfig.item(AdministrativeAreaType.LOCALGOVERNMENT),
                        CacheValueKeyConfig.search(AdministrativeAreaType.LOCALGOVERNMENT));
    }

    /** A delete invalidates exactly as a write does — it is the same cascade. */
    @Test
    void deletingAParish_clearsOnlyTheTwoParishRegions() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete(BASE + "/" + codes.get(AdministrativeAreaType.PARISH))
                .param("type", "PARISH"));

        assertThat(populatedRegions()).hasSize(10);
    }

    private java.util.Set<String> remainingAfterClearing(AdministrativeAreaType written) {
        java.util.Set<String> remaining = new java.util.LinkedHashSet<>(CacheValueKeyConfig.allCacheNames());
        remaining.removeAll(CacheValueKeyConfig.cascadeFrom(written));
        return remaining;
    }
}
