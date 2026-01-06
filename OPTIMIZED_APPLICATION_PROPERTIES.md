# Optimized Application Properties for High Traffic

## Database Connection Pool Optimization

```properties
# HikariCP Connection Pool - Optimized for High Traffic
spring.datasource.hikari.pool-name=SIIP-AREA-POOL
spring.datasource.hikari.maximum-pool-size=100
spring.datasource.hikari.minimum-idle=20
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.leak-detection-threshold=60000
spring.datasource.hikari.register-mbeans=true

# Enable batch processing for bulk operations
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
spring.jpa.properties.hibernate.jdbc.batch_versioned_data=true

# Query optimization
spring.jpa.properties.hibernate.generate_statistics=false
spring.jpa.properties.hibernate.use_sql_comments=false
```

## Redis Configuration

```properties
# Redis Connection Pool (configured in Java config)
spring.data.redis.database=${REDIS-DATABASE:0}
spring.data.redis.host=${REDIS-HOST:localhost}
spring.data.redis.port=${REDIS-PORT:80034}
spring.cache.redis.cache-null-values=${REDIS-CACHE-NULL:false}

# Redis timeout settings
spring.data.redis.timeout=2000
spring.data.redis.lettuce.pool.max-active=200
spring.data.redis.lettuce.pool.max-idle=50
spring.data.redis.lettuce.pool.min-idle=10
```

## Server Configuration

```properties
# Server settings
server.port=${APP-PORT:8084}
server.compression.enabled=true
server.compression.mime-types=application/json,application/xml,text/html,text/xml,text/plain
server.compression.min-response-size=1024

# Tomcat thread pool (if using embedded Tomcat)
server.tomcat.threads.max=200
server.tomcat.threads.min-spare=20
server.tomcat.accept-count=100
server.tomcat.max-connections=10000
```

## JVM Optimization (for production)

Add these JVM options when running the application:

```bash
-Xms2g -Xmx4g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UseStringDeduplication
-XX:+OptimizeStringConcat
-XX:+UseCompressedOops
-XX:+UseCompressedClassPointers
```

## Monitoring & Actuator

```properties
# Spring Boot Actuator
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=when-authorized
management.metrics.export.prometheus.enabled=true
management.metrics.distribution.percentiles-histogram.http.server.requests=true
management.metrics.distribution.sla.http.server.requests=100ms,500ms,1s,2s
```

## Logging Configuration

```properties
# Logging levels
logging.level.root=INFO
logging.level.com.wanfadger.AdministrativeareaApi=DEBUG
logging.level.org.springframework.web=INFO
logging.level.org.hibernate.SQL=WARN
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=WARN

# Logging pattern (JSON for log aggregation)
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
```

## Virtual Threads (Java 21)

```properties
# Virtual threads are already enabled
spring.threads.virtual.enabled=true
```

## Performance Tuning

```properties
# Disable unnecessary features in production
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
spring.jpa.properties.hibernate.use_sql_comments=false

# Enable second-level cache (if using)
spring.jpa.properties.hibernate.cache.use_second_level_cache=false
spring.jpa.properties.hibernate.cache.use_query_cache=false
```
