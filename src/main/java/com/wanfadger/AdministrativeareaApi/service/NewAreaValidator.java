package com.wanfadger.AdministrativeareaApi.service;

import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.InvalidException;
import com.wanfadger.AdministrativeareaApi.dto.NewAdministrativeAreaDTO;
import com.wanfadger.AdministrativeareaApi.dto.OnCreate;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Validates the CREATE body, which is a list.
 *
 * <p>This is deliberately programmatic rather than {@code @Valid @RequestBody List<...>}, because
 * that annotation does not do what it looks like it does: Spring's
 * {@code RequestResponseBodyMethodProcessor} runs the validator against the {@code ArrayList}
 * itself, which carries no constraints, so nothing fires and every element sails through unchecked.
 * Container-element cascading is a different mechanism with different exceptions.
 *
 * <p>Doing it here also means the failure is one {@link InvalidException} with a populated,
 * human-readable message — which matters because the frontend renders {@code detail} verbatim —
 * and it reports the offending index, so a caller posting 500 areas learns which one is wrong.
 */
@Component
@RequiredArgsConstructor
public class NewAreaValidator {

    private final Validator validator;

    public void validateForCreate(List<NewAdministrativeAreaDTO> dtos) {
        String detail = IntStream.range(0, dtos.size())
                .boxed()
                .flatMap(i -> violations(dtos.get(i)).stream()
                        .map(v -> "[" + i + "]." + v.getPropertyPath() + ": " + v.getMessage()))
                .sorted()
                .collect(Collectors.joining("; "));

        if (!detail.isBlank()) {
            throw new InvalidException(detail);
        }
    }

    private List<ConstraintViolation<NewAdministrativeAreaDTO>> violations(NewAdministrativeAreaDTO dto) {
        return List.copyOf(validator.validate(dto, OnCreate.class, Default.class));
    }
}
