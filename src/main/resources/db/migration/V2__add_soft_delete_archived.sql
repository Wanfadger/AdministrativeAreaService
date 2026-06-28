-- Soft-delete support: ensure an `archived` flag + supporting index on every
-- administrative-area table.
--
-- This migration is defensive on purpose. Flyway runs BEFORE Hibernate, so on a
-- brand-new database the tables do not exist yet — there, Hibernate (ddl-auto=update)
-- creates the column and the idx_<table>_archived index from the entity mappings, and
-- this migration is a guarded no-op. On an existing / restored-production database the
-- tables already exist, so this migration adds the column + index idempotently.
--
-- Each table is wrapped in `IF to_regclass(...) IS NOT NULL` so CREATE INDEX never runs
-- against a missing table (CREATE INDEX IF NOT EXISTS does NOT guard against that).
--
-- TABLE NAMES depend on the configured Hibernate naming strategies:
--   implicit = ImplicitNamingStrategyLegacyJpaImpl
--   physical = PhysicalNamingStrategyStandardImpl  (NO camelCase -> snake_case)
-- => table names equal the entity class names, unquoted, folded to lowercase by
--    PostgreSQL: region, subregion, localgovernment, county, subcounty, parish.

DO $$
BEGIN
    IF to_regclass('region') IS NOT NULL THEN
        ALTER TABLE region ADD COLUMN IF NOT EXISTS archived boolean NOT NULL DEFAULT false;
        CREATE INDEX IF NOT EXISTS idx_region_archived ON region (archived);
    END IF;

    IF to_regclass('subregion') IS NOT NULL THEN
        ALTER TABLE subregion ADD COLUMN IF NOT EXISTS archived boolean NOT NULL DEFAULT false;
        CREATE INDEX IF NOT EXISTS idx_subregion_archived ON subregion (archived);
    END IF;

    IF to_regclass('localgovernment') IS NOT NULL THEN
        ALTER TABLE localgovernment ADD COLUMN IF NOT EXISTS archived boolean NOT NULL DEFAULT false;
        CREATE INDEX IF NOT EXISTS idx_localgovernment_archived ON localgovernment (archived);
    END IF;

    IF to_regclass('county') IS NOT NULL THEN
        ALTER TABLE county ADD COLUMN IF NOT EXISTS archived boolean NOT NULL DEFAULT false;
        CREATE INDEX IF NOT EXISTS idx_county_archived ON county (archived);
    END IF;

    IF to_regclass('subcounty') IS NOT NULL THEN
        ALTER TABLE subcounty ADD COLUMN IF NOT EXISTS archived boolean NOT NULL DEFAULT false;
        CREATE INDEX IF NOT EXISTS idx_subcounty_archived ON subcounty (archived);
    END IF;

    IF to_regclass('parish') IS NOT NULL THEN
        ALTER TABLE parish ADD COLUMN IF NOT EXISTS archived boolean NOT NULL DEFAULT false;
        CREATE INDEX IF NOT EXISTS idx_parish_archived ON parish (archived);
    END IF;
END $$;
