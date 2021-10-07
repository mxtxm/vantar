BEGIN;

DROP TABLE IF EXISTS agency;
CREATE TABLE agency (
    id                     SERIAL    PRIMARY KEY,
    name                   TEXT      NOT NULL UNIQUE,
    url                    TEXT      NOT NULL UNIQUE,
    enabled                BOOLEAN   NOT NULL DEFAULT TRUE,
    priority               INTEGER   NOT NULL DEFAULT 1,
    server                 INTEGER   NOT NULL DEFAULT 1,
    crawl_depth            INTEGER   NOT NULL DEFAULT 1,
    parse_method           CHARACTER NOT NULL DEFAULT 'S',

    selector_code          TEXT,
    selector_subject       TEXT,
    selector_headline1     TEXT,
    selector_headline2     TEXT,
    selector_abstract      TEXT,
    selector_body          TEXT,
    selector_create_time   TEXT,
    selector_modify_time   TEXT,
    selector_reporter_code TEXT,

    url_black_list         TEXT,

    create_t               TIMESTAMP NOT NULL DEFAULT NOW(),
    modify_t               TIMESTAMP NOT NULL DEFAULT NOW()
);

DROP TABLE IF EXISTS crawl;
CREATE TABLE crawl (
    id                     SERIAL    PRIMARY KEY,
    agency_id              INTEGER   NOT NULL,
    create_t               TIMESTAMP NOT NULL DEFAULT NOW()
);




COMMIT;