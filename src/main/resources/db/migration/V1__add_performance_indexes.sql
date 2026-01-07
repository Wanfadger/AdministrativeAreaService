-- Performance Indexes for Administrative Area API
-- This migration adds critical indexes to improve query performance
-- Note: This migration will only create indexes if the tables exist
-- Tables are created by Hibernate's ddl-auto=update before this migration runs

-- Function to safely create index only if table exists
DO $$
BEGIN
    -- Code indexes (most frequently queried field)
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'region') THEN
        CREATE INDEX IF NOT EXISTS idx_region_code ON region(code);
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'subregion') THEN
        CREATE INDEX IF NOT EXISTS idx_subregion_code ON subregion(code);
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'localgovernment') THEN
        CREATE INDEX IF NOT EXISTS idx_localgovernment_code ON localgovernment(code);
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'county') THEN
        CREATE INDEX IF NOT EXISTS idx_county_code ON county(code);
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'subcounty') THEN
        CREATE INDEX IF NOT EXISTS idx_subcounty_code ON subcounty(code);
    END IF;
    
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'parish') THEN
        CREATE INDEX IF NOT EXISTS idx_parish_code ON parish(code);
    END IF;

    -- Name indexes (case-insensitive lookups)
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
