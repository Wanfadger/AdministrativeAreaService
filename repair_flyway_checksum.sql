-- Flyway Checksum Repair Script
-- Run this SQL script against your database to repair the checksum mismatch
-- for V1__add_performance_indexes.sql migration

-- Update the checksum to match the new file content
UPDATE flyway_schema_history 
SET checksum = 1703871227 
WHERE version = '1' AND script = 'V1__add_performance_indexes.sql';

-- Verify the update
SELECT version, script, checksum, installed_on, success
FROM flyway_schema_history 
WHERE version = '1';

-- Expected result: checksum should be 1703871227
