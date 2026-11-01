-- Flyway migration: V4__create_games_table.sql
-- Create games table with team foreign key, quarter score columns, fouls, and cascading deletion

CREATE TABLE games (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    opponent TEXT NOT NULL,
    round TEXT NOT NULL DEFAULT '1',
    date TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    result_c1 INTEGER NOT NULL DEFAULT 0,
    result_c2 INTEGER NOT NULL DEFAULT 0,
    result_c3 INTEGER NOT NULL DEFAULT 0,
    result_c4 INTEGER NOT NULL DEFAULT 0,
    result_extra INTEGER NOT NULL DEFAULT 0,
    result_c1_opponent INTEGER NOT NULL DEFAULT 0,
    result_c2_opponent INTEGER NOT NULL DEFAULT 0,
    result_c3_opponent INTEGER NOT NULL DEFAULT 0,
    result_c4_opponent INTEGER NOT NULL DEFAULT 0,
    result_extra_opponent INTEGER NOT NULL DEFAULT 0,

    fouls_c1 INTEGER NOT NULL DEFAULT 0,
    fouls_c2 INTEGER NOT NULL DEFAULT 0,
    fouls_c3 INTEGER NOT NULL DEFAULT 0,
    fouls_c4 INTEGER NOT NULL DEFAULT 0,
    fouls_extra INTEGER NOT NULL DEFAULT 0,
    fouls_c1_opponent INTEGER NOT NULL DEFAULT 0,
    fouls_c2_opponent INTEGER NOT NULL DEFAULT 0,
    fouls_c3_opponent INTEGER NOT NULL DEFAULT 0,
    fouls_c4_opponent INTEGER NOT NULL DEFAULT 0,
    fouls_extra_opponent INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT fk_games_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE
);

CREATE INDEX idx_games_team_id ON games (team_id);

