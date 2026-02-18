package com.wanfadger.AdministrativeareaApi.shared;

import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;

public interface SharedService {
    String generateCode(AdministrativeAreaType areaType);
}
