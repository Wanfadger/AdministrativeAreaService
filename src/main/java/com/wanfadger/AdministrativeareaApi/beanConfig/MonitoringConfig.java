package com.wanfadger.AdministrativeareaApi.beanConfig;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Configuration for monitoring and health checks
 */
@Configuration
public class MonitoringConfig {

    /**
     * Database health indicator
     */
    @Bean
    public HealthIndicator databaseHealthIndicator(DataSource dataSource) {
        return () -> {
            try (Connection connection = dataSource.getConnection()) {
                if (connection.isValid(2)) {
                    return Health.up()
                            .withDetail("database", "PostgreSQL")
                            .withDetail("status", "Connected")
                            .build();
                } else {
                    return Health.down()
                            .withDetail("database", "PostgreSQL")
                            .withDetail("status", "Connection invalid")
                            .build();
                }
            } catch (SQLException e) {
                return Health.down()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }

    /**
     * Redis health indicator
     */
    @Bean
    public HealthIndicator redisHealthIndicator(RedisConnectionFactory redisConnectionFactory) {
        return () -> {
            try {
                redisConnectionFactory.getConnection().ping();
                return Health.up()
                        .withDetail("cache", "Redis")
                        .withDetail("status", "Connected")
                        .build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("cache", "Redis")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }

    /**
     * Cache health indicator
     */
    @Bean
    public HealthIndicator cacheHealthIndicator(CacheManager cacheManager) {
        return () -> {
            try {
                if (cacheManager != null) {
                    return Health.up()
                            .withDetail("cacheManager", cacheManager.getClass().getSimpleName())
                            .withDetail("status", "Available")
                            .build();
                } else {
                    return Health.down()
                            .withDetail("cacheManager", "null")
                            .withDetail("status", "Not configured")
                            .build();
                }
            } catch (Exception e) {
                return Health.down()
                        .withDetail("cacheManager", "Error")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }
}
