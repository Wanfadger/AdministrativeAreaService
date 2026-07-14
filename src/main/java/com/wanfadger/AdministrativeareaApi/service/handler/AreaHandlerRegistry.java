package com.wanfadger.AdministrativeareaApi.service.handler;

import com.wanfadger.AdministrativeareaApi.administrativeareaexceptions.MissingDataException;
import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

/**
 * Resolves a type to its handler.
 *
 * <p>Spring injects every {@link AreaHandler} bean, and the constructor asserts that all six levels
 * are covered — so a missing handler is a startup failure with a clear message, not a
 * {@code NullPointerException} on the one endpoint nobody exercised.
 */
@Component
public class AreaHandlerRegistry {

    private final EnumMap<AdministrativeAreaType, AreaHandler<?, ?>> handlers =
            new EnumMap<>(AdministrativeAreaType.class);

    public AreaHandlerRegistry(List<AreaHandler<?, ?>> beans) {
        beans.forEach(handler -> handlers.put(handler.type(), handler));

        EnumSet<AdministrativeAreaType> missing = EnumSet.allOf(AdministrativeAreaType.class);
        missing.removeAll(handlers.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("No AreaHandler registered for: " + missing);
        }
    }

    public AreaHandler<?, ?> get(AdministrativeAreaType type) {
        AreaHandler<?, ?> handler = handlers.get(type);
        if (handler == null) {
            throw new MissingDataException("Missing Administrative Area Type");
        }
        return handler;
    }
}
