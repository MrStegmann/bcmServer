-- Flyway migration: V5__create_player_stats_table.sql
-- Create player_stats table with game and player foreign keys, time, and statistical counters

CREATE TABLE player_stats (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    minutes INTEGER NOT NULL DEFAULT 0,
    seconds INTEGER NOT NULL DEFAULT 0,
    t1a INTEGER NOT NULL DEFAULT 0,
    t1i INTEGER NOT NULL DEFAULT 0,
    t2a INTEGER NOT NULL DEFAULT 0,
    t2i INTEGER NOT NULL DEFAULT 0,
    t3a INTEGER NOT NULL DEFAULT 0,
    t3i INTEGER NOT NULL DEFAULT 0,
    oreb INTEGER NOT NULL DEFAULT 0,
    dreb INTEGER NOT NULL DEFAULT 0,
    asis INTEGER NOT NULL DEFAULT 0,
    rec INTEGER NOT NULL DEFAULT 0,
    per INTEGER NOT NULL DEFAULT 0,
    falt INTEGER NOT NULL DEFAULT 0,
    valuation INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_player_stats_game FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE CASCADE,
    CONSTRAINT fk_player_stats_player FOREIGN KEY (player_id) REFERENCES players (id) ON DELETE CASCADE
);

CREATE INDEX idx_player_stats_game_id ON player_stats (game_id);
CREATE INDEX idx_player_stats_player_id ON player_stats (player_id);
CREATE INDEX idx_player_stats_game_player ON player_stats (game_id, player_id);

