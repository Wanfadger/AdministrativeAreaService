-- Performance Indexes for Administrative Area API
-- This migration adds critical indexes that cannot be created via JPA annotations:
-- 1. Function-based indexes (LOWER(name)) - JPA annotations cannot create these
-- 2. Foreign key indexes - Not automatically created by Hibernate
-- 3. Composite indexes - Better managed via SQL for complex patterns
--
-- Note: Simple code and name indexes are handled by @Table annotations in entity classes.
-- This migration focuses on indexes that require SQL expressions or are not auto-created.
--
-- Execution order:
-- 1. Hibernate creates tables and simple indexes from @Table annotations (if ddl-auto=update)
-- 2. Flyway runs this migration to add complex indexes
-- 3. IF NOT EXISTS ensures idempotency if indexes already exist

-- Function to safely create index only if table exists
DO $$
BEGIN
    -- Name indexes with LOWER() for case-insensitive lookups
    -- These cannot be created via JPA @Table annotations (function-based indexes)
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'region') THEN
        CREATE INDEX IF NOT EXISTS idx_region_name_lower ON region(LOWER(name));
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'subregion') THEN
        CREATE INDEX IF NOT EXISTS idx_subregion_name_lower ON subregion(LOWER(name));
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'localgovernment') THEN
        CREATE INDEX IF NOT EXISTS idx_localgovernment_name_lower ON localgovernment(LOWER(name));
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'county') THEN
        CREATE INDEX IF NOT EXISTS idx_county_name_lower ON county(LOWER(name));
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'subcounty') THEN
        CREATE INDEX IF NOT EXISTS idx_subcounty_name_lower ON subcounty(LOWER(name));
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'parish') THEN
        CREATE INDEX IF NOT EXISTS idx_parish_name_lower ON parish(LOWER(name));
    END IF;

    -- Foreign key indexes (critical for JOINs and hierarchical queries)
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'subregion') THEN
        CREATE INDEX IF NOT EXISTS idx_subregion_region_id ON subregion(region_id);
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'localgovernment') THEN
        CREATE INDEX IF NOT EXISTS idx_localgovernment_subregion_id ON localgovernment(subregion_id);
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'county') THEN
        CREATE INDEX IF NOT EXISTS idx_county_localgovernment_id ON county(localgovernment_id);
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'subcounty') THEN
        CREATE INDEX IF NOT EXISTS idx_subcounty_county_id ON subcounty(county_id);
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'parish') THEN
        CREATE INDEX IF NOT EXISTS idx_parish_subcounty_id ON parish(subcounty_id);
    END IF;

    -- Composite indexes for common query patterns
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'subregion') THEN
        CREATE INDEX IF NOT EXISTS idx_subregion_name_region ON subregion(LOWER(name), region_id);
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'localgovernment') THEN
        CREATE INDEX IF NOT EXISTS idx_localgovernment_name_subregion ON localgovernment(LOWER(name), subregion_id);
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'county') THEN
        CREATE INDEX IF NOT EXISTS idx_county_name_localgovernment ON county(LOWER(name), localgovernment_id);
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'subcounty') THEN
        CREATE INDEX IF NOT EXISTS idx_subcounty_name_county ON subcounty(LOWER(name), county_id);
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'parish') THEN
        CREATE INDEX IF NOT EXISTS idx_parish_name_subcounty ON parish(LOWER(name), subcounty_id);
    END IF;

    -- Analyze tables after index creation for query planner optimization
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'region') THEN
        ANALYZE region;
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'subregion') THEN
        ANALYZE subregion;
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'localgovernment') THEN
        ANALYZE localgovernment;
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'county') THEN
        ANALYZE county;
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'subcounty') THEN
        ANALYZE subcounty;
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'parish') THEN
        ANALYZE parish;
    END IF;
END $$;
