package com.basketapp.bcmserver.domain.model;

import java.util.Objects;

/**
 * Pure Java 21 PlayerStats domain entity.
 * Free from framework and persistence annotations.
 */
public class PlayerStats {

    private final Long id;
    private Long gameId;
    private Long playerId;
    private int minutes;
    private int seconds;
    private int t1a;
    private int t1i;
    private int t2a;
    private int t2i;
    private int t3a;
    private int t3i;
    private int oreb;
    private int dreb;
    private int asis;
    private int rec;
    private int per;
    private int falt;
    private int valuation;

    public PlayerStats(
            Long id,
            Long gameId,
            Long playerId,
            int minutes,
            int seconds,
            int t1a,
            int t1i,
            int t2a,
            int t2i,
            int t3a,
            int t3i,
            int oreb,
            int dreb,
            int asis,
            int rec,
            int per,
            int falt,
            int valuation
    ) {
        this.id = id;
        this.gameId = validatePositiveId(gameId, "Game ID");
        this.playerId = validatePositiveId(playerId, "Player ID");
        this.minutes = validateNonNegative(minutes, "minutes");
        this.seconds = validateSeconds(seconds);
        this.t1a = validateNonNegative(t1a, "t1a");
        this.t1i = validateShots(t1i, t1a, "t1i", "t1a");
        this.t2a = validateNonNegative(t2a, "t2a");
        this.t2i = validateShots(t2i, t2a, "t2i", "t2a");
        this.t3a = validateNonNegative(t3a, "t3a");
        this.t3i = validateShots(t3i, t3a, "t3i", "t3a");
        this.oreb = validateNonNegative(oreb, "oreb");
        this.dreb = validateNonNegative(dreb, "dreb");
        this.asis = validateNonNegative(asis, "asis");
        this.rec = validateNonNegative(rec, "rec");
        this.per = validateNonNegative(per, "per");
        this.falt = validateNonNegative(falt, "falt");
        this.valuation = valuation;
    }

    public static PlayerStats create(
            Long gameId,
            Long playerId,
            Integer minutes,
            Integer seconds,
            Integer t1a,
            Integer t1i,
            Integer t2a,
            Integer t2i,
            Integer t3a,
            Integer t3i,
            Integer oreb,
            Integer dreb,
            Integer asis,
            Integer rec,
            Integer per,
            Integer falt,
            Integer valuation
    ) {
        return new PlayerStats(
                null,
                gameId,
                playerId,
                minutes != null ? minutes : 0,
                seconds != null ? seconds : 0,
                t1a != null ? t1a : 0,
                t1i != null ? t1i : 0,
                t2a != null ? t2a : 0,
                t2i != null ? t2i : 0,
                t3a != null ? t3a : 0,
                t3i != null ? t3i : 0,
                oreb != null ? oreb : 0,
                dreb != null ? dreb : 0,
                asis != null ? asis : 0,
                rec != null ? rec : 0,
                per != null ? per : 0,
                falt != null ? falt : 0,
                valuation != null ? valuation : 0
        );
    }

    public static PlayerStats reconstitute(
            Long id,
            Long gameId,
            Long playerId,
            int minutes,
            int seconds,
            int t1a,
            int t1i,
            int t2a,
            int t2i,
            int t3a,
            int t3i,
            int oreb,
            int dreb,
            int asis,
            int rec,
            int per,
            int falt,
            int valuation
    ) {
        Objects.requireNonNull(id, "ID cannot be null when reconstituting existing player stats");
        if (id <= 0) {
            throw new IllegalArgumentException("ID must be a positive number when reconstituting existing player stats");
        }
        return new PlayerStats(
                id,
                gameId,
                playerId,
                minutes,
                seconds,
                t1a,
                t1i,
                t2a,
                t2i,
                t3a,
                t3i,
                oreb,
                dreb,
                asis,
                rec,
                per,
                falt,
                valuation
        );
    }

    public void update(
            Long gameId,
            Long playerId,
            int minutes,
            int seconds,
            int t1a,
            int t1i,
            int t2a,
            int t2i,
            int t3a,
            int t3i,
            int oreb,
            int dreb,
            int asis,
            int rec,
            int per,
            int falt,
            int valuation
    ) {
        this.gameId = validatePositiveId(gameId, "Game ID");
        this.playerId = validatePositiveId(playerId, "Player ID");
        this.minutes = validateNonNegative(minutes, "minutes");
        this.seconds = validateSeconds(seconds);
        this.t1a = validateNonNegative(t1a, "t1a");
        this.t1i = validateShots(t1i, t1a, "t1i", "t1a");
        this.t2a = validateNonNegative(t2a, "t2a");
        this.t2i = validateShots(t2i, t2a, "t2i", "t2a");
        this.t3a = validateNonNegative(t3a, "t3a");
        this.t3i = validateShots(t3i, t3a, "t3i", "t3a");
        this.oreb = validateNonNegative(oreb, "oreb");
        this.dreb = validateNonNegative(dreb, "dreb");
        this.asis = validateNonNegative(asis, "asis");
        this.rec = validateNonNegative(rec, "rec");
        this.per = validateNonNegative(per, "per");
        this.falt = validateNonNegative(falt, "falt");
        this.valuation = valuation;
    }

    private static Long validatePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be a positive non-null number");
        }
        return id;
    }

    private static int validateNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be a non-negative integer (>= 0)");
        }
        return value;
    }

    private static int validateSeconds(int seconds) {
        if (seconds < 0 || seconds > 59) {
            throw new IllegalArgumentException("seconds must be between 0 and 59");
        }
        return seconds;
    }

    private static int validateShots(int made, int attempted, String madeField, String attemptedField) {
        validateNonNegative(made, madeField);
        validateNonNegative(attempted, attemptedField);
        if (made > attempted) {
            throw new IllegalArgumentException(madeField + " cannot exceed " + attemptedField);
        }
        return made;
    }

    public Long getId() {
        return id;
    }

    public Long getGameId() {
        return gameId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public int getMinutes() {
        return minutes;
    }

    public int getSeconds() {
        return seconds;
    }

    public int getT1a() {
        return t1a;
    }

    public int getT1i() {
        return t1i;
    }

    public int getT2a() {
        return t2a;
    }

    public int getT2i() {
        return t2i;
    }

    public int getT3a() {
        return t3a;
    }

    public int getT3i() {
        return t3i;
    }

    public int getOreb() {
        return oreb;
    }

    public int getDreb() {
        return dreb;
    }

    public int getAsis() {
        return asis;
    }

    public int getRec() {
        return rec;
    }

    public int getPer() {
        return per;
    }

    public int getFalt() {
        return falt;
    }

    public int getValuation() {
        return valuation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerStats that = (PlayerStats) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PlayerStats{" +
                "id=" + id +
                ", gameId=" + gameId +
                ", playerId=" + playerId +
                ", minutes=" + minutes +
                ", seconds=" + seconds +
                ", t1a=" + t1a +
                ", t1i=" + t1i +
                ", t2a=" + t2a +
                ", t2i=" + t2i +
                ", t3a=" + t3a +
                ", t3i=" + t3i +
                ", oreb=" + oreb +
                ", dreb=" + dreb +
                ", asis=" + asis +
                ", rec=" + rec +
                ", per=" + per +
                ", falt=" + falt +
                ", valuation=" + valuation +
                '}';
    }
}

