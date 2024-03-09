package com.wanfadger.AdministrativeareaApi.shared.security;


import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;

public interface JwtService {
    String extractUsername(String token);

    String generateToken(UserDetails userDetails);

    boolean isTokenValid(String token, UserDetails userDetails);
    Date extractExpiration(String token);
}
