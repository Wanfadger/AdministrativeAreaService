# API Query Parameter Examples

This document provides comprehensive examples of how to use query parameters with the Administrative Area API.

## Valid Administrative Area Types

The `type` parameter accepts the following values (case-insensitive):
- `REGION`
- `SUB REGION` (note the space)
- `LOCAL GOVERNMENT` (note the space)
- `COUNTY`
- `SUB COUNTY` (note the space)
- `PARISH`

## Endpoint Examples

### 1. GET /AdministrativeAreas/filterList

Get a list of administrative areas (code and name only).

**Examples:**

```bash
# Get all regions
GET /AdministrativeAreas/filterList?type=REGION

# Get all sub-regions in a specific region
GET /AdministrativeAreas/filterList?type=SUB REGION&partOf=001

# Get all local governments in a sub-region
GET /AdministrativeAreas/filterList?type=LOCAL GOVERNMENT&partOf=SR001

# Get all counties in a local government
GET /AdministrativeAreas/filterList?type=COUNTY&partOf=LG001

# Get all sub-counties in a county
GET /AdministrativeAreas/filterList?type=SUB COUNTY&partOf=CT001

# Get all parishes in a sub-county
GET /AdministrativeAreas/filterList?type=PARISH&partOf=SC001
```

**cURL Examples:**

```bash
# All regions
curl "http://localhost:8084/AdministrativeAreas/filterList?type=REGION"

# Sub-regions by parent region
curl "http://localhost:8084/AdministrativeAreas/filterList?type=SUB REGION&partOf=001"

# Counties by local government
curl "http://localhost:8084/AdministrativeAreas/filterList?type=COUNTY&partOf=LG001"
```

---

### 2. GET /AdministrativeAreas/filterOne

Get a single administrative area by code (code and name only).

**Examples:**

```bash
# Get a specific region
GET /AdministrativeAreas/filterOne?type=REGION&code=001

# Get a specific sub-region
GET /AdministrativeAreas/filterOne?type=SUB REGION&code=SR001&partOf=001

# Get a specific parish
GET /AdministrativeAreas/filterOne?type=PARISH&code=P001&partOf=SC001
```

**cURL Examples:**

```bash
# Get region by code
curl "http://localhost:8084/AdministrativeAreas/filterOne?type=REGION&code=001"

# Get sub-region with parent
curl "http://localhost:8084/AdministrativeAreas/filterOne?type=SUB REGION&code=SR001&partOf=001"
```

---

### 3. GET /AdministrativeAreas/searchList

Get a list of administrative areas with full details (code, name, latitude, longitude).

**Examples:**

```bash
# Get all regions with coordinates
GET /AdministrativeAreas/searchList?type=REGION

# Get local governments in a sub-region with coordinates
GET /AdministrativeAreas/searchList?type=LOCAL GOVERNMENT&partOf=SR001

# Get counties with coordinates
GET /AdministrativeAreas/searchList?type=COUNTY&partOf=LG001
```

**cURL Examples:**

```bash
# All regions with full details
curl "http://localhost:8084/AdministrativeAreas/searchList?type=REGION"

# Local governments by sub-region
curl "http://localhost:8084/AdministrativeAreas/searchList?type=LOCAL GOVERNMENT&partOf=SR001"
```

---

### 4. GET /AdministrativeAreas/searchOne

Get a single administrative area with full details (code, name, latitude, longitude).

**Examples:**

```bash
# Get region with coordinates
GET /AdministrativeAreas/searchOne?type=REGION&code=001

# Get parish with coordinates
GET /AdministrativeAreas/searchOne?type=PARISH&code=P001&partOf=SC001
```

**cURL Examples:**

```bash
# Get region with full details
curl "http://localhost:8084/AdministrativeAreas/searchOne?type=REGION&code=001"

# Get parish with parent context
curl "http://localhost:8084/AdministrativeAreas/searchOne?type=PARISH&code=P001&partOf=SC001"
```

---

### 5. GET /AdministrativeAreas/parishListByPartOf

Get all parishes that belong to a parent administrative area.

**Examples:**

```bash
# Get parishes in a region
GET /AdministrativeAreas/parishListByPartOf?type=REGION&partOfCode=001

# Get parishes in a sub-region
GET /AdministrativeAreas/parishListByPartOf?type=SUB REGION&partOfCode=SR001

# Get parishes in a local government
GET /AdministrativeAreas/parishListByPartOf?type=LOCAL GOVERNMENT&partOfCode=LG001

# Get parishes in a county
GET /AdministrativeAreas/parishListByPartOf?type=COUNTY&partOfCode=CT001

# Get parishes in a sub-county
GET /AdministrativeAreas/parishListByPartOf?type=SUB COUNTY&partOfCode=SC001
```

**cURL Examples:**

```bash
# Parishes by region
curl "http://localhost:8084/AdministrativeAreas/parishListByPartOf?type=REGION&partOfCode=001"

# Parishes by county
curl "http://localhost:8084/AdministrativeAreas/parishListByPartOf?type=COUNTY&partOfCode=CT001"
```

---

### 6. POST /AdministrativeAreas/one

Create a new administrative area.

**Query Parameters:**
- `type` (required): Administrative area type

**Request Body:**
```json
{
  "name": "Central Region",
  "description": "Central Region of Uganda",
  "latitude": "0.3476",
  "longitude": "32.5825"
}
```

**Examples:**

```bash
# Create a region
POST /AdministrativeAreas/one?type=REGION
Body: { "name": "Central Region", "description": "...", "latitude": "0.3476", "longitude": "32.5825" }

# Create a sub-region (requires partOfCode)
POST /AdministrativeAreas/one?type=SUB REGION
Body: { "name": "Kampala Sub-Region", "partOfCode": "001", "latitude": "0.3136", "longitude": "32.5811" }

# Create a parish (requires partOfCode)
POST /AdministrativeAreas/one?type=PARISH
Body: { "name": "Kampala Parish", "partOfCode": "SC001", "latitude": "0.3136", "longitude": "32.5811" }
```

**cURL Examples:**

```bash
# Create region
curl -X POST "http://localhost:8084/AdministrativeAreas/one?type=REGION" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Central Region",
    "description": "Central Region of Uganda",
    "latitude": "0.3476",
    "longitude": "32.5825"
  }'

# Create sub-region
curl -X POST "http://localhost:8084/AdministrativeAreas/one?type=SUB REGION" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Kampala Sub-Region",
    "partOfCode": "001",
    "latitude": "0.3136",
    "longitude": "32.5811"
  }'
```

---

### 7. POST /AdministrativeAreas/list

Create multiple administrative areas.

**Query Parameters:**
- `type` (required): Administrative area type

**Request Body:**
```json
[
  {
    "name": "Region 1",
    "description": "Description 1",
    "latitude": "0.3476",
    "longitude": "32.5825"
  },
  {
    "name": "Region 2",
    "description": "Description 2",
    "latitude": "0.3477",
    "longitude": "32.5826"
  }
]
```

**cURL Example:**

```bash
curl -X POST "http://localhost:8084/AdministrativeAreas/list?type=REGION" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "name": "Central Region",
      "description": "Central Region",
      "latitude": "0.3476",
      "longitude": "32.5825"
    },
    {
      "name": "Northern Region",
      "description": "Northern Region",
      "latitude": "2.5000",
      "longitude": "32.5000"
    }
  ]'
```

---

### 8. PUT /AdministrativeAreas/one

Update an existing administrative area.

**Query Parameters:**
- `type` (required): Administrative area type

**Request Body:**
```json
{
  "code": "001",
  "name": "Updated Region Name",
  "description": "Updated description",
  "latitude": "0.3476",
  "longitude": "32.5825"
}
```

**cURL Example:**

```bash
curl -X PUT "http://localhost:8084/AdministrativeAreas/one?type=REGION" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "001",
    "name": "Updated Central Region",
    "description": "Updated description",
    "latitude": "0.3476",
    "longitude": "32.5825"
  }'
```

---

## Query Parameter Reference

### Common Parameters

| Parameter | Required | Description | Example Values |
|-----------|----------|-------------|----------------|
| `type` | Yes | Administrative area type | `REGION`, `SUB REGION`, `LOCAL GOVERNMENT`, `COUNTY`, `SUB COUNTY`, `PARISH` |
| `code` | Yes (for single item endpoints) | Unique code of the area | `001`, `SR001`, `LG001`, `CT001`, `SC001`, `P001` |
| `partOf` | Optional | Parent administrative area code | `001`, `SR001`, `LG001` |
| `partOfCode` | Yes (for parishListByPartOf) | Parent administrative area code | `001`, `SR001`, `LG001`, `CT001`, `SC001` |

### Parameter Notes

1. **Type Values**: Must match exactly (case-insensitive):
   - `REGION` (not `region` or `Region`)
   - `SUB REGION` (with space, not `SUBREGION` or `SUB-REGION`)
   - `LOCAL GOVERNMENT` (with space, not `LOCALGOVERNMENT`)
   - `COUNTY`
   - `SUB COUNTY` (with space)
   - `PARISH`

2. **Hierarchical Queries**: 
   - For `SUB REGION`, `LOCAL GOVERNMENT`, `COUNTY`, `SUB COUNTY`, `PARISH`: `partOf` is often required
   - `partOf` should contain the code of the parent area

3. **URL Encoding**: 
   - Spaces in type values should be URL encoded as `%20` or `+`
   - Example: `SUB REGION` → `SUB%20REGION` or `SUB+REGION`

---

## Complete Workflow Example

### 1. Get all regions
```bash
GET /AdministrativeAreas/filterList?type=REGION
```

### 2. Get sub-regions in a region
```bash
GET /AdministrativeAreas/filterList?type=SUB REGION&partOf=001
```

### 3. Get local governments in a sub-region
```bash
GET /AdministrativeAreas/filterList?type=LOCAL GOVERNMENT&partOf=SR001
```

### 4. Get counties in a local government
```bash
GET /AdministrativeAreas/filterList?type=COUNTY&partOf=LG001
```

### 5. Get sub-counties in a county
```bash
GET /AdministrativeAreas/filterList?type=SUB COUNTY&partOf=CT001
```

### 6. Get parishes in a sub-county
```bash
GET /AdministrativeAreas/filterList?type=PARISH&partOf=SC001
```

### 7. Get all parishes in a region (using parishListByPartOf)
```bash
GET /AdministrativeAreas/parishListByPartOf?type=REGION&partOfCode=001
```

---

## Testing in Swagger UI

1. Open Swagger UI: `http://localhost:8084/swagger-ui.html`
2. Find the endpoint you want to test
3. Click "Try it out"
4. Enter query parameters in the parameter fields
5. For POST/PUT, enter the request body in JSON format
6. Click "Execute"
7. View the JSON response

---

## Response Format

All endpoints return responses in this format:

```json
{
  "data": <response_data>,
  "message": "success",
  "status": true
}
```

**Example Response (filterList):**
```json
{
  "data": [
    {
      "code": "001",
      "name": "Central Region"
    },
    {
      "code": "002",
      "name": "Northern Region"
    }
  ],
  "message": "success",
  "status": true
}
```

**Example Response (searchOne):**
```json
{
  "data": {
    "code": "001",
    "name": "Central Region",
    "latitude": "0.3476",
    "longitude": "32.5825"
  },
  "message": "success",
  "status": true
}
```

---

## Common Errors

### Error: "Missing Administrative Area Type"
- **Cause**: `type` parameter is missing or invalid
- **Solution**: Ensure `type` is one of the valid values (case-insensitive)

### Error: "Missing Administrative Area partOf"
- **Cause**: `partOf` is required for hierarchical types but not provided
- **Solution**: Add `partOf` parameter with parent code

### Error: 404 Not Found
- **Cause**: Administrative area with the specified code doesn't exist
- **Solution**: Verify the code is correct

---

## Tips

1. **Caching**: GET endpoints are cached. First request may be slower (hits database), subsequent requests are faster (from cache).

2. **URL Encoding**: When using spaces in type values, encode them:
   - `SUB REGION` → `SUB%20REGION` or `SUB+REGION`
   - `LOCAL GOVERNMENT` → `LOCAL%20GOVERNMENT` or `LOCAL+GOVERNMENT`

3. **Hierarchical Navigation**: Use the hierarchy:
   - Region → Sub-Region → Local Government → County → Sub-County → Parish

4. **Code Format**: Codes are typically alphanumeric (e.g., `001`, `SR001`, `LG001`)

---

**For more details, see Swagger UI at:** `http://localhost:8084/swagger-ui.html`
