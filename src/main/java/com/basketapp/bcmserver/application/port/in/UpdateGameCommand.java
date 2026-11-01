package com.basketapp.bcmserver.application.port.in;

/**
 * Command carrying validated data required to update an existing Game.
 */
public record UpdateGameCommand(
        Long id,
        Long teamId,
        String opponent,
        String round,
        String date,
        String status,
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
) {}

