package com.wanfadger.AdministrativeareaApi.cache;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A cache hit must not touch the database at all — not even to open and close an empty transaction.
 *
 * <p>It used to. {@code @Cacheable} and {@code @Transactional(readOnly = true)} sat on the same
 * method, and their two AOP advisors <b>both</b> default to {@code LOWEST_PRECEDENCE}, so which one
 * wrapped the other was decided arbitrarily rather than by intent. With the transaction interceptor
 * outermost, every cache hit checked a connection out of the Hikari pool, began a transaction, found
 * the value already cached, and committed a transaction that had done nothing.
 *
 * <p>That is worth a test of its own rather than a comment, because it is invisible: the endpoint
 * returns the right answer quickly, the cache hit-rate metric reads 100%, and the only symptom is
 * that the connection pool — the actual throughput ceiling of this service, since virtual threads
 * removed the thread-pool ceiling — saturates under load for no reason anybody can see.
 *
 * <p>The fix is structural: the cache lives in {@code AdministrativeAreaCachingService}, a separate
 * bean in front of the transactional one. No advisor ordering can undo that.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:connectiondb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class CacheHitTakesNoConnectionTest {

    private static final String BASE = "/api/v1/administrative-areas";

    @Autowired MockMvc mockMvc;

    @Test
    void aCacheHit_opensNoDatabaseConnection() throws Exception {
        mockMvc.perform(post(BASE).param("type", "REGION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"name\":\"Connection Counting Region\"}]"))
                .andExpect(status().isCreated());

        // Cold: this one must hit the database.
        CountingDataSource.CONNECTIONS.set(0);
        mockMvc.perform(get(BASE + "/search").param("type", "REGION")).andExpect(status().isOk());
        int cold = CountingDataSource.CONNECTIONS.get();
        assertThat(cold).as("a cold read must query the database").isPositive();

        // Warm: byte-identical request, so it resolves to the same cache key.
        CountingDataSource.CONNECTIONS.set(0);
        mockMvc.perform(get(BASE + "/search").param("type", "REGION")).andExpect(status().isOk());

        assertThat(CountingDataSource.CONNECTIONS.get())
                .as("a cache hit must not check out a connection — not even for an empty transaction")
                .isZero();
    }

    @Test
    void aGetOneCacheHit_opensNoDatabaseConnection() throws Exception {
        String response = mockMvc.perform(post(BASE).param("type", "REGION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"name\":\"Connection Counting GetOne\"}]"))
                .andReturn().getResponse().getContentAsString();
        String code = response.replaceAll("(?s).*\"data\"\\s*:\\s*\\[\\s*\"([^\"]+)\".*", "$1");

        mockMvc.perform(get(BASE + "/" + code).param("type", "REGION")).andExpect(status().isOk());

        CountingDataSource.CONNECTIONS.set(0);
        mockMvc.perform(get(BASE + "/" + code).param("type", "REGION")).andExpect(status().isOk());

        assertThat(CountingDataSource.CONNECTIONS.get()).isZero();
    }

    /**
     * Wraps the real {@link DataSource} rather than replacing it. A {@code @Primary DataSource} bean
     * declared here would both try to inject itself and cause {@code DataSourceAutoConfiguration}
     * (which is {@code @ConditionalOnMissingBean(DataSource.class)}) to back off, leaving nothing to
     * wrap.
     */
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

    /** Counts every checkout of a connection from the pool. */
    static class CountingDataSource extends DelegatingDataSource {
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
