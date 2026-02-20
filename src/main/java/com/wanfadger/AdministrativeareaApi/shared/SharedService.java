package com.wanfadger.AdministrativeareaApi.shared;

import java.util.function.Function;
import java.util.function.Predicate;

import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;

public interface SharedService {
    String generateCode(AdministrativeAreaType areaType);
    <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor);
}
