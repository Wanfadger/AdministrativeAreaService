package com.wanfadger.AdministrativeareaApi.service.query;

import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The ONLY place a search query is built. Everything that reaches the service or the cache key
 * passes through {@link #canonicalise}.
 *
 * <p>Three things depend on that being true.
 *
 * <p><b>1. Cache-key cardinality.</b> The cache key is derived from the query map, and the map used
 * to be the raw request params. So {@code ?type=REGION&_cb=<random>} minted an unbounded number of
 * distinct cache entries, each holding a full result page — a memory-exhaustion vector on both
 * cache tiers, reachable by any client. Only whitelisted keys survive canonicalisation, so junk
 * params can no longer fork the key (or the SQL).
 *
 * <p><b>2. Guardrails.</b> {@code size=0}, {@code sortBy=bogus} and {@code sortDirection=bogus}
 * were all 500s: {@code PageRequest.of(_, 0)} throws, {@code Direction.fromString} throws, and an
 * unknown sort property throws {@code PropertyReferenceException} at query time. Size is now
 * clamped, direction falls back to ASC, and an unknown sort field is a clean 400.
 *
 * <p><b>3. Cache warmup.</b> The warmup runner must produce byte-identical keys to the controller,
 * or it warms entries nobody ever reads while every dashboard cheerfully reports success. Sharing
 * this component is what guarantees that.
 */
@Component
public class AreaQueryFactory {

    /** Default page size. Deliberately small: a caller who omits `size` should not pull a
     *  thousand rows of six-deep nested ancestry. Every frontend call site sets it explicitly. */
    public static final int DEFAULT_PAGE_SIZE = 50;

    /** Hard ceiling. The map view legitimately asks for ~2,000; beyond 5,000 a single response
     *  would be tens of megabytes of heap per concurrent request. */
    public static final int MAX_PAGE_SIZE = 5000;

    /** Sortable columns. An unknown value is a 400, never a 500 from deep inside the query layer. */
    private static final Set<String> SORTABLE =
            Set.of("name", "code", "latitude", "longitude", "description",
                    "createdDateTime", "updatedDateTime");

    /** Rendered into the 400 so the caller learns what they may sort by. */
    private static final String SORTABLE_LIST =
            SORTABLE.stream().sorted().collect(java.util.stream.Collectors.joining(", "));

    /** Columns a {@code field:OPERATOR=value} filter may target. */
    private static final Set<String> FILTERABLE =
            Set.of("name", "code", "latitude", "longitude", "description");

    /**
     * Structural params, consumed here rather than turned into column filters.
     *
     * <p><b>{@code code} is deliberately NOT in this set</b>, though it looks like it belongs. It is a
     * {@link #FILTERABLE} column, and reserving it here silently disabled every {@code code:OPERATOR}
     * filter: {@link #canonicalise} and {@link #toQuery} both skip RESERVED fields, so
     * {@code ?code:IN=a,b,c} was dropped and the caller got the <i>entire level</i> back — no error,
     * just the wrong rows. That is the worst way for a filter to fail, and it made
     * {@code code:IN} useless as a validation tool: asking "do these codes exist?" answered "yes" for
     * every code, because the filter was never applied.
     *
     * <p>{@code /{code}} is a path variable and {@code getOne} builds its own map directly, never
     * passing through here — so nothing needs protecting.
     */
    private static final Set<String> RESERVED =
            Set.of("type", "partOf", "search", "page", "size", "sortBy", "sortDirection", "view");

    /**
     * Build the canonical query map: whitelisted, clamped, normalised, and sorted so that two
     * requests differing only in param order collapse to the same cache key.
     */
    public Map<String, String> canonicalise(AdministrativeAreaType type, Map<String, String> raw) {
        Map<String, String> out = new TreeMap<>();
        out.put("type", type.name());

        Map<String, String> in = raw == null ? Map.of() : raw;

        put(out, "partOf", in.get("partOf"));
        put(out, "search", in.get("search"));

        int page = Math.max(1, parseInt(in.get("page"), 1));
        int size = clamp(parseInt(in.get("size"), DEFAULT_PAGE_SIZE), 1, MAX_PAGE_SIZE);
        out.put("page", String.valueOf(page));
        out.put("size", String.valueOf(size));

        String sortBy = blankToNull(in.get("sortBy"));
        if (sortBy == null) {
            sortBy = "name";
        } else if (!SORTABLE.contains(sortBy)) {
            throw new InvalidException(
                    "Unknown sort field '" + sortBy + "'. Sortable fields: " + SORTABLE_LIST);
        }
        out.put("sortBy", sortBy);

        // An unparseable direction falls back to ASC rather than throwing — it cannot produce a
        // wrong result, only an unsurprising one.
        Sort.Direction direction = Sort.Direction
                .fromOptionalString(String.valueOf(in.get("sortDirection")))
                .orElse(Sort.Direction.ASC);
        out.put("sortDirection", direction.name().toLowerCase());

        // Normalise so view=, view=nested, view=NESTED and view=garbage share one cache entry.
        View view = View.from(in.get("view"));
        if (view == View.FLAT) {
            out.put("view", "flat");
        }

        // Surviving advanced filters: field:OPERATOR=value, field whitelisted.
        for (Map.Entry<String, String> e : in.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();
            if (key == null || value == null || value.isBlank()) continue;

            int colon = key.indexOf(':');
            String field = colon > 0 ? key.substring(0, colon) : key;
            if (RESERVED.contains(field)) continue;
            if (!FILTERABLE.contains(field)) continue;   // silently dropped: cannot fork the key

            out.put(key, value.trim());
        }
        return out;
    }

    /** Re-read a canonical map into a validated {@link SearchQuery}. */
    public SearchQuery toQuery(Map<String, String> canonical) {
        AdministrativeAreaType type = AdministrativeAreaType.fromStr(canonical.get("type"))
                .orElseThrow(() -> new MissingDataException("Missing or unsupported Administrative Area Type"));

        int page = parseInt(canonical.get("page"), 1);
        int size = parseInt(canonical.get("size"), DEFAULT_PAGE_SIZE);
        Sort.Direction direction = Sort.Direction
                .fromOptionalString(String.valueOf(canonical.get("sortDirection")))
                .orElse(Sort.Direction.ASC);
        Pageable pageable = PageRequest.of(page - 1, size,
                Sort.by(direction, canonical.getOrDefault("sortBy", "name")));

        Map<String, String> filters = new LinkedHashMap<>();
        canonical.forEach((k, v) -> {
            String field = k.indexOf(':') > 0 ? k.substring(0, k.indexOf(':')) : k;
            if (!RESERVED.contains(field)) filters.put(k, v);
        });

        return new SearchQuery(
                type,
                canonical.get("search"),
                canonical.get("partOf"),
                View.from(canonical.get("view")),
                pageable,
                filters,
                page,
                size);
    }

    private static void put(Map<String, String> map, String key, String value) {
        String v = blankToNull(value);
        if (v != null) map.put(key, v);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static int clamp(int v, int min, int max) {
        return Math.min(Math.max(v, min), max);
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
