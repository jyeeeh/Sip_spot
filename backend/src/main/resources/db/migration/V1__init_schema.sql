CREATE TABLE room (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(7)  NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    max_members INT         NOT NULL DEFAULT 8,

    CONSTRAINT room_code_unique UNIQUE (code),
    CONSTRAINT room_code_format CHECK (code ~ '^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{7}$')
);

CREATE TABLE member (
    id          UUID        PRIMARY KEY,
    room_id     BIGINT      NOT NULL REFERENCES room(id) ON DELETE CASCADE,
    nickname    VARCHAR(12) NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    is_host     BOOLEAN     NOT NULL DEFAULT false,
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX member_token_hash_uidx   ON member (token_hash);
CREATE UNIQUE INDEX member_room_nickname_uidx ON member (room_id, lower(nickname));
