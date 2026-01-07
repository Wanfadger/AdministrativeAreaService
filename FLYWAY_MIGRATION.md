# Flyway Database Migration Guide

## Overview

This application uses Flyway for database schema migrations. Flyway automatically runs migrations on application startup to ensure the database schema is up-to-date.

## Configuration

Flyway is configured in `application-dev1.properties`:

```properties
# Flyway Database Migration
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
spring.flyway.validate-on-migrate=true
spring.flyway.clean-disabled=true
```

### Configuration Options

- **`spring.flyway.enabled=true`**: Enables Flyway migrations
- **`spring.flyway.locations=classpath:db/migration`**: Location of migration scripts
- **`spring.flyway.baseline-on-migrate=true`**: Creates baseline for existing databases
- **`spring.flyway.validate-on-migrate=true`**: Validates migrations before execution
- **`spring.flyway.clean-disabled=true`**: Prevents accidental database cleanup

## How Flyway Executes

### Automatic Execution

Flyway runs automatically when the Spring Boot application starts:

1. **On Application Startup**: Flyway checks the `flyway_schema_history` table
2. **Migration Detection**: Compares existing migrations with files in `src/main/resources/db/migration`
3. **Execution**: Runs any new migrations in version order (V1, V2, V3, etc.)
4. **History Tracking**: Records executed migrations in `flyway_schema_history` table

### Migration File Naming

Migration files must follow this naming convention:
```
V{version}__{description}.sql
```

Examples:
- `V1__add_performance_indexes.sql`
- `V2__add_new_table.sql`
- `V3__update_schema.sql`

**Important**: 
- Version numbers must be sequential
- Use double underscore (`__`) between version and description
- File extension must be `.sql`

## Current Migrations

### V1__add_performance_indexes.sql

**Purpose**: Creates performance indexes on all administrative area tables.

**What it does**:
- Creates indexes on `code` columns (most frequent lookup)
- Creates indexes on `name` columns with `LOWER()` for case-insensitive searches
- Creates foreign key indexes for hierarchical relationships
- Creates composite indexes for common query patterns
- Safely handles existing indexes (uses `IF NOT EXISTS`)

**Location**: `src/main/resources/db/migration/V1__add_performance_indexes.sql`

**Note**: This migration safely checks if tables exist before creating indexes, making it compatible with both fresh installations and existing databases.

## Integration with Hibernate

### Schema Creation Strategy

The application uses a hybrid approach:

1. **Hibernate** (`ddl-auto=update`): Creates/updates table structures
2. **Flyway**: Adds indexes and performs schema optimizations

**Why this approach?**
- Hibernate handles entity relationships and column definitions automatically
- Flyway handles performance optimizations (indexes) that are better managed via SQL
- BaseEntity also defines indexes via `@Index` annotation for fresh installations

### Execution Order

1. **Application starts**
2. **Hibernate creates/updates tables** (if `ddl-auto=update`)
3. **Flyway runs migrations** (adds indexes, optimizations)
4. **Application ready**

## Manual Flyway Operations

### Check Migration Status

View Flyway history in the database:
```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

### Repair Failed Migrations

If a migration fails and needs to be repaired:
```bash
mvn flyway:repair
```

### Run Migrations Manually (Maven)

```bash
# Migrate database
mvn flyway:migrate

# Check migration status
mvn flyway:info

# Validate migrations
mvn flyway:validate
```

**Note**: These commands require Flyway Maven plugin configuration in `pom.xml`.

## Best Practices

### 1. Migration Files

- **Idempotent**: Migrations should be safe to run multiple times
- **Use `IF NOT EXISTS`**: For indexes and other objects that might already exist
- **Test First**: Test migrations on development database before production

### 2. Version Management

- **Sequential versions**: Never skip version numbers
- **Descriptive names**: Use clear descriptions in file names
- **One change per migration**: Keep migrations focused and atomic

### 3. Data Seeding

**Use Java components for data seeding**:
- `TestDataSeeder` component handles entity relationships correctly
- Set `app.seed.test-data=true` in `application-dev1.properties`
- Java components handle column names and relationships automatically

**Why not SQL for seeding?**
- Column names depend on Hibernate naming strategy
- Entity relationships are complex
- Timestamp columns are auto-generated
- Java components are more maintainable

## Troubleshooting

### Migration Not Running

1. **Check Flyway is enabled**: Verify `spring.flyway.enabled=true`
2. **Check file location**: Ensure files are in `src/main/resources/db/migration`
3. **Check naming**: Verify file follows `V{version}__{description}.sql` format
4. **Check logs**: Look for Flyway execution messages in application logs

### Migration Fails

1. **Check database connection**: Ensure database is accessible
2. **Check permissions**: Ensure database user has CREATE/ALTER permissions
3. **Check syntax**: Verify SQL syntax is correct for your database
4. **Check dependencies**: Ensure previous migrations completed successfully

### View Flyway Logs

Enable Flyway logging in `application.properties`:
```properties
logging.level.org.flywaydb=DEBUG
```

## Migration History

Flyway tracks all executed migrations in the `flyway_schema_history` table:

```sql
-- View all migrations
SELECT 
    installed_rank,
    version,
    description,
    type,
    script,
    installed_on,
    execution_time,
    success
FROM flyway_schema_history
ORDER BY installed_rank;
```

## Related Documentation

- **Database Indexes**: See `SCALABILITY_RECOMMENDATIONS.md` for index strategy
- **Test Data Seeding**: See `TEST_DATA_SEEDING.md` for data seeding approach
- **BaseEntity Indexes**: See `BaseEntity.java` for entity-level index definitions
