-- É2: catalogs admins maintain. Same shape on purpose; separate tables so later FKs keep integrity.
CREATE TABLE surgery_type (
    id     BIGSERIAL   PRIMARY KEY,
    name   VARCHAR(80) NOT NULL,
    active BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX ux_surgery_type_name_lower ON surgery_type (lower(name));

CREATE TABLE scar_type (
    id     BIGSERIAL   PRIMARY KEY,
    name   VARCHAR(80) NOT NULL,
    active BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX ux_scar_type_name_lower ON scar_type (lower(name));

CREATE TABLE body_segment (
    id     BIGSERIAL   PRIMARY KEY,
    name   VARCHAR(80) NOT NULL,
    active BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX ux_body_segment_name_lower ON body_segment (lower(name));
