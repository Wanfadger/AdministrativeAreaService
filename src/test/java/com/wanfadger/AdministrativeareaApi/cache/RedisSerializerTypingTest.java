package com.wanfadger.AdministrativeareaApi.cache;

import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.ParishDTO;
import com.wanfadger.AdministrativeareaApi.dto.SubCountyDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.ResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.cache.support.NullValue;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The Redis value serializer must do two things that pull against each other: preserve enough type
 * information to reconstruct the DTO subtypes, while refusing to instantiate anything else.
 *
 * <p><b>Why the type hints are needed.</b> Cached values are generic — {@code ResponseDTO<T>},
 * {@code List<T>} — so the declared type is erased by the time the value comes back out of Redis.
 * Without a hint, Jackson has no way to know a {@code ParishDTO} from a {@code RegionDTO} and hands
 * back {@code LinkedHashMap}s, which changes the response shape.
 *
 * <p><b>Why the whitelist is needed.</b> "Instantiate whichever class this JSON names" is the classic
 * Java deserialization gadget vector. This cache is owned solely by this service — other consumers go
 * through the API, never through Redis — so the classes that may legitimately appear are knowable and
 * are enumerated. Anything else must fail to deserialize rather than be constructed.
 */
class RedisSerializerTypingTest {

    /** Exactly the serializer the cache is configured with — not a look-alike built for the test. */
    private final RedisSerializer<Object> serializer = CacheValueSerializer.create();

    private Object roundTrip(Object value) {
        return serializer.deserialize(serializer.serialize(value));
    }

    @Test
    void aParishRoundTripsAsAParish_notAMap() {
        ParishDTO parish = new ParishDTO();
        parish.setCode("P1");
        parish.setName("ABALANG");
        SubCountyDTO subCounty = new SubCountyDTO();
        subCounty.setCode("SC1");
        subCounty.setName("ALWA");
        parish.setSubCounty(subCounty);

        Object result = roundTrip(new ResponseDTO<AdministrativeAreaDTO>(parish));

        assertThat(result).isInstanceOf(ResponseDTO.class);
        Object data = ((ResponseDTO<?>) result).getData();
        assertThat(data)
                .as("without a type hint this comes back as a LinkedHashMap")
                .isInstanceOf(ParishDTO.class);
        assertThat(((ParishDTO) data).getSubCounty().getName()).isEqualTo("ALWA");
    }

    /** The shape /search returns: a paginated envelope whose data is a list of polymorphic DTOs. */
    @Test
    void aPaginatedPageOfParishesRoundTrips() {
        ParishDTO parish = new ParishDTO();
        parish.setCode("P1");
        parish.setName("ABALANG");

        PaginatedResponseDTO<AdministrativeAreaDTO> page = new PaginatedResponseDTO<>(List.of(parish));
        page.setTotalElements(1);
        page.setPage(1);
        page.setSize(50);

        Object result = roundTrip(page);

        assertThat(result).isInstanceOf(PaginatedResponseDTO.class);
        PaginatedResponseDTO<?> back = (PaginatedResponseDTO<?>) result;
        assertThat(back.getTotalElements()).isEqualTo(1);
        assertThat(back.getPage()).isEqualTo(1);
        assertThat(back.getData()).singleElement().isInstanceOf(ParishDTO.class);
    }

    /** Spring's "cached, and the value is null" sentinel has to survive the network hop. */
    @Test
    void theNullValueSentinelRoundTrips() {
        assertThat(roundTrip(NullValue.INSTANCE)).isInstanceOf(NullValue.class);
    }

    /**
     * The whole point of the validator. A payload naming a class outside the whitelist must be
     * refused, not constructed — even though the JSON is perfectly well-formed.
     */
    @Test
    void aClassOutsideTheWhitelistIsRefused() {
        byte[] hostile = ("{\"@class\":\"javax.naming.spi.ObjectFactory\",\"x\":1}")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);

        assertThatThrownBy(() -> serializer.deserialize(hostile))
                .as("default typing without a validator would try to build this")
                .hasMessageContaining("javax.naming");
    }
}
