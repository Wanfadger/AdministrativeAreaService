package com.wanfadger.AdministrativeareaApi.repository.projections;

import java.io.Serializable;

public interface CodeNameProjection extends Serializable {
    String getCode();
    String getName();
}
