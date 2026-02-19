package com.wanfadger.AdministrativeareaApi.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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

        private GenericJackson2JsonRedisSerializer getJsonSerializer() {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                objectMapper.activateDefaultTyping(
                                objectMapper.getPolymorphicTypeValidator(),
                                ObjectMapper.DefaultTyping.NON_FINAL,
                                JsonTypeInfo.As.PROPERTY);
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

        @Bean("searchCacheManager")
        public RedisCacheManager searchCacheManager(RedisConnectionFactory connectionFactory) {
                GenericJackson2JsonRedisSerializer serializer = getJsonSerializer();

                RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                                .serializeKeysWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(serializer))
                                .entryTtl(Duration.ofMinutes(30))
                                .disableCachingNullValues();

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(config)
                                .build();
        }

        @Bean("sortedMapKeyGenerator")
        public org.springframework.cache.interceptor.KeyGenerator keyGenerator() {
                return (target, method, params) -> {
                        if (params.length > 0 && params[0] instanceof java.util.Map) {
                                @SuppressWarnings("unchecked")
                                java.util.Map<String, String> queryMap = (java.util.Map<String, String>) params[0];
                                return queryMap.entrySet().stream()
                                                .sorted(java.util.Map.Entry.comparingByKey())
                                                .map(entry -> entry.getKey() + "=" + entry.getValue())
                                                .collect(java.util.stream.Collectors.joining("&"));
                        }
                        // Fallback for non-map parameters
                        return org.springframework.cache.interceptor.SimpleKeyGenerator.generateKey(params);
                };
        }

}
