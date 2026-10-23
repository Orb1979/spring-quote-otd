CREATE TABLE viewer (
    id   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name text NOT NULL
);

CREATE TABLE quote (
    id   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    text text NOT NULL UNIQUE
);

CREATE TABLE quote_history (
    viewer_id uuid        NOT NULL REFERENCES viewer(id) ON DELETE CASCADE,
    quote_id  uuid        NOT NULL REFERENCES quote(id)  ON DELETE CASCADE,
    cycle     integer     NOT NULL DEFAULT 1,
    sequence  integer     NOT NULL,
    shown_at  timestamptz NOT NULL DEFAULT now(),

    PRIMARY KEY (viewer_id, quote_id, cycle),
    UNIQUE (viewer_id, sequence)
);

CREATE INDEX idx_quote_history_recent
    ON quote_history (viewer_id, sequence DESC);
