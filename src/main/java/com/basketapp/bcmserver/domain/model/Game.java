package com.basketapp.bcmserver.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Pure Java 21 Game domain entity.
 * Free from framework and persistence annotations.
 */
public class Game {

    private final Long id;
    private Long teamId;
    private String opponent;
    private String round;
    private String date;
    private GameStatus status;
    private final Instant createdAt;

    // Scores (Home / Own)
    private int resultC1;
    private int resultC2;
    private int resultC3;
    private int resultC4;
    private int resultExtra;

    // Scores (Opponent)
    private int resultC1Opponent;
    private int resultC2Opponent;
    private int resultC3Opponent;
    private int resultC4Opponent;
    private int resultExtraOpponent;

    // Fouls (Home / Own)
    private int foulsC1;
    private int foulsC2;
    private int foulsC3;
    private int foulsC4;
    private int foulsExtra;

    // Fouls (Opponent)
    private int foulsC1Opponent;
    private int foulsC2Opponent;
    private int foulsC3Opponent;
    private int foulsC4Opponent;
    private int foulsExtraOpponent;

    public Game(
            Long id,
            Long teamId,
            String opponent,
            String round,
            String date,
            GameStatus status,
            Instant createdAt,
            int resultC1,
            int resultC2,
            int resultC3,
            int resultC4,
            int resultExtra,
            int resultC1Opponent,
            int resultC2Opponent,
            int resultC3Opponent,
            int resultC4Opponent,
            int resultExtraOpponent,
            int foulsC1,
            int foulsC2,
            int foulsC3,
            int foulsC4,
            int foulsExtra,
            int foulsC1Opponent,
            int foulsC2Opponent,
            int foulsC3Opponent,
            int foulsC4Opponent,
            int foulsExtraOpponent
    ) {
        this.id = id;
        this.teamId = validateTeamId(teamId);
        this.opponent = sanitizeAndValidateOpponent(opponent);
        this.round = sanitizeAndValidateRound(round);
        this.date = sanitizeAndValidateDate(date);
        this.status = status != null ? status : GameStatus.PENDING;
        this.createdAt = createdAt != null ? createdAt : Instant.now();

        this.resultC1 = validateNonNegative(resultC1, "resultC1");
        this.resultC2 = validateNonNegative(resultC2, "resultC2");
        this.resultC3 = validateNonNegative(resultC3, "resultC3");
        this.resultC4 = validateNonNegative(resultC4, "resultC4");
        this.resultExtra = validateNonNegative(resultExtra, "resultExtra");

        this.resultC1Opponent = validateNonNegative(resultC1Opponent, "resultC1Opponent");
        this.resultC2Opponent = validateNonNegative(resultC2Opponent, "resultC2Opponent");
        this.resultC3Opponent = validateNonNegative(resultC3Opponent, "resultC3Opponent");
        this.resultC4Opponent = validateNonNegative(resultC4Opponent, "resultC4Opponent");
        this.resultExtraOpponent = validateNonNegative(resultExtraOpponent, "resultExtraOpponent");

        this.foulsC1 = validateNonNegative(foulsC1, "foulsC1");
        this.foulsC2 = validateNonNegative(foulsC2, "foulsC2");
        this.foulsC3 = validateNonNegative(foulsC3, "foulsC3");
        this.foulsC4 = validateNonNegative(foulsC4, "foulsC4");
        this.foulsExtra = validateNonNegative(foulsExtra, "foulsExtra");

        this.foulsC1Opponent = validateNonNegative(foulsC1Opponent, "foulsC1Opponent");
        this.foulsC2Opponent = validateNonNegative(foulsC2Opponent, "foulsC2Opponent");
        this.foulsC3Opponent = validateNonNegative(foulsC3Opponent, "foulsC3Opponent");
        this.foulsC4Opponent = validateNonNegative(foulsC4Opponent, "foulsC4Opponent");
        this.foulsExtraOpponent = validateNonNegative(foulsExtraOpponent, "foulsExtraOpponent");
    }

    public static Game create(
            Long teamId,
            String opponent,
            String round,
            String date,
            GameStatus status,
            Integer resultC1,
            Integer resultC2,
            Integer resultC3,
            Integer resultC4,
            Integer resultExtra,
            Integer resultC1Opponent,
            Integer resultC2Opponent,
            Integer resultC3Opponent,
            Integer resultC4Opponent,
            Integer resultExtraOpponent,
            Integer foulsC1,
            Integer foulsC2,
            Integer foulsC3,
            Integer foulsC4,
            Integer foulsExtra,
            Integer foulsC1Opponent,
            Integer foulsC2Opponent,
            Integer foulsC3Opponent,
            Integer foulsC4Opponent,
            Integer foulsExtraOpponent
    ) {
        return new Game(
                null,
                teamId,
                opponent,
                round != null && !round.isBlank() ? round : "1",
                date,
                status != null ? status : GameStatus.PENDING,
                Instant.now(),
                resultC1 != null ? resultC1 : 0,
                resultC2 != null ? resultC2 : 0,
                resultC3 != null ? resultC3 : 0,
                resultC4 != null ? resultC4 : 0,
                resultExtra != null ? resultExtra : 0,
                resultC1Opponent != null ? resultC1Opponent : 0,
                resultC2Opponent != null ? resultC2Opponent : 0,
                resultC3Opponent != null ? resultC3Opponent : 0,
                resultC4Opponent != null ? resultC4Opponent : 0,
                resultExtraOpponent != null ? resultExtraOpponent : 0,
                foulsC1 != null ? foulsC1 : 0,
                foulsC2 != null ? foulsC2 : 0,
                foulsC3 != null ? foulsC3 : 0,
                foulsC4 != null ? foulsC4 : 0,
                foulsExtra != null ? foulsExtra : 0,
                foulsC1Opponent != null ? foulsC1Opponent : 0,
                foulsC2Opponent != null ? foulsC2Opponent : 0,
                foulsC3Opponent != null ? foulsC3Opponent : 0,
                foulsC4Opponent != null ? foulsC4Opponent : 0,
                foulsExtraOpponent != null ? foulsExtraOpponent : 0
        );
    }

    public static Game reconstitute(
            Long id,
            Long teamId,
            String opponent,
            String round,
            String date,
            GameStatus status,
            Instant createdAt,
            int resultC1,
            int resultC2,
            int resultC3,
            int resultC4,
            int resultExtra,
            int resultC1Opponent,
            int resultC2Opponent,
            int resultC3Opponent,
            int resultC4Opponent,
            int resultExtraOpponent,
            int foulsC1,
            int foulsC2,
            int foulsC3,
            int foulsC4,
            int foulsExtra,
            int foulsC1Opponent,
            int foulsC2Opponent,
            int foulsC3Opponent,
            int foulsC4Opponent,
            int foulsExtraOpponent
    ) {
        Objects.requireNonNull(id, "ID cannot be null when reconstituting existing game");
        if (id <= 0) {
            throw new IllegalArgumentException("ID must be a positive number when reconstituting existing game");
        }
        return new Game(
                id,
                teamId,
                opponent,
                round,
                date,
                status,
                createdAt,
                resultC1,
                resultC2,
                resultC3,
                resultC4,
                resultExtra,
                resultC1Opponent,
                resultC2Opponent,
                resultC3Opponent,
                resultC4Opponent,
                resultExtraOpponent,
                foulsC1,
                foulsC2,
                foulsC3,
                foulsC4,
                foulsExtra,
                foulsC1Opponent,
                foulsC2Opponent,
                foulsC3Opponent,
                foulsC4Opponent,
                foulsExtraOpponent
        );
    }

    public void update(
            Long teamId,
            String opponent,
            String round,
            String date,
            GameStatus status,
            int resultC1,
            int resultC2,
            int resultC3,
            int resultC4,
            int resultExtra,
            int resultC1Opponent,
            int resultC2Opponent,
            int resultC3Opponent,
            int resultC4Opponent,
            int resultExtraOpponent,
            int foulsC1,
            int foulsC2,
            int foulsC3,
            int foulsC4,
            int foulsExtra,
            int foulsC1Opponent,
            int foulsC2Opponent,
            int foulsC3Opponent,
            int foulsC4Opponent,
            int foulsExtraOpponent
    ) {
        this.teamId = validateTeamId(teamId);
        this.opponent = sanitizeAndValidateOpponent(opponent);
        this.round = sanitizeAndValidateRound(round);
        this.date = sanitizeAndValidateDate(date);
        this.status = Objects.requireNonNull(status, "status cannot be null");

        this.resultC1 = validateNonNegative(resultC1, "resultC1");
        this.resultC2 = validateNonNegative(resultC2, "resultC2");
        this.resultC3 = validateNonNegative(resultC3, "resultC3");
        this.resultC4 = validateNonNegative(resultC4, "resultC4");
        this.resultExtra = validateNonNegative(resultExtra, "resultExtra");

        this.resultC1Opponent = validateNonNegative(resultC1Opponent, "resultC1Opponent");
        this.resultC2Opponent = validateNonNegative(resultC2Opponent, "resultC2Opponent");
        this.resultC3Opponent = validateNonNegative(resultC3Opponent, "resultC3Opponent");
        this.resultC4Opponent = validateNonNegative(resultC4Opponent, "resultC4Opponent");
        this.resultExtraOpponent = validateNonNegative(resultExtraOpponent, "resultExtraOpponent");

        this.foulsC1 = validateNonNegative(foulsC1, "foulsC1");
        this.foulsC2 = validateNonNegative(foulsC2, "foulsC2");
        this.foulsC3 = validateNonNegative(foulsC3, "foulsC3");
        this.foulsC4 = validateNonNegative(foulsC4, "foulsC4");
        this.foulsExtra = validateNonNegative(foulsExtra, "foulsExtra");

        this.foulsC1Opponent = validateNonNegative(foulsC1Opponent, "foulsC1Opponent");
        this.foulsC2Opponent = validateNonNegative(foulsC2Opponent, "foulsC2Opponent");
        this.foulsC3Opponent = validateNonNegative(foulsC3Opponent, "foulsC3Opponent");
        this.foulsC4Opponent = validateNonNegative(foulsC4Opponent, "foulsC4Opponent");
        this.foulsExtraOpponent = validateNonNegative(foulsExtraOpponent, "foulsExtraOpponent");
    }

    private static Long validateTeamId(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new IllegalArgumentException("Team ID must be a positive non-null number");
        }
        return teamId;
    }

    private static String sanitizeAndValidateOpponent(String opponent) {
        if (opponent == null || opponent.isBlank()) {
            throw new IllegalArgumentException("Opponent name cannot be null, empty, or blank");
        }
        String trimmed = opponent.trim();
        if (trimmed.length() > 255) {
            throw new IllegalArgumentException("Opponent name cannot exceed 255 characters");
        }
        return trimmed;
    }

    private static String sanitizeAndValidateRound(String round) {
        if (round == null || round.isBlank()) {
            throw new IllegalArgumentException("Round cannot be null, empty, or blank");
        }
        return round.trim();
    }

    private static String sanitizeAndValidateDate(String date) {
        if (date == null || date.isBlank()) {
            throw new IllegalArgumentException("Date cannot be null, empty, or blank");
        }
        return date.trim();
    }

    private static int validateNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be a non-negative integer (>= 0)");
        }
        return value;
    }

    public Long getId() {
        return id;
    }

    public Long getTeamId() {
        return teamId;
    }

    public String getOpponent() {
        return opponent;
    }

    public String getRound() {
        return round;
    }

    public String getDate() {
        return date;
    }

    public GameStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public int getResultC1() {
        return resultC1;
    }

    public int getResultC2() {
        return resultC2;
    }

    public int getResultC3() {
        return resultC3;
    }

    public int getResultC4() {
        return resultC4;
    }

    public int getResultExtra() {
        return resultExtra;
    }

    public int getResultC1Opponent() {
        return resultC1Opponent;
    }

    public int getResultC2Opponent() {
        return resultC2Opponent;
    }

    public int getResultC3Opponent() {
        return resultC3Opponent;
    }

    public int getResultC4Opponent() {
        return resultC4Opponent;
    }

    public int getResultExtraOpponent() {
        return resultExtraOpponent;
    }

    public int getFoulsC1() {
        return foulsC1;
    }

    public int getFoulsC2() {
        return foulsC2;
    }

    public int getFoulsC3() {
        return foulsC3;
    }

    public int getFoulsC4() {
        return foulsC4;
    }

    public int getFoulsExtra() {
        return foulsExtra;
    }

    public int getFoulsC1Opponent() {
        return foulsC1Opponent;
    }

    public int getFoulsC2Opponent() {
        return foulsC2Opponent;
    }

    public int getFoulsC3Opponent() {
        return foulsC3Opponent;
    }

    public int getFoulsC4Opponent() {
        return foulsC4Opponent;
    }

    public int getFoulsExtraOpponent() {
        return foulsExtraOpponent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Game game = (Game) o;
        return Objects.equals(id, game.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Game{" +
                "id=" + id +
                ", teamId=" + teamId +
                ", opponent='" + opponent + '\'' +
                ", round='" + round + '\'' +
                ", date='" + date + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", resultC1=" + resultC1 +
                ", resultC2=" + resultC2 +
                ", resultC3=" + resultC3 +
                ", resultC4=" + resultC4 +
                ", resultExtra=" + resultExtra +
                ", resultC1Opponent=" + resultC1Opponent +
                ", resultC2Opponent=" + resultC2Opponent +
                ", resultC3Opponent=" + resultC3Opponent +
                ", resultC4Opponent=" + resultC4Opponent +
                ", resultExtraOpponent=" + resultExtraOpponent +
                ", foulsC1=" + foulsC1 +
                ", foulsC2=" + foulsC2 +
                ", foulsC3=" + foulsC3 +
                ", foulsC4=" + foulsC4 +
                ", foulsExtra=" + foulsExtra +
                ", foulsC1Opponent=" + foulsC1Opponent +
                ", foulsC2Opponent=" + foulsC2Opponent +
                ", foulsC3Opponent=" + foulsC3Opponent +
                ", foulsC4Opponent=" + foulsC4Opponent +
                ", foulsExtraOpponent=" + foulsExtraOpponent +
                '}';
    }
}

