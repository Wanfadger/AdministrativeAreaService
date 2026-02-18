package com.wanfadger.AdministrativeareaApi.shared;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;

@Service
public class SharedServiceImpl implements SharedService {

    /**
     * Generates a unique code in the format:
     * CODE-YYYY-HHmmssSSS
     * Format: CODE-<YEAR>-<hours><minutes><seconds><milliseconds>
     * Example: CODE-2025-143052123 (for 2:30:52.123 PM on any day in 2025)
     * 
     * Includes milliseconds to ensure uniqueness even when multiple records
     * are created within the same second.
     * 
     * @return A unique code string
     */
    @Override
    public String generateCode(AdministrativeAreaType areaType) {
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        int hour = now.getHour();
        int minute = now.getMinute();
        int second = now.getSecond();
        int millisecond = now.getNano() / 1_000_000; // Convert nanoseconds to milliseconds
        int randomSuffix = java.util.concurrent.ThreadLocalRandom.current().nextInt(1000, 9999);

        String code = switch (areaType) {
            case REGION -> "RG";
            case SUBREGION -> "SR";
            case LOCALGOVERNMENT -> "LG";
            case COUNTY -> "CO";
            case SUBCOUNTY -> "SC";
            case PARISH -> "PA";
        };

        return String.format(code + "%04d%02d%02d%02d%03d%04d", year, hour, minute, second,
                millisecond,
                randomSuffix);
    }

}
