-- V1은 절대 수정하지 않음
ALTER TABLE member
    ADD COLUMN location            VARCHAR(16) NOT NULL DEFAULT 'LIVING_ROOM',
    ADD COLUMN location_updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE member ADD CONSTRAINT member_location_check
    CHECK (location IN ('KITCHEN', 'LIVING_ROOM', 'BED', 'OUTSIDE'));
