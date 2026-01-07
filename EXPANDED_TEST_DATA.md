# Expanded Test Data Seeding

## Overview
The test data seeder has been expanded to create more comprehensive test data while maintaining all hierarchical relationships.

## Data Structure

### Before (Original):
- 3 Regions
- 6 Sub-Regions (2 per region)
- 12 Local Governments (2 per sub-region)
- 24 Counties (2 per local government)
- 48 Sub-Counties (2 per county)
- 96 Parishes (2 per sub-county)
- **Total: 189 administrative areas**

### After (Expanded):
- **5 Regions** (increased from 3)
- **15 Sub-Regions** (3 per region, increased from 2)
- **45 Local Governments** (3 per sub-region, increased from 2)
- **135 Counties** (3 per local government, increased from 2)
- **405 Sub-Counties** (3 per county, increased from 2)
- **1,215 Parishes** (3 per sub-county, increased from 2)
- **Total: 1,820 administrative areas** (9.6x increase)

## Hierarchical Relationships Maintained

All hierarchical references are properly maintained:

1. **Regions** → Top level (no parent)
2. **Sub-Regions** → Reference their parent Region
3. **Local Governments** → Reference their parent Sub-Region
4. **Counties** → Reference their parent Local Government
5. **Sub-Counties** → Reference their parent County
6. **Parishes** → Reference their parent Sub-County

## New Regions Added

1. Central Region (REG001) - 0.3476°N, 32.5825°E
2. Northern Region (REG002) - 2.5000°N, 32.5000°E
3. Eastern Region (REG003) - 1.3733°N, 33.2041°E
4. **Western Region (REG004)** - 0.2833°N, 30.2667°E (NEW)
5. **Southern Region (REG005)** - -0.6167°N, 30.6667°E (NEW)

## Code Generation

All codes follow a consistent pattern:
- Regions: `REG001`, `REG002`, etc.
- Sub-Regions: `SR001`, `SR002`, etc.
- Local Governments: `LG001`, `LG002`, etc.
- Counties: `CT001`, `CT002`, etc.
- Sub-Counties: `SC001`, `SC002`, etc.
- Parishes: `PR001`, `PR002`, etc.

## Description Fields

All entities include descriptions that reference their parent:
- Example: "Test Central Region Sub-Region 1 Local Government 2 - maintains hierarchical reference to Central Region Sub-Region 1"

This makes it easy to verify hierarchical relationships.

## Usage

### Enable Seeding
Set in `application-dev1.properties`:
```properties
app.seed.test-data=true
app.seed.clear-existing=false  # Set to true to clear and reseed
```

### Clear Existing Data
To clear existing data and reseed:
```properties
app.seed.test-data=true
app.seed.clear-existing=true
```

## Benefits

1. **More Realistic Testing**: Larger dataset better simulates production scenarios
2. **Better Load Testing**: More data for comprehensive load testing
3. **Hierarchy Validation**: All relationships properly maintained
4. **Cache Testing**: More data to test caching effectiveness
5. **Query Testing**: More diverse queries possible

## Verification

After seeding, verify the hierarchy:

```bash
# Check regions
curl "http://localhost:8084/AdministrativeAreas/filterList?type=REGION"

# Check sub-regions for a region
curl "http://localhost:8084/AdministrativeAreas/filterList?type=SUB REGION&partOf=REG001"

# Check local governments for a sub-region
curl "http://localhost:8084/AdministrativeAreas/filterList?type=LOCAL GOVERNMENT&partOf=SR001"

# Continue down the hierarchy...
```

## Performance Considerations

- Seeding 1,820 records may take a few seconds
- All operations are batched for efficiency
- Existing data check prevents duplicate seeding
- Use `app.seed.clear-existing=true` only when needed

## Next Steps

1. Restart the application
2. Check logs for seeding progress
3. Verify data via API endpoints
4. Test caching with the expanded dataset
5. Run load tests with more realistic data volume
