DELETE FROM coffee;

ALTER TABLE coffee
    ADD COLUMN writer_name   VARCHAR(10) NOT NULL,
    ADD COLUMN password_hash VARCHAR(60) NOT NULL;
