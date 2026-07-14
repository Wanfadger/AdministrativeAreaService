package com.wanfadger.AdministrativeareaApi.cache;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.support.NullValue;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.ClassUtils;

/**
 * How cached values are written to and read back from Redis.
 *
 * <p>Two requirements that pull against each other.
 *
 * <h2>1. Type information must be written</h2>
 *
 * Cached values are generic — {@code ResponseDTO<T>}, {@code PaginatedResponseDTO<T>} whose {@code T}
 * is one of {@code RegionDTO … ParishDTO}. Generics are erased, so at read time the declared type
 * tells Jackson nothing about which subtype it is looking at, and a value written as a
 * {@code ParishDTO} comes back as a {@code LinkedHashMap}. The {@code @class} hint written alongside
 * each value is what makes the round-trip faithful.
 *
 * <p>This is a property of the <b>Redis wire format only</b>. It is deliberately not achieved by
 * putting {@code @JsonTypeInfo} on the DTO classes: that would add a type property to the <i>HTTP</i>
 * JSON too, and every existing client would see a changed response shape. The HTTP response is
 * rendered by Boot's own auto-configured {@code ObjectMapper}, which this class does not touch —
 * declaring a top-level {@code ObjectMapper} bean would back off {@code JacksonAutoConfiguration}
 * entirely (a bug this codebase has already had once).
 *
 * <h2>2. ...but only for types we own</h2>
 *
 * Default typing means "instantiate whatever class this JSON names", which is the classic Java
 * deserialization gadget vector: anyone who can write to Redis can name a class whose construction
 * has side effects and have this JVM build it.
 *
 * <p>This cache is owned solely by this service — no other service reads or writes these keys; they
 * consume the data through the API. So the set of classes that may legitimately appear in a cached
 * value is small and knowable, and it is whitelisted below. A payload naming anything else fails to
 * deserialize instead of being constructed. Because that is the entire security boundary here, it is
 * enforced by test ({@code RedisSerializerTypingTest}), not just by comment.
 */
public final class CacheValueSerializer {

    private CacheValueSerializer() {
    }

    /** This application's own DTOs — the only application classes ever cached. */
    private static final String APP_PACKAGE = "com.wanfadger.AdministrativeareaApi.";

    public static RedisSerializer<Object> create() {
        return new GenericJackson2JsonRedisSerializer(objectMapper());
    }

    static ObjectMapper objectMapper() {
        PolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(APP_PACKAGE)
                .allowIfSubType(APP_PACKAGE)
                // The containers and scalars the DTOs are built from.
                .allowIfSubType("java.util.")
                .allowIfSubType("java.lang.")
                .allowIfSubType("java.time.")
                // Spring's "cached, and the value is null" sentinel — distinct from "not cached",
                // and it has to survive the network hop or such keys are re-queried forever.
                .allowIfSubType(NullValue.class)
                .build();

        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        // Id.CLASS + As.PROPERTY reproduces the "@class" property the stock serializer writes, so
        // entries cached by the previous configuration still read back after this deploy.
        StdTypeResolverBuilder typer = new AppTypeResolverBuilder(validator)
                .init(JsonTypeInfo.Id.CLASS, null)
                .inclusion(JsonTypeInfo.As.PROPERTY);
        mapper.setDefaultTyping(typer);

        // The stock serializer registers this internally; a hand-supplied mapper does not get it free,
        // and without it NullValue fails to serialize.
        GenericJackson2JsonRedisSerializer.registerNullValueSerializer(mapper, null);
        return mapper;
    }

    /**
     * Decides which types carry an {@code @class} hint.
     *
     * <p>The obvious choice, Jackson's built-in {@code DefaultTyping.NON_FINAL}, is subtly wrong here
     * and fails on exactly one value: {@link NullValue}. It is {@code final}, so {@code NON_FINAL}
     * declines to <i>read</i> a type id for it — while Spring's null-value serializer writes one
     * regardless. The hint then arrives as an unrecognised bean property and deserialization blows up.
     * Since a cached null is meant to be indistinguishable from any other cached value, this would
     * have surfaced as an intermittent 500 on whichever key happened to have no value.
     *
     * <p>So the rule is the one Spring Data's own serializer uses: skip type hints only for types that
     * cannot be ambiguous anyway — primitives, enums, and {@code final} JDK types such as
     * {@code String}, {@code Double} and {@code LocalDateTime}. Everything else, including
     * {@code NullValue} and this application's DTOs, is tagged. Keeping the hints off the JDK scalars
     * is not cosmetic either: tagging every {@code String} would roughly double the size of every
     * cached page.
     */
    private static final class AppTypeResolverBuilder extends ObjectMapper.DefaultTypeResolverBuilder {

        AppTypeResolverBuilder(PolymorphicTypeValidator validator) {
            super(ObjectMapper.DefaultTyping.EVERYTHING, validator);
        }

        @Override
        public boolean useForType(JavaType type) {
            if (type.isJavaLangObject()) {
                return true;
            }
            JavaType element = type;
            while (element.isArrayType()) {
                element = element.getContentType();
            }
            Class<?> raw = element.getRawClass();
            if (element.isEnumType() || ClassUtils.isPrimitiveOrWrapper(raw)) {
                return false;
            }
            return !element.isFinal() || !raw.getPackageName().startsWith("java");
        }
    }
}
