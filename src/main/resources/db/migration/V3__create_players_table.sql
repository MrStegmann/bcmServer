-- Flyway migration: V3__create_players_table.sql
-- Create players table with team foreign key, cascading deletion, and index

CREATE TABLE players (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    number TEXT NOT NULL,
    jersey_name TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_players_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE
);

CREATE INDEX idx_players_team_id ON players (team_id);

