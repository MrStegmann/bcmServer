package com.basketapp.bcmserver.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity representing the 'games' table in PostgreSQL.
 * Isolated within the persistence adapter ring.
 */
@Entity
@Table(name = "games")
public class GameJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "opponent", nullable = false, columnDefinition = "TEXT")
    private String opponent;

    @Column(name = "round", nullable = false, columnDefinition = "TEXT")
    private String round;

    @Column(name = "date", nullable = false, columnDefinition = "TEXT")
    private String date;

    @Column(name = "status", nullable = false, columnDefinition = "TEXT")
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Scores
    @Column(name = "result_c1", nullable = false)
    private int resultC1;

    @Column(name = "result_c2", nullable = false)
    private int resultC2;

    @Column(name = "result_c3", nullable = false)
    private int resultC3;

    @Column(name = "result_c4", nullable = false)
    private int resultC4;

    @Column(name = "result_extra", nullable = false)
    private int resultExtra;

    @Column(name = "result_c1_opponent", nullable = false)
    private int resultC1Opponent;

    @Column(name = "result_c2_opponent", nullable = false)
    private int resultC2Opponent;

    @Column(name = "result_c3_opponent", nullable = false)
    private int resultC3Opponent;

    @Column(name = "result_c4_opponent", nullable = false)
    private int resultC4Opponent;

    @Column(name = "result_extra_opponent", nullable = false)
    private int resultExtraOpponent;

    // Fouls
    @Column(name = "fouls_c1", nullable = false)
    private int foulsC1;

    @Column(name = "fouls_c2", nullable = false)
    private int foulsC2;

    @Column(name = "fouls_c3", nullable = false)
    private int foulsC3;

    @Column(name = "fouls_c4", nullable = false)
    private int foulsC4;

    @Column(name = "fouls_extra", nullable = false)
    private int foulsExtra;

    @Column(name = "fouls_c1_opponent", nullable = false)
    private int foulsC1Opponent;

    @Column(name = "fouls_c2_opponent", nullable = false)
    private int foulsC2Opponent;

    @Column(name = "fouls_c3_opponent", nullable = false)
    private int foulsC3Opponent;

    @Column(name = "fouls_c4_opponent", nullable = false)
    private int foulsC4Opponent;

    @Column(name = "fouls_extra_opponent", nullable = false)
    private int foulsExtraOpponent;

    public GameJpaEntity() {
    }

    public GameJpaEntity(
            Long id,
            Long teamId,
            String opponent,
            String round,
            String date,
            String status,
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
        this.teamId = teamId;
        this.opponent = opponent;
        this.round = round;
        this.date = date;
        this.status = status;
        this.createdAt = createdAt;
        this.resultC1 = resultC1;
        this.resultC2 = resultC2;
        this.resultC3 = resultC3;
        this.resultC4 = resultC4;
        this.resultExtra = resultExtra;
        this.resultC1Opponent = resultC1Opponent;
        this.resultC2Opponent = resultC2Opponent;
        this.resultC3Opponent = resultC3Opponent;
        this.resultC4Opponent = resultC4Opponent;
        this.resultExtraOpponent = resultExtraOpponent;
        this.foulsC1 = foulsC1;
        this.foulsC2 = foulsC2;
        this.foulsC3 = foulsC3;
        this.foulsC4 = foulsC4;
        this.foulsExtra = foulsExtra;
        this.foulsC1Opponent = foulsC1Opponent;
        this.foulsC2Opponent = foulsC2Opponent;
        this.foulsC3Opponent = foulsC3Opponent;
        this.foulsC4Opponent = foulsC4Opponent;
        this.foulsExtraOpponent = foulsExtraOpponent;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getOpponent() {
        return opponent;
    }

    public void setOpponent(String opponent) {
        this.opponent = opponent;
    }

    public String getRound() {
        return round;
    }

    public void setRound(String round) {
        this.round = round;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public int getResultC1() {
        return resultC1;
    }

    public void setResultC1(int resultC1) {
        this.resultC1 = resultC1;
    }

    public int getResultC2() {
        return resultC2;
    }

    public void setResultC2(int resultC2) {
        this.resultC2 = resultC2;
    }

    public int getResultC3() {
        return resultC3;
    }

    public void setResultC3(int resultC3) {
        this.resultC3 = resultC3;
    }

    public int getResultC4() {
        return resultC4;
    }

    public void setResultC4(int resultC4) {
        this.resultC4 = resultC4;
    }

    public int getResultExtra() {
        return resultExtra;
    }

    public void setResultExtra(int resultExtra) {
        this.resultExtra = resultExtra;
    }

    public int getResultC1Opponent() {
        return resultC1Opponent;
    }

    public void setResultC1Opponent(int resultC1Opponent) {
        this.resultC1Opponent = resultC1Opponent;
    }

    public int getResultC2Opponent() {
        return resultC2Opponent;
    }

    public void setResultC2Opponent(int resultC2Opponent) {
        this.resultC2Opponent = resultC2Opponent;
    }

    public int getResultC3Opponent() {
        return resultC3Opponent;
    }

    public void setResultC3Opponent(int resultC3Opponent) {
        this.resultC3Opponent = resultC3Opponent;
    }

    public int getResultC4Opponent() {
        return resultC4Opponent;
    }

    public void setResultC4Opponent(int resultC4Opponent) {
        this.resultC4Opponent = resultC4Opponent;
    }

    public int getResultExtraOpponent() {
        return resultExtraOpponent;
    }

    public void setResultExtraOpponent(int resultExtraOpponent) {
        this.resultExtraOpponent = resultExtraOpponent;
    }

    public int getFoulsC1() {
        return foulsC1;
    }

    public void setFoulsC1(int foulsC1) {
        this.foulsC1 = foulsC1;
    }

    public int getFoulsC2() {
        return foulsC2;
    }

    public void setFoulsC2(int foulsC2) {
        this.foulsC2 = foulsC2;
    }

    public int getFoulsC3() {
        return foulsC3;
    }

    public void setFoulsC3(int foulsC3) {
        this.foulsC3 = foulsC3;
    }

    public int getFoulsC4() {
        return foulsC4;
    }

    public void setFoulsC4(int foulsC4) {
        this.foulsC4 = foulsC4;
    }

    public int getFoulsExtra() {
        return foulsExtra;
    }

    public void setFoulsExtra(int foulsExtra) {
        this.foulsExtra = foulsExtra;
    }

    public int getFoulsC1Opponent() {
        return foulsC1Opponent;
    }

    public void setFoulsC1Opponent(int foulsC1Opponent) {
        this.foulsC1Opponent = foulsC1Opponent;
    }

    public int getFoulsC2Opponent() {
        return foulsC2Opponent;
    }

    public void setFoulsC2Opponent(int foulsC2Opponent) {
        this.foulsC2Opponent = foulsC2Opponent;
    }

    public int getFoulsC3Opponent() {
        return foulsC3Opponent;
    }

    public void setFoulsC3Opponent(int foulsC3Opponent) {
        this.foulsC3Opponent = foulsC3Opponent;
    }

    public int getFoulsC4Opponent() {
        return foulsC4Opponent;
    }

    public void setFoulsC4Opponent(int foulsC4Opponent) {
        this.foulsC4Opponent = foulsC4Opponent;
    }

    public int getFoulsExtraOpponent() {
        return foulsExtraOpponent;
    }

    public void setFoulsExtraOpponent(int foulsExtraOpponent) {
        this.foulsExtraOpponent = foulsExtraOpponent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameJpaEntity that = (GameJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

