package com.wanfadger.AdministrativeareaApi.cache;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Warmup is only worth anything if it warms <b>the keys that will actually be read</b>.
 *
 * <p>This is the single way the feature fails silently. A warmup that builds its own query map —
 * even one that looks right — produces a different cache key from the one the controller looks up.
 * The warmed entries then sit unread until they expire, every real request misses, the hit rate
 * stays at zero, and the startup log says "warmed 39 entries". There is no symptom except the
 * absence of the benefit, which is why asserting "the runner ran" or "the cache is non-empty" would
 * be worthless here: both would pass with every key wrong.
 *
 * <p>So the assertion is behavioural and end-to-end: after startup, the <b>HTTP endpoint</b> a client
 * calls must serve its very first request <b>without touching the database</b>.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:warmupdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "app.cache.warmup.enabled=true",
        "app.cache.warmup.map-size=2000",
        "app.cache.warmup.table-size=25",
        "app.cache.warmup.tree-size=1000"
})
class CacheWarmupTest {

    private static final String BASE = "/api/v1/administrative-areas";

    @Autowired MockMvc mockMvc;

    /** The API default page — the likeliest request an integrating service will ever make. */
    @Test
    void theApiDefaultPage_isAlreadyWarm() throws Exception {
        CountingDataSource.CONNECTIONS.set(0);

        mockMvc.perform(get(BASE + "/search").param("type", "REGION"))
                .andExpect(status().isOk());

        assertThat(CountingDataSource.CONNECTIONS.get())
                .as("warmup already cached this exact key; the first real request must not query the DB")
                .isZero();
    }

    /** The map's 2,000-row pull, per level — the entry a cold pod most needs. */
    @Test
    void theMapPull_isAlreadyWarm() throws Exception {
        CountingDataSource.CONNECTIONS.set(0);

        mockMvc.perform(get(BASE + "/search")
                        .param("type", "PARISH")
                        .param("size", "2000")
                        .param("sortBy", "name")
                        .param("view", "flat"))
                .andExpect(status().isOk());

        assertThat(CountingDataSource.CONNECTIONS.get()).isZero();
    }

    /** The explorer's landing table: page 1, 25 rows, flat. */
    @Test
    void theExplorerLandingPage_isAlreadyWarm() throws Exception {
        CountingDataSource.CONNECTIONS.set(0);

        mockMvc.perform(get(BASE + "/search")
                        .param("type", "REGION")
                        .param("page", "1").param("size", "25")
                        .param("sortBy", "name").param("sortDirection", "asc")
                        .param("view", "flat"))
                .andExpect(status().isOk());

        assertThat(CountingDataSource.CONNECTIONS.get()).isZero();
    }

    /**
     * The control, and the reason the zeroes above mean anything.
     *
     * <p>Without this, every assertion in this class would also pass if the application simply never
     * opened a connection for any read. A size nobody warms must still reach the database.
     */
    @Test
    void aSizeNobodyWarmed_stillQueriesTheDatabase() throws Exception {
        CountingDataSource.CONNECTIONS.set(0);

        mockMvc.perform(get(BASE + "/search").param("type", "REGION").param("size", "137"))
                .andExpect(status().isOk());

        assertThat(CountingDataSource.CONNECTIONS.get())
                .as("size=137 is not in the warmup plan, so it must be a genuine cache miss")
                .isPositive();
    }

    @TestConfiguration
    static class CountingDataSourceConfig {
        @Bean
        static BeanPostProcessor countingDataSourceWrapper() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    if (bean instanceof DataSource ds && !(bean instanceof CountingDataSource)) {
                        return new CountingDataSource(ds);
                    }
                    return bean;
                }
            };
        }
    }

    static class CountingDataSource extends org.springframework.jdbc.datasource.DelegatingDataSource {
        static final AtomicInteger CONNECTIONS = new AtomicInteger();

        CountingDataSource(DataSource target) {
            super(target);
        }

        @Override
        public Connection getConnection() throws SQLException {
            CONNECTIONS.incrementAndGet();
            return super.getConnection();
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            CONNECTIONS.incrementAndGet();
            return super.getConnection(username, password);
        }
    }
}
