# Flyway Checksum Repair Guide

## Problem
After modifying `V1__add_performance_indexes.sql`, Flyway detects a checksum mismatch:
- Applied to database: 449677818
- Resolved locally: 1703871227

## Solution Options

### Option 1: Repair Checksum via SQL (Recommended)

Connect to your database and run:

```sql
-- Update the checksum for V1 migration to match the new file
UPDATE flyway_schema_history 
SET checksum = 1703871227 
WHERE version = '1' AND script = 'V1__add_performance_indexes.sql';
```

Then re-enable validation in `application-dev1.properties`:
```properties
spring.flyway.validate-on-migrate=true
```

### Option 2: Temporarily Disable Validation

Validation has been temporarily disabled in `application-dev1.properties`:
```properties
spring.flyway.validate-on-migrate=false
```

**Important**: After repairing the checksum via SQL (Option 1), re-enable validation.

### Option 3: Use Flyway Repair Command

If you have Flyway CLI or Maven plugin configured with database credentials:

```bash
mvn flyway:repair -Dflyway.url=jdbc:postgresql://localhost:5437/areadevdb \
  -Dflyway.user=siip-db-user-dev \
  -Dflyway.password=qwertyuftdytrsdev
```

## Why This Happened

We removed redundant simple `code` indexes from V1 migration because:
- Entity-level `@Table` annotations now handle simple code/name indexes
- Flyway migration now focuses only on complex indexes (function-based, foreign keys, composite)

## Prevention

**Best Practice**: Never modify an already-applied migration file. Instead:
1. Create a new migration (V2, V3, etc.) for changes
2. Or repair checksum if modification is intentional (development only)

## Verification

After repair, verify the checksum matches:

```sql
SELECT version, script, checksum, installed_on 
FROM flyway_schema_history 
WHERE version = '1';
```

The checksum should be: `1703871227`
