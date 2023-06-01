DROP TABLE IF EXISTS stats;

CREATE TABLE IF NOT EXISTS stats(
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY NOT NULL,
    app       BIGINT NOT NULL,
    uri       VARCHAR(100) NOT NULL,
    ip        VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP WITHOUT TIME ZONE
);