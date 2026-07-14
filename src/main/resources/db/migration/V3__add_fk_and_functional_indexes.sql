-- Performance indexes + a correctness fix for the name-uniqueness scope.
--
-- The restored production database arrived with ZERO indexes beyond primary keys and unique
-- constraints. Every lookup against 3,517 parishes is a sequential scan today. The @Index
-- annotations on the entities exist only in Java; this database predates them.
--
-- Three groups below, in order of impact.

-- ---------------------------------------------------------------------------------------------
-- (a) FUNCTIONAL indexes on upper(code) / upper(name).
--
-- Spring Data renders `findByCodeIgnoreCase` as `upper(code) = upper(?)`. A plain b-tree on
-- `code` CANNOT serve that predicate — Postgres will not use it. So the unique constraints on
-- `code`, and the idx_<table>_code indexes Hibernate would create, are both useless to the query
-- the application actually runs.
--
-- Every getOne, every partOf parent resolution and every duplicate check goes through one of
-- these predicates. This is the single largest database win available.
-- ---------------------------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_region_code_upper           ON region          (upper(code));
CREATE INDEX IF NOT EXISTS idx_region_name_upper           ON region          (upper(name));
CREATE INDEX IF NOT EXISTS idx_subregion_code_upper        ON subregion       (upper(code));
CREATE INDEX IF NOT EXISTS idx_subregion_name_upper        ON subregion       (upper(name));
CREATE INDEX IF NOT EXISTS idx_localgovernment_code_upper  ON localgovernment (upper(code));
CREATE INDEX IF NOT EXISTS idx_localgovernment_name_upper  ON localgovernment (upper(name));
CREATE INDEX IF NOT EXISTS idx_county_code_upper           ON county          (upper(code));
CREATE INDEX IF NOT EXISTS idx_county_name_upper           ON county          (upper(name));
CREATE INDEX IF NOT EXISTS idx_subcounty_code_upper        ON subcounty       (upper(code));
CREATE INDEX IF NOT EXISTS idx_subcounty_name_upper        ON subcounty       (upper(name));
CREATE INDEX IF NOT EXISTS idx_parish_code_upper           ON parish          (upper(code));
CREATE INDEX IF NOT EXISTS idx_parish_name_upper           ON parish          (upper(name));

-- ---------------------------------------------------------------------------------------------
-- (b) The real read path: partOf filter + @SQLRestriction("archived = false") + ORDER BY name.
--
-- No foreign-key column is indexed at all today, so `?type=PARISH&partOf=<subcounty>` seq-scans
-- every parish.
--
-- PARTIAL indexes (WHERE archived = false): soft-deleted rows are a rounding error, so the partial
-- index is effectively the whole table minus tombstones — smaller than a full index, and it
-- matches the restriction Hibernate appends to every single query.
--
-- (parent_fk, name) covers both the filter and the sort, so the planner can satisfy
-- "children of X, ordered by name" from the index alone.
-- ---------------------------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_subregion_region_name
    ON subregion (region_id, name) WHERE archived = false;
CREATE INDEX IF NOT EXISTS idx_localgovernment_subregion_name
    ON localgovernment (subregion_id, name) WHERE archived = false;
CREATE INDEX IF NOT EXISTS idx_county_localgovernment_name
    ON county (localgovernment_id, name) WHERE archived = false;
CREATE INDEX IF NOT EXISTS idx_subcounty_county_name
    ON subcounty (county_id, name) WHERE archived = false;
CREATE INDEX IF NOT EXISTS idx_parish_subcounty_name
    ON parish (subcounty_id, name) WHERE archived = false;

-- The unfiltered sorted scan (the map view pulls a whole level ordered by name).
CREATE INDEX IF NOT EXISTS idx_region_name_active          ON region          (name) WHERE archived = false;
CREATE INDEX IF NOT EXISTS idx_subregion_name_active       ON subregion       (name) WHERE archived = false;
CREATE INDEX IF NOT EXISTS idx_localgovernment_name_active ON localgovernment (name) WHERE archived = false;
CREATE INDEX IF NOT EXISTS idx_county_name_active          ON county          (name) WHERE archived = false;
CREATE INDEX IF NOT EXISTS idx_subcounty_name_active       ON subcounty       (name) WHERE archived = false;
CREATE INDEX IF NOT EXISTS idx_parish_name_active          ON parish          (name) WHERE archived = false;

-- ---------------------------------------------------------------------------------------------
-- (c) CORRECTNESS: scope the name-uniqueness of subregion/localgovernment to their parent.
--
-- The service checks for duplicates WITHIN A PARENT (findByNameIgnoreCaseAndRegion_Code), but the
-- database enforces a GLOBAL unique on name. So creating a legitimately-named sub-region under a
-- different region passes the application check and then dies on a raw
-- DataIntegrityViolationException -> 500 instead of a clean 409.
--
-- The replacement is also archive-aware (WHERE archived = false), which additionally fixes the
-- documented wart that a soft-deleted name/code could never be reused.
--
-- The constraint is dropped BY DISCOVERY, not by name: production carries Hibernate's generated
-- hash names (uk_g9r3n28cw8p15twou67spnw0d) while a database freshly built from V1 carries the
-- readable ones (uk_subregion_name). Hard-coding either would break the other.
--
-- Verified against the restored data before writing this: zero case-insensitive duplicate names
-- within any parent at any level, so these unique indexes build cleanly.
-- ---------------------------------------------------------------------------------------------
DO $$
DECLARE
    tbl   text;
    con   text;
BEGIN
    FOREACH tbl IN ARRAY ARRAY['subregion', 'localgovernment'] LOOP
        -- Find any UNIQUE constraint whose definition is exactly UNIQUE (name).
        SELECT c.conname INTO con
        FROM pg_constraint c
        JOIN pg_class r ON r.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = r.relnamespace
        WHERE n.nspname = 'public'
          AND r.relname = tbl
          AND c.contype = 'u'
          AND pg_get_constraintdef(c.oid) = 'UNIQUE (name)'
        LIMIT 1;

        IF con IS NOT NULL THEN
            EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', tbl, con);
            RAISE NOTICE 'Dropped global UNIQUE(name) constraint %.%', tbl, con;
        END IF;
        con := NULL;
    END LOOP;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_subregion_name_in_region
    ON subregion (upper(name), region_id) WHERE archived = false;
CREATE UNIQUE INDEX IF NOT EXISTS uk_localgovernment_name_in_subregion
    ON localgovernment (upper(name), subregion_id) WHERE archived = false;
