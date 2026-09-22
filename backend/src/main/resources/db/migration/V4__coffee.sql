CREATE TABLE coffee (
    id         BIGSERIAL PRIMARY KEY,
    room_id    BIGINT NOT NULL REFERENCES room(id) ON DELETE CASCADE,
    message    VARCHAR(200),
    status     VARCHAR(16) NOT NULL DEFAULT 'FREE_SENT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX coffee_room_id_idx ON coffee (room_id);

ALTER TABLE coffee
    ADD CONSTRAINT coffee_status_check
    CHECK (status IN ('PENDING', 'FREE_SENT'));
