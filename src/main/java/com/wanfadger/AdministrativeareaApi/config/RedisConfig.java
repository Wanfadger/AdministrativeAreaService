package com.wanfadger.AdministrativeareaApi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {

        private final ObjectMapper objectMapper;

        private GenericJackson2JsonRedisSerializer getJsonSerializer() {
                return new GenericJackson2JsonRedisSerializer(objectMapper);
        }

        @Bean
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
                RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
                redisTemplate.setConnectionFactory(redisConnectionFactory);

                GenericJackson2JsonRedisSerializer serializer = getJsonSerializer();

                redisTemplate.setKeySerializer(new StringRedisSerializer());
                redisTemplate.setValueSerializer(serializer);
                redisTemplate.setHashKeySerializer(new StringRedisSerializer());
                redisTemplate.setHashValueSerializer(serializer);
                redisTemplate.afterPropertiesSet();
                return redisTemplate;
        }

        @Bean
        @Primary
        public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
                GenericJackson2JsonRedisSerializer serializer = getJsonSerializer();

                RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                                .serializeKeysWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(serializer))
                                .entryTtl(Duration.ofHours(1))
                                .disableCachingNullValues();

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(config)
                                .build();
        }
}
