create table coffee
(
    id     BIGSERIAL NOT NULL PRIMARY KEY,
    brand  VARCHAR(20),
    origin VARCHAR(20),
    characteristics VARCHAR(30)
);
