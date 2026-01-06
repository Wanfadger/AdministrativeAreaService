# Build Verification Guide - Phase 2

## Quick Build Test

### Step 1: Start Redis Stack
```bash
# Start Redis Stack (includes Redis + Redis Insight)
docker-compose up -d redis

# Verify it's running
docker-compose ps

# Check logs
docker-compose logs redis
```

### Step 2: Verify Redis Stack is Accessible
- **Redis**: `localhost:8101`
- **Redis Insight UI**: http://localhost:8001

Open Redis Insight in browser:
- Go to http://localhost:8001
- Click "Add Database"
- Host: `redis` (or `localhost` if connecting from host)
- Port: `6379`
- Name: `Local Redis`
- Click "Add Redis Database"

### Step 3: Build the Project
```bash
# Clean and compile
mvn clean compile

# If successful, package
mvn clean package -DskipTests
```

### Step 4: Verify Dependencies
Check that Flyway is included:
```bash
mvn dependency:tree | grep flyway
```

Should show:
```
org.flywaydb:flyway-core:jar
```

### Step 5: Start Application (Test Mode)
```bash
# Start application
mvn spring-boot:run

# OR if you have the JAR
java -jar target/AdministrativeareaApi-0.0.1-SNAPSHOT.jar
```

### Step 6: Check Application Logs
Look for:
- ✅ Flyway migration success: `Flyway migration successful` or `Successfully applied X migration(s)`
- ✅ Redis connection: `Connected to Redis`
- ✅ Application started: `Started AdministrativeareaApiApplication`

### Step 7: Verify Database Indexes
```bash
# Connect to database
psql -U siip-db-user-dev -d areadevdb -p 5431

# Check indexes
SELECT indexname, tablename 
FROM pg_indexes 
WHERE tablename IN ('region', 'subregion', 'localgovernment', 'county', 'subcounty', 'parish')
ORDER BY tablename, indexname;

# Should show 22 indexes total
```

### Step 8: Test Cache
```bash
# Test endpoint (first call - cache miss)
curl "http://localhost:8084/AdministrativeAreas/filterOne?type=REGION&code=test123"

# Test same endpoint with different parameter order (should hit cache)
curl "http://localhost:8084/AdministrativeAreas/filterOne?code=test123&type=REGION"

# Check Redis Insight - should see cache keys starting with "adminArea."
```

---

## Common Issues & Solutions

### Issue 1: Redis Connection Failed
**Error**: `Unable to connect to Redis`
**Solution**:
```bash
# Check Redis is running
docker-compose ps redis

# Check Redis logs
docker-compose logs redis

# Restart Redis
docker-compose restart redis
```

### Issue 2: Flyway Migration Failed
**Error**: `Migration failed` or `Table already exists`
**Solution**:
- Check if indexes already exist in database
- Check Flyway schema history table: `SELECT * FROM flyway_schema_history;`
- If needed, manually fix migration or reset Flyway baseline

### Issue 3: Port Already in Use
**Error**: `Port 8101 is already allocated`
**Solution**:
```bash
# Check what's using the port
lsof -i :8101
# or
netstat -an | grep 8101

# Stop conflicting service or change port in docker-compose.yml
```

### Issue 4: Database Connection Failed
**Error**: `Connection refused` or `Authentication failed`
**Solution**:
- Verify database is running: `docker-compose ps db`
- Check connection string in `application-dev1.properties`
- Verify credentials match docker-compose.yml

---

## Verification Checklist

- [ ] Redis Stack starts successfully
- [ ] Redis Insight accessible at http://localhost:8001
- [ ] Project compiles without errors
- [ ] Application starts successfully
- [ ] Flyway migration runs (check logs)
- [ ] Database indexes created (verify in database)
- [ ] Redis connection successful (check logs)
- [ ] Cache works (test endpoints)
- [ ] No errors in application logs

---

## Quick Test Commands

```bash
# All-in-one verification script
#!/bin/bash

echo "1. Starting Redis Stack..."
docker-compose up -d redis
sleep 5

echo "2. Building project..."
mvn clean compile

echo "3. Checking Flyway dependency..."
mvn dependency:tree | grep flyway

echo "4. Starting application (background)..."
mvn spring-boot:run &
APP_PID=$!
sleep 15

echo "5. Testing endpoint..."
curl -s "http://localhost:8084/AdministrativeAreas/filterOne?type=REGION&code=test" | head -20

echo "6. Checking Redis keys..."
docker exec -it $(docker-compose ps -q redis) redis-cli KEYS "*"

echo "7. Stopping application..."
kill $APP_PID

echo "Verification complete!"
```

---

## Expected Results

✅ **Build**: Compiles successfully  
✅ **Redis**: Running and accessible  
✅ **Redis Insight**: Available at http://localhost:8001  
✅ **Flyway**: Migration runs automatically  
✅ **Indexes**: 22 indexes created in database  
✅ **Application**: Starts without errors  
✅ **Cache**: Working (keys visible in Redis Insight)  

---

## Next Steps

Once all checks pass:
1. ✅ Confirm build is successful
2. ✅ Proceed to Phase 3: Monitoring & Observability
3. ✅ Add Spring Boot Actuator
4. ✅ Set up monitoring dashboard

---

**Status**: Ready for Build Verification

Please run through the verification steps and confirm when ready for Phase 3!
