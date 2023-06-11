DROP TABLE IF EXISTS users, categories, events, compilations, compilation_events, requests;


CREATE TABLE IF NOT EXISTS users
(
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    name VARCHAR (250),
    email VARCHAR(64) UNIQUE NOT NULL,
    CONSTRAINT pk_user PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS categories
(
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    name VARCHAR(50) UNIQUE,
    CONSTRAINT pk_category PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS events
(
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    title VARCHAR(120),
    annotation VARCHAR(2000),
    category_id BIGINT NOT NULL,
    confirmed_requests BIGINT NOT NULL,
    description VARCHAR(7000),
    event_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    initiator_id BIGINT NOT NULL,
    lat FLOAT NOT NULL,
    lon FLOAT NOT NULL,
    paid BOOLEAN NOT NULL,
    participant_limit INTEGER DEFAULT 0,
    request_moderation BOOLEAN NOT NULL,
    state VARCHAR(100) NOT NULL,
    created_on TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    published_on TIMESTAMP WITHOUT TIME ZONE,
    views BIGINT,
    CONSTRAINT pk_event PRIMARY KEY (id),
    CONSTRAINT fk_initiator_id FOREIGN KEY (initiator_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_category_id FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS requests
(
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    requester_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    created_on TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status VARCHAR(120) NOT NULL,
    CONSTRAINT pk_requests PRIMARY KEY (id),
    CONSTRAINT fk_requester_id FOREIGN KEY (requester_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_requests_event_id FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS compilations
(
    id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    pinned BOOLEAN NOT NULL,
    title VARCHAR(120),
    event_id BIGINT,
    CONSTRAINT pk_compilation PRIMARY KEY (id),
    CONSTRAINT fk_compilations_event_id  FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS compilation_events
(
    compilation_id BIGINT,
    event_id BIGINT,
   CONSTRAINT fk_event_id FOREIGN KEY (event_id) REFERENCES events (id),
   CONSTRAINT fk_compilation_id FOREIGN KEY (compilation_id) REFERENCES compilations (id),
   PRIMARY KEY (event_id, compilation_id)
);


