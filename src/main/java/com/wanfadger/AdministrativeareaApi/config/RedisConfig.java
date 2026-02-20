package com.wanfadger.AdministrativeareaApi.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {

        private final ObjectMapper objectMapper;

        private GenericJacksonJsonRedisSerializer getJsonSerializer() {
                ObjectMapper redisMapper = objectMapper.rebuild()
                                .activateDefaultTyping(
                                                BasicPolymorphicTypeValidator.builder()
                                                                .allowIfBaseType(Object.class)
                                                                .build(),
                                                DefaultTyping.NON_FINAL,
                                                JsonTypeInfo.As.PROPERTY)
                                .build();
                return new GenericJacksonJsonRedisSerializer(redisMapper);
        }

        @Bean
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
                RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
                redisTemplate.setConnectionFactory(redisConnectionFactory);

                GenericJacksonJsonRedisSerializer serializer = getJsonSerializer();

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
                GenericJacksonJsonRedisSerializer serializer = getJsonSerializer();

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
                GenericJacksonJsonRedisSerializer serializer = getJsonSerializer();

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
