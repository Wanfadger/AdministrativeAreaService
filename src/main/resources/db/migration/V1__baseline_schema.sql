-- Baseline schema: the six administrative-area tables.
--
-- WHEN THIS RUNS
--   Fresh/empty database  -> Flyway applies V1, V2, V3 in order. V1 creates everything.
--   Existing database     -> spring.flyway.baseline-on-migrate=true makes Flyway stamp the
--                            existing schema at version 1 and SKIP V1, then apply V2 and V3.
--   So on the restored production database this file never executes. It is still written to be
--   idempotent, because a baseline you cannot safely re-run is a baseline you cannot trust.
--
-- SHAPE
--   Transcribed from the real restored production schema (pg_dump --schema-only, taken BEFORE the
--   app was ever started against it, so Hibernate's ddl-auto=update had no chance to mutate it).
--   Column types, nullability and the `archived`-less shape are exactly as production had them —
--   `archived` is deliberately absent here and added by V2, which is the true history.
--
-- TABLE / COLUMN NAMES
--   Determined by the configured naming strategies:
--     implicit = ImplicitNamingStrategyLegacyJpaImpl
--     physical = PhysicalNamingStrategyStandardImpl   (no camelCase -> snake_case)
--   => table names are the entity class names folded to lowercase by PostgreSQL, and the FK
--      columns are region_id, subregion_id, localgovernment_id, county_id, subcounty_id.
--      These were VERIFIED against information_schema, not assumed.
--
-- NOT INCLUDED
--   The production database also carries a `company` table (0 rows). It maps to no entity and is
--   not part of this service's model, so the baseline does not claim ownership of it.
--
-- CONSTRAINT NAMES
--   Production's unique constraints carry Hibernate-generated hash names (uk_2ylxp2r1...). New
--   databases get the readable names below instead. Nothing may depend on a constraint's *name* —
--   V3 discovers them dynamically for exactly this reason.

CREATE TABLE IF NOT EXISTS region (
    id                character varying(255) NOT NULL,
    code              character varying(255) NOT NULL,
    name              character varying(255) NOT NULL,
    description       character varying(255),
    latitude          double precision,
    longitude         double precision,
    createddatetime   timestamp(6) without time zone,
    updateddatetime   timestamp(6) without time zone,
    CONSTRAINT region_pkey     PRIMARY KEY (id),
    CONSTRAINT uk_region_code  UNIQUE (code),
    CONSTRAINT uk_region_name  UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS subregion (
    id                character varying(255) NOT NULL,
    code              character varying(255) NOT NULL,
    name              character varying(255) NOT NULL,
    description       character varying(255),
    latitude          double precision,
    longitude         double precision,
    createddatetime   timestamp(6) without time zone,
    updateddatetime   timestamp(6) without time zone,
    region_id         character varying(255) NOT NULL,
    CONSTRAINT subregion_pkey    PRIMARY KEY (id),
    CONSTRAINT uk_subregion_code UNIQUE (code),
    -- Global UNIQUE(name), matching production. V3 replaces this with a parent-scoped constraint:
    -- the service only ever checks for duplicates *within a parent*, so a global unique turns a
    -- legitimate create into a raw DataIntegrityViolationException -> 500.
    CONSTRAINT uk_subregion_name UNIQUE (name),
    CONSTRAINT fk_subregion_region FOREIGN KEY (region_id) REFERENCES region (id)
);

CREATE TABLE IF NOT EXISTS localgovernment (
    id                character varying(255) NOT NULL,
    code              character varying(255) NOT NULL,
    name              character varying(255) NOT NULL,
    description       character varying(255),
    latitude          double precision,
    longitude         double precision,
    createddatetime   timestamp(6) without time zone,
    updateddatetime   timestamp(6) without time zone,
    subregion_id      character varying(255) NOT NULL,
    CONSTRAINT localgovernment_pkey    PRIMARY KEY (id),
    CONSTRAINT uk_localgovernment_code UNIQUE (code),
    CONSTRAINT uk_localgovernment_name UNIQUE (name),   -- see note on subregion; V3 replaces this
    CONSTRAINT fk_localgovernment_subregion FOREIGN KEY (subregion_id) REFERENCES subregion (id)
);

CREATE TABLE IF NOT EXISTS county (
    id                  character varying(255) NOT NULL,
    code                character varying(255) NOT NULL,
    name                character varying(255) NOT NULL,
    description         character varying(255),
    latitude            double precision,
    longitude           double precision,
    createddatetime     timestamp(6) without time zone,
    updateddatetime     timestamp(6) without time zone,
    localgovernment_id  character varying(255) NOT NULL,
    CONSTRAINT county_pkey    PRIMARY KEY (id),
    CONSTRAINT uk_county_code UNIQUE (code),
    CONSTRAINT fk_county_localgovernment FOREIGN KEY (localgovernment_id) REFERENCES localgovernment (id)
);

CREATE TABLE IF NOT EXISTS subcounty (
    id                character varying(255) NOT NULL,
    code              character varying(255) NOT NULL,
    name              character varying(255) NOT NULL,
    description       character varying(255),
    latitude          double precision,
    longitude         double precision,
    createddatetime   timestamp(6) without time zone,
    updateddatetime   timestamp(6) without time zone,
    county_id         character varying(255) NOT NULL,
    CONSTRAINT subcounty_pkey    PRIMARY KEY (id),
    CONSTRAINT uk_subcounty_code UNIQUE (code),
    CONSTRAINT fk_subcounty_county FOREIGN KEY (county_id) REFERENCES county (id)
);

CREATE TABLE IF NOT EXISTS parish (
    id                character varying(255) NOT NULL,
    code              character varying(255) NOT NULL,
    name              character varying(255) NOT NULL,
    description       character varying(255),
    latitude          double precision,
    longitude         double precision,
    createddatetime   timestamp(6) without time zone,
    updateddatetime   timestamp(6) without time zone,
    subcounty_id      character varying(255) NOT NULL,
    CONSTRAINT parish_pkey    PRIMARY KEY (id),
    CONSTRAINT uk_parish_code UNIQUE (code),
    CONSTRAINT fk_parish_subcounty FOREIGN KEY (subcounty_id) REFERENCES subcounty (id)
);
