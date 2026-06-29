package com.wanfadger.AdministrativeareaApi.converter;

import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Case-insensitive binding for {@code AdministrativeAreaType} request params. Spring's default
 * String→enum conversion is {@code Enum.valueOf}, which only matches the exact constant name
 * (e.g. {@code REGION}). This routes through {@link AdministrativeAreaType#fromStr(String)} instead,
 * so {@code region}, {@code Region}, {@code REGION} — and even the display form {@code "sub region"}
 * — all resolve. Spring Boot auto-registers {@link Converter} beans into the MVC conversion service.
 *
 * <p>An unknown value throws {@link IllegalArgumentException}, which the framework wraps as a
 * {@code MethodArgumentTypeMismatchException} → handled as a 400 in the global exception handler.
 */
@Component
public class StringToAdministrativeAreaTypeConverter implements Converter<String, AdministrativeAreaType> {

    @Override
    public AdministrativeAreaType convert(@NonNull String source) {
        return AdministrativeAreaType.fromStr(source)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported administrative area type: '" + source + "'"));
    }
}
