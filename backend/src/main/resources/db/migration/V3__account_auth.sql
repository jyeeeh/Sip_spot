-- ① 기존 room 데이터 삭제 (host_account_id NOT NULL 제약을 위해)
DELETE FROM room;

-- ② account 테이블
CREATE TABLE account (
    id            BIGSERIAL   PRIMARY KEY,
    username      VARCHAR(30) NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    nickname      VARCHAR(12) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- 대소문자 무시 유니크 인덱스
CREATE UNIQUE INDEX account_username_ci_uidx ON account (lower(username));
CREATE UNIQUE INDEX account_nickname_ci_uidx ON account (lower(nickname));

-- ③ account_session 테이블
CREATE TABLE account_session (
    id          BIGSERIAL PRIMARY KEY,
    account_id  BIGINT NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX account_session_token_hash_uidx ON account_session (token_hash);

-- ④ room 테이블 변경: max_members 제거, host·location 추가
ALTER TABLE room
    DROP COLUMN max_members,
    ADD COLUMN host_account_id     BIGINT      NOT NULL REFERENCES account(id),
    ADD COLUMN location            VARCHAR(16) NOT NULL DEFAULT 'LIVING_ROOM',
    ADD COLUMN location_updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE room
    ADD CONSTRAINT room_host_account_unique UNIQUE (host_account_id),
    ADD CONSTRAINT room_location_check
        CHECK (location IN ('KITCHEN', 'LIVING_ROOM', 'BED', 'OUTSIDE'));

-- ⑤ member 테이블 삭제
DROP TABLE member;
