# Test Data Seeding Guide

This guide explains how to seed test data for Swagger testing and load testing.

## Overview

The application includes two methods for seeding test data:

1. **Java Component Seeder** (`TestDataSeeder.java`) - Recommended
   - Runs automatically on application startup (if enabled)
   - Creates hierarchical test data
   - Can be enabled/disabled via properties

2. **SQL Migration Script** (`V2__seed_test_data.sql`) - Alternative
   - Flyway migration script
   - Can be run manually or via Flyway

## Test Data Structure

The seeder creates a complete hierarchical structure:

- **3 Regions** (REG001, REG002, REG003)
- **6 Sub-Regions** (2 per region: SR001-SR006)
- **12 Local Governments** (2 per sub-region: LG001-LG012)
- **24 Counties** (2 per local government: CT001-CT024)
- **48 Sub-Counties** (2 per county: SC001-SC048)
- **96 Parishes** (2 per sub-county: PR001-PR096)

**Total: 189 administrative areas**

## Method 1: Java Component Seeder (Recommended)

### Configuration

Add to `application-dev1.properties`:

```properties
# Test Data Seeding Configuration
app.seed.test-data=true          # Enable/disable test data seeding
app.seed.clear-existing=false    # Clear existing data before seeding (use with caution!)
```

### How It Works

1. The `TestDataSeeder` component runs after application startup
2. Checks if data already exists (unless `clear-existing=true`)
3. Creates hierarchical test data if enabled
4. Logs seeding progress and results

### Enable Seeding

```properties
# Enable test data seeding
app.seed.test-data=true
```

### Disable Seeding

```properties
# Disable test data seeding
app.seed.test-data=false
```

### Clear and Reseed

**⚠️ WARNING**: This will delete all existing data!

```properties
# Clear existing data and reseed
app.seed.test-data=true
app.seed.clear-existing=true
```

### Usage

1. **First Time Setup**:
   ```properties
   app.seed.test-data=true
   app.seed.clear-existing=false
   ```
   Start the application - data will be seeded automatically.

2. **Reseed Data** (clears existing):
   ```properties
   app.seed.test-data=true
   app.seed.clear-existing=true
   ```
   Start the application - existing data will be cleared and new data seeded.

3. **Disable Seeding**:
   ```properties
   app.seed.test-data=false
   ```
   Start the application - no seeding will occur.

### Example Log Output

```
Starting test data seeding...
Seeding regions...
Seeding sub-regions...
Seeding local governments...
Seeding counties...
Seeding sub-counties...
Seeding parishes...
Test data seeding completed successfully:
  - 3 Regions
  - 6 Sub-Regions
  - 12 Local Governments
  - 24 Counties
  - 48 Sub-Counties
  - 96 Parishes
Total: 189 administrative areas
```

## Method 2: SQL Migration Script

### Using Flyway

The SQL script `V2__seed_test_data.sql` will run automatically if:
- Flyway is enabled
- Tables exist
- No data exists in the `region` table

### Manual SQL Execution

You can also run the SQL script manually:

```bash
psql -U siip-db-user-dev -d areadevdb -p 5438 -f src/main/resources/db/migration/V2__seed_test_data.sql
```

Or via psql:

```sql
\i src/main/resources/db/migration/V2__seed_test_data.sql
```

## Test Data Codes

### Regions
- `REG001` - Central Region
- `REG002` - Northern Region
- `REG003` - Eastern Region

### Example Query Codes

**Get all regions:**
```bash
GET /AdministrativeAreas/filterList?type=REGION
```

**Get sub-regions in REG001:**
```bash
GET /AdministrativeAreas/filterList?type=SUB REGION&partOf=REG001
```

**Get local governments in SR001:**
```bash
GET /AdministrativeAreas/filterList?type=LOCAL GOVERNMENT&partOf=SR001
```

**Get counties in LG001:**
```bash
GET /AdministrativeAreas/filterList?type=COUNTY&partOf=LG001
```

**Get sub-counties in CT001:**
```bash
GET /AdministrativeAreas/filterList?type=SUB COUNTY&partOf=CT001
```

**Get parishes in SC001:**
```bash
GET /AdministrativeAreas/filterList?type=PARISH&partOf=SC001
```

**Get all parishes in REG001:**
```bash
GET /AdministrativeAreas/parishListByPartOf?type=REGION&partOfCode=REG001
```

## Verification

### Check Data via API

```bash
# Check regions
curl "http://localhost:8084/AdministrativeAreas/filterList?type=REGION"

# Check sub-regions
curl "http://localhost:8084/AdministrativeAreas/filterList?type=SUB REGION&partOf=REG001"

# Check parishes
curl "http://localhost:8084/AdministrativeAreas/filterList?type=PARISH&partOf=SC001"
```

### Check Data via Database

```sql
-- Count records
SELECT 'Regions' as type, COUNT(*) as count FROM region
UNION ALL
SELECT 'Sub-Regions', COUNT(*) FROM subregion
UNION ALL
SELECT 'Local Governments', COUNT(*) FROM localgovernment
UNION ALL
SELECT 'Counties', COUNT(*) FROM county
UNION ALL
SELECT 'Sub-Counties', COUNT(*) FROM subcounty
UNION ALL
SELECT 'Parishes', COUNT(*) FROM parish;

-- View sample data
SELECT code, name FROM region LIMIT 5;
SELECT code, name FROM subregion LIMIT 5;
```

## Using Test Data for Swagger Testing

1. **Enable Seeding**:
   ```properties
   app.seed.test-data=true
   ```

2. **Start Application**:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev1
   ```

3. **Wait for Seeding**:
   - Check logs for "Test data seeding completed successfully"

4. **Test in Swagger UI**:
   - Open `http://localhost:8084/swagger-ui.html`
   - Try endpoints with test codes:
     - `GET /AdministrativeAreas/filterList?type=REGION`
     - `GET /AdministrativeAreas/filterOne?type=REGION&code=REG001`
     - `GET /AdministrativeAreas/searchList?type=REGION`

## Using Test Data for Load Testing

1. **Ensure Data is Seeded**:
   ```properties
   app.seed.test-data=true
   ```

2. **Start Application**:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev1
   ```

3. **Run Load Tests**:
   ```bash
   # Using k6
   cd load-tests
   k6 run k6-baseline-test.js
   
   # Using curl script
   ./load-tests/curl-load-test.sh http://localhost:8084 10 100
   ```

4. **Monitor Cache Performance**:
   - First requests hit database (slower)
   - Subsequent requests hit cache (faster)
   - Check cache hit rate via Actuator

## Troubleshooting

### Data Not Seeding

1. **Check Configuration**:
   ```properties
   app.seed.test-data=true
   ```

2. **Check Logs**:
   - Look for "Starting test data seeding..."
   - Check for errors

3. **Check Database Connection**:
   - Verify database is accessible
   - Check connection pool settings

### Data Already Exists

If data already exists and you want to reseed:

```properties
app.seed.test-data=true
app.seed.clear-existing=true
```

**⚠️ WARNING**: This will delete all existing data!

### Seeding Fails

1. **Check Entity Relationships**:
   - Ensure parent entities exist before creating children
   - Verify foreign key constraints

2. **Check Unique Constraints**:
   - Codes must be unique
   - Names must be unique within parent scope

3. **Check Database State**:
   - Verify tables exist
   - Check for constraint violations

## Best Practices

1. **Development/Testing**:
   - Enable seeding: `app.seed.test-data=true`
   - Keep `app.seed.clear-existing=false` to preserve data

2. **Load Testing**:
   - Seed data before running tests
   - Use consistent data for reproducible results

3. **Production**:
   - **NEVER** enable seeding in production
   - Use proper data migration scripts
   - Seed data manually if needed

## Disabling Seeding in Production

For production environments, ensure seeding is disabled:

```properties
# Production properties
app.seed.test-data=false
app.seed.clear-existing=false
```

Or use environment variables:

```bash
export APP_SEED_TEST_DATA=false
export APP_SEED_CLEAR_EXISTING=false
```

---

**Status**: ✅ Test Data Seeding Ready  
**Total Test Records**: 189 administrative areas  
**Ready for**: Swagger Testing & Load Testing
