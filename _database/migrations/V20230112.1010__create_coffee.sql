create table coffee
(
    id     BIGSERIAL NOT NULL PRIMARY KEY,
    external_id UUID NOT NULL UNIQUE,
    brand           VARCHAR(20),
    origin          VARCHAR(20),
    characteristics VARCHAR(30),
    status          VARCHAR(20)
);
