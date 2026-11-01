package com.basketapp.bcmserver.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * JPA entity representing the 'player_stats' table in PostgreSQL.
 * Isolated within the persistence adapter ring.
 */
@Entity
@Table(name = "player_stats")
public class PlayerStatsJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "minutes", nullable = false)
    private int minutes;

    @Column(name = "seconds", nullable = false)
    private int seconds;

    @Column(name = "t1a", nullable = false)
    private int t1a;

    @Column(name = "t1i", nullable = false)
    private int t1i;

    @Column(name = "t2a", nullable = false)
    private int t2a;

    @Column(name = "t2i", nullable = false)
    private int t2i;

    @Column(name = "t3a", nullable = false)
    private int t3a;

    @Column(name = "t3i", nullable = false)
    private int t3i;

    @Column(name = "oreb", nullable = false)
    private int oreb;

    @Column(name = "dreb", nullable = false)
    private int dreb;

    @Column(name = "asis", nullable = false)
    private int asis;

    @Column(name = "rec", nullable = false)
    private int rec;

    @Column(name = "per", nullable = false)
    private int per;

    @Column(name = "falt", nullable = false)
    private int falt;

    @Column(name = "valuation", nullable = false)
    private int valuation;

    public PlayerStatsJpaEntity() {
    }

    public PlayerStatsJpaEntity(
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
        this.gameId = gameId;
        this.playerId = playerId;
        this.minutes = minutes;
        this.seconds = seconds;
        this.t1a = t1a;
        this.t1i = t1i;
        this.t2a = t2a;
        this.t2i = t2i;
        this.t3a = t3a;
        this.t3i = t3i;
        this.oreb = oreb;
        this.dreb = dreb;
        this.asis = asis;
        this.rec = rec;
        this.per = per;
        this.falt = falt;
        this.valuation = valuation;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public int getT1a() {
        return t1a;
    }

    public void setT1a(int t1a) {
        this.t1a = t1a;
    }

    public int getT1i() {
        return t1i;
    }

    public void setT1i(int t1i) {
        this.t1i = t1i;
    }

    public int getT2a() {
        return t2a;
    }

    public void setT2a(int t2a) {
        this.t2a = t2a;
    }

    public int getT2i() {
        return t2i;
    }

    public void setT2i(int t2i) {
        this.t2i = t2i;
    }

    public int getT3a() {
        return t3a;
    }

    public void setT3a(int t3a) {
        this.t3a = t3a;
    }

    public int getT3i() {
        return t3i;
    }

    public void setT3i(int t3i) {
        this.t3i = t3i;
    }

    public int getOreb() {
        return oreb;
    }

    public void setOreb(int oreb) {
        this.oreb = oreb;
    }

    public int getDreb() {
        return dreb;
    }

    public void setDreb(int dreb) {
        this.dreb = dreb;
    }

    public int getAsis() {
        return asis;
    }

    public void setAsis(int asis) {
        this.asis = asis;
    }

    public int getRec() {
        return rec;
    }

    public void setRec(int rec) {
        this.rec = rec;
    }

    public int getPer() {
        return per;
    }

    public void setPer(int per) {
        this.per = per;
    }

    public int getFalt() {
        return falt;
    }

    public void setFalt(int falt) {
        this.falt = falt;
    }

    public int getValuation() {
        return valuation;
    }

    public void setValuation(int valuation) {
        this.valuation = valuation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerStatsJpaEntity that = (PlayerStatsJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

