-- Performance Indexes for Administrative Area API
-- This migration adds critical indexes to improve query performance

-- Code indexes (most frequently queried field)
CREATE INDEX IF NOT EXISTS idx_region_code ON region(code);
CREATE INDEX IF NOT EXISTS idx_subregion_code ON subregion(code);
CREATE INDEX IF NOT EXISTS idx_localgovernment_code ON localgovernment(code);
CREATE INDEX IF NOT EXISTS idx_county_code ON county(code);
CREATE INDEX IF NOT EXISTS idx_subcounty_code ON subcounty(code);
CREATE INDEX IF NOT EXISTS idx_parish_code ON parish(code);

-- Name indexes (case-insensitive lookups)
CREATE INDEX IF NOT EXISTS idx_region_name_lower ON region(LOWER(name));
CREATE INDEX IF NOT EXISTS idx_subregion_name_lower ON subregion(LOWER(name));
CREATE INDEX IF NOT EXISTS idx_localgovernment_name_lower ON localgovernment(LOWER(name));
CREATE INDEX IF NOT EXISTS idx_county_name_lower ON county(LOWER(name));
CREATE INDEX IF NOT EXISTS idx_subcounty_name_lower ON subcounty(LOWER(name));
CREATE INDEX IF NOT EXISTS idx_parish_name_lower ON parish(LOWER(name));

-- Foreign key indexes (critical for JOINs and hierarchical queries)
CREATE INDEX IF NOT EXISTS idx_subregion_region_id ON subregion(region_id);
CREATE INDEX IF NOT EXISTS idx_localgovernment_subregion_id ON localgovernment(subregion_id);
CREATE INDEX IF NOT EXISTS idx_county_localgovernment_id ON county(localgovernment_id);
CREATE INDEX IF NOT EXISTS idx_subcounty_county_id ON subcounty(county_id);
CREATE INDEX IF NOT EXISTS idx_parish_subcounty_id ON parish(subcounty_id);

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_subregion_name_region ON subregion(LOWER(name), region_id);
CREATE INDEX IF NOT EXISTS idx_localgovernment_name_subregion ON localgovernment(LOWER(name), subregion_id);
CREATE INDEX IF NOT EXISTS idx_county_name_localgovernment ON county(LOWER(name), localgovernment_id);
CREATE INDEX IF NOT EXISTS idx_subcounty_name_county ON subcounty(LOWER(name), county_id);
CREATE INDEX IF NOT EXISTS idx_parish_name_subcounty ON parish(LOWER(name), subcounty_id);

-- Note: Code-based foreign key lookups use the region_id index above
-- If you need to query by parent code frequently, consider adding a functional index:
-- CREATE INDEX IF NOT EXISTS idx_subregion_region_code_lookup ON subregion(region_id);
-- The region_id index above should be sufficient for most queries

-- Analyze tables after index creation for query planner optimization
ANALYZE region;
ANALYZE subregion;
ANALYZE localgovernment;
ANALYZE county;
ANALYZE subcounty;
ANALYZE parish;
