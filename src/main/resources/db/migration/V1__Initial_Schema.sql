-- Initial Schema for Administrative Area API
-- Includes Tables, Constraints, and Performance Indexes

-- 1. Region Table
CREATE TABLE region (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    code VARCHAR(255) NOT NULL UNIQUE,
    area_type VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    archived BOOLEAN DEFAULT FALSE NOT NULL,
    created_date_time TIMESTAMP(6),
    updated_date_time TIMESTAMP(6)
);

-- 2. SubRegion Table
CREATE TABLE sub_region (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    code VARCHAR(255) NOT NULL UNIQUE,
    area_type VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    archived BOOLEAN DEFAULT FALSE NOT NULL,
    created_date_time TIMESTAMP(6),
    updated_date_time TIMESTAMP(6),
    region_id BIGINT NOT NULL,
    CONSTRAINT fk_sub_region_region FOREIGN KEY (region_id) REFERENCES region(id)
);

-- 3. LocalGovernment Table
CREATE TABLE local_government (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    code VARCHAR(255) NOT NULL UNIQUE,
    area_type VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    archived BOOLEAN DEFAULT FALSE NOT NULL,
    created_date_time TIMESTAMP(6),
    updated_date_time TIMESTAMP(6),
    sub_region_id BIGINT NOT NULL,
    CONSTRAINT fk_local_government_sub_region FOREIGN KEY (sub_region_id) REFERENCES sub_region(id)
);

-- 4. County Table
CREATE TABLE county (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL UNIQUE,
    area_type VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    archived BOOLEAN DEFAULT FALSE NOT NULL,
    created_date_time TIMESTAMP(6),
    updated_date_time TIMESTAMP(6),
    local_government_id BIGINT NOT NULL,
    CONSTRAINT fk_county_local_government FOREIGN KEY (local_government_id) REFERENCES local_government(id)
);

-- 5. SubCounty Table
CREATE TABLE sub_county (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL UNIQUE,
    area_type VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    archived BOOLEAN DEFAULT FALSE NOT NULL,
    created_date_time TIMESTAMP(6),
    updated_date_time TIMESTAMP(6),
    county_id BIGINT NOT NULL,
    CONSTRAINT fk_sub_county_county FOREIGN KEY (county_id) REFERENCES county(id)
);

-- 6. Parish Table
CREATE TABLE parish (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL UNIQUE,
    area_type VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    archived BOOLEAN DEFAULT FALSE NOT NULL,
    created_date_time TIMESTAMP(6),
    updated_date_time TIMESTAMP(6),
    sub_county_id BIGINT NOT NULL,
    CONSTRAINT fk_parish_sub_county FOREIGN KEY (sub_county_id) REFERENCES sub_county(id)
);

-- PERFORMANCE INDEXES (Optimized for URRMS patterns)

-- Case-insensitive name indexes
CREATE INDEX idx_region_name_lower ON region (LOWER(name));
CREATE INDEX idx_sub_region_name_lower ON sub_region (LOWER(name));
CREATE INDEX idx_local_government_name_lower ON local_government (LOWER(name));
CREATE INDEX idx_county_name_lower ON county (LOWER(name));
CREATE INDEX idx_sub_county_name_lower ON sub_county (LOWER(name));
CREATE INDEX idx_parish_name_lower ON parish (LOWER(name));

-- Standard search indexes (as defined in entities)
CREATE INDEX idx_region_code ON region (code);
CREATE INDEX idx_sub_region_code ON sub_region (code);
CREATE INDEX idx_local_government_code ON local_government (code);
CREATE INDEX idx_county_code ON county (code);
CREATE INDEX idx_sub_county_code ON sub_county (code);
CREATE INDEX idx_parish_code ON parish (code);

-- Archived column indexes (for soft delete performance)
CREATE INDEX idx_region_archived ON region(archived);
CREATE INDEX idx_sub_region_archived ON sub_region(archived);
CREATE INDEX idx_local_government_archived ON local_government(archived);
CREATE INDEX idx_county_archived ON county(archived);
CREATE INDEX idx_sub_county_archived ON sub_county(archived);
CREATE INDEX idx_parish_archived ON parish(archived);

-- Foreign key indexes (Critical for JOINs)
CREATE INDEX idx_sub_region_region_id ON sub_region (region_id);
CREATE INDEX idx_local_government_sub_region_id ON local_government (sub_region_id);
CREATE INDEX idx_county_local_government_id ON county (local_government_id);
CREATE INDEX idx_sub_county_county_id ON sub_county (county_id);
CREATE INDEX idx_parish_sub_county_id ON parish (sub_county_id);

-- Composite indexes for hierarchical lookup
CREATE INDEX idx_sub_region_name_region ON sub_region (LOWER(name), region_id);
CREATE INDEX idx_local_government_name_sub_region ON local_government (LOWER(name), sub_region_id);
CREATE INDEX idx_county_name_local_government ON county (LOWER(name), local_government_id);
CREATE INDEX idx_sub_county_name_county ON sub_county (LOWER(name), county_id);


