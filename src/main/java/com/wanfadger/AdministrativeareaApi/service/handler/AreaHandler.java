package com.wanfadger.AdministrativeareaApi.service.handler;

import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.AlreadyExistsException;
import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.NotFoundException;
import com.wanfadger.AdministrativeareaApi.dto.AdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.reponses.PaginatedResponseDTO;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import com.wanfadger.AdministrativeareaApi.entity.BaseEntity;
import com.wanfadger.AdministrativeareaApi.entity.NamedArea;
import com.wanfadger.AdministrativeareaApi.repository.AreaRepository;
import com.wanfadger.AdministrativeareaApi.repository.specification.MatchType;
import com.wanfadger.AdministrativeareaApi.repository.specification.SpecificationBuilder;
import com.wanfadger.AdministrativeareaApi.service.mapper.AreaDtoMapper;
import com.wanfadger.AdministrativeareaApi.service.query.SearchQuery;
import com.wanfadger.AdministrativeareaApi.service.query.View;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * One implementation of each operation, shared by all six levels.
 *
 * <p>The service used to be a 627-line file where every public method was a six-branch switch, each
 * branch structurally identical to the other five. That is how the sub-county duplicate-check bug
 * survived: {@code create} and {@code update} had drifted, and only {@code create} was wrong. With
 * one code path they cannot drift again.
 *
 * <p><b>The generics.</b> {@code T} is the entity, {@code P} its parent entity ({@link Void} for
 * REGION, which has none). Both are bound inside this class, so {@code repo.findAll(spec, pageable)}
 * and {@code entity.setSubCounty(parent)} type-check here with no casts. The public methods never
 * mention {@code T} or {@code P}, so the registry can hold a heterogeneous
 * {@code Map<AdministrativeAreaType, AreaHandler<?, ?>>} and callers just invoke them.
 *
 * <p><b>The error strings are contract, not cosmetics.</b> They are inconsistent today — create says
 * "Parish Already Exists in the sub county" while update says "Parish already exists in the sub
 * county"; SUBCOUNTY raises a bare "Invalid PartOfCode" where the other levels append the code; and
 * REGION reports a missing area on update as a 400 "Invalid PartOfCode" rather than a 404. The
 * frontend renders {@code detail} verbatim, so the hooks below exist to TRANSCRIBE these strings and
 * behaviours, not to tidy them. The golden files enforce it.
 */
@RequiredArgsConstructor
public abstract class AreaHandler<T extends BaseEntity & NamedArea, P> {

    protected final AreaRepository<T> repo;

    // ------------------------------------------------------------------ typed hooks

    public abstract AdministrativeAreaType type();

    protected abstract T newEntity();

    /** Look the parent up by code, or throw this level's exact "invalid parent" error. */
    protected abstract P resolveParent(String partOfCode);

    /** Attach the resolved parent. No-op for REGION. */
    protected abstract void attachParent(T entity, P parent);

    /** Duplicate lookup scoped to the parent. REGION scopes globally — it has no parent. */
    protected abstract Optional<T> findDuplicate(String name, String partOfCode);

    /** Does this area still have children? Always false for PARISH, the leaf. */
    protected abstract boolean hasChildren(String code);

    protected abstract AdministrativeAreaDTO toNestedDto(T entity);

    /** The immediate parent's code, for {@code ?view=flat}. Null for REGION. */
    protected abstract String parentCodeOf(T entity);

    /** LEFT JOIN FETCH chain: full ancestry for NESTED, immediate parent only for FLAT. */
    protected abstract Specification<T> fetchChain(View view);

    /** Property path for the parent-code filter, e.g. "subCounty.code". Null for REGION. */
    protected abstract String parentCodePath();

    // ------------------------------------------------------------------ message hooks (verbatim)

    protected abstract String missingParentMessage();

    protected abstract String duplicateOnCreateMessage();

    protected abstract String duplicateOnUpdateMessage();

    protected abstract String notFoundMessage();

    /** Null for PARISH — a leaf can always be deleted. */
    protected abstract String hasChildrenMessage();

    /** REGION has no parent, so partOfCode is neither required nor meaningful. */
    protected boolean requiresParent() {
        return true;
    }

    /**
     * Thrown when the area being UPDATED does not exist. Every level reports a 404 "Administrative
     * Area NotFound" — except REGION, which reports a 400 "Invalid PartOfCode". Preserved as-is.
     */
    protected RuntimeException notFoundOnUpdate() {
        return new NotFoundException("Administrative Area NotFound");
    }

    // ------------------------------------------------------------------ operations

    public final String create(NewAdministrativeAreaDTO dto) {
        if (requiresParent() && isBlank(dto.getPartOfCode())) {
            throw new MissingDataException(missingParentMessage());
        }
        // Parent is resolved BEFORE the duplicate check, matching the original ordering: an invalid
        // partOfCode must surface as "Invalid PartOfCode", not as a duplicate error.
        P parent = requiresParent() ? resolveParent(dto.getPartOfCode()) : null;

        if (findDuplicate(dto.getName(), dto.getPartOfCode()).isPresent()) {
            throw new AlreadyExistsException(duplicateOnCreateMessage());
        }

        T entity = newEntity();
        entity.setName(dto.getName());
        // Description was persisted for REGION only and silently dropped for the other five levels.
        entity.setDescription(dto.getDescription());
        entity.setLatitude(parseCoordinate(dto.getLatitude(), "latitude"));
        entity.setLongitude(parseCoordinate(dto.getLongitude(), "longitude"));
        // Was a do/while that SELECTed to check for a UUID collision: a DB round-trip per created
        // row, which also broke JDBC batching on bulk create. The collision probability is nil.
        entity.setCode(UUID.randomUUID().toString());

        if (requiresParent()) {
            attachParent(entity, parent);
        }
        repo.save(entity);
        return entity.getCode();
    }

    public final void update(String code, NewAdministrativeAreaDTO dto) {
        if (requiresParent() && isBlank(dto.getPartOfCode())) {
            throw new MissingDataException("Missing PartOfCode");
        }
        // Resolved before the area is loaded — an invalid parent beats a missing area, as before.
        P parent = requiresParent() ? resolveParent(dto.getPartOfCode()) : null;

        T entity = repo.findByCodeIgnoreCase(code).orElseThrow(this::notFoundOnUpdate);

        // A rename must not collide with a sibling under the (possibly new) parent.
        String effectiveName = notBlank(dto.getName()) ? dto.getName() : entity.getName();
        findDuplicate(effectiveName, dto.getPartOfCode())
                .filter(found -> !found.getCode().equalsIgnoreCase(code))
                .ifPresent(found -> {
                    throw new AlreadyExistsException(duplicateOnUpdateMessage());
                });

        // Partial update: only fields the caller actually supplied are applied.
        if (notBlank(dto.getName())) entity.setName(dto.getName());
        if (notBlank(dto.getDescription())) entity.setDescription(dto.getDescription());
        if (notBlank(dto.getLatitude())) entity.setLatitude(parseCoordinate(dto.getLatitude(), "latitude"));
        if (notBlank(dto.getLongitude())) entity.setLongitude(parseCoordinate(dto.getLongitude(), "longitude"));

        if (requiresParent()) {
            attachParent(entity, parent);
        }
        repo.save(entity);
    }

    public final void delete(String code) {
        T entity = repo.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException(notFoundMessage()));
        if (hasChildrenMessage() != null && hasChildren(code)) {
            throw new InvalidException(hasChildrenMessage());
        }
        repo.delete(entity);   // @SQLDelete -> soft delete
    }

    public final AdministrativeAreaDTO getOne(String code) {
        return toNestedDto(repo.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException(notFoundMessage())));
    }

    public final PaginatedResponseDTO<AdministrativeAreaDTO> search(SearchQuery query) {
        Specification<T> spec = SpecificationBuilder.<T>freeText(query.search(), "name", "code")
                .and(SpecificationBuilder.fromQueryMap(query.filters(), Set.of()))
                .and(fetchChain(query.view()));

        if (notBlank(query.partOf()) && parentCodePath() != null) {
            spec = spec.and(SpecificationBuilder.where(parentCodePath(), query.partOf(), MatchType.EQUALS));
        }

        // T is bound here, so this type-checks with no cast — the point of the design.
        Page<T> result = repo.findAll(spec, query.pageable());

        Function<T, AdministrativeAreaDTO> mapper = query.view() == View.FLAT
                ? entity -> AreaDtoMapper.flat(entity, parentCodeOf(entity))
                : this::toNestedDto;

        List<AdministrativeAreaDTO> data = result.getContent().stream().map(mapper).toList();

        PaginatedResponseDTO<AdministrativeAreaDTO> response = new PaginatedResponseDTO<>(data);
        response.setMessage("Administrative areas fetched successfully");
        response.setStatus(true);
        response.setPage(query.page());
        response.setSize(query.size());
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        response.setHasNext(result.hasNext());
        response.setHasPrevious(result.hasPrevious());
        return response;
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Coordinates arrive as strings. Blank means ABSENT, and absent means {@code null} — never
     * {@code 0}, because zero is a legitimate coordinate (the equator, the prime meridian).
     * Defaulting an unset area to zero would place it in the Atlantic off Ghana.
     *
     * <p>This used to be {@code getLatitude() != null ? Double.valueOf(...) : null}, so a blank
     * string reached {@code Double.valueOf("")} and threw {@code NumberFormatException} → 500. The
     * frontend sends {@code ""} whenever the coordinate fields are left empty, making it a 500 on an
     * ordinary user action, on five of the six levels.
     */
    protected static Double parseCoordinate(String raw, String field) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Double.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            throw new InvalidException("Invalid " + field + " '" + raw + "': must be a decimal number");
        }
    }

    protected static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    protected static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
