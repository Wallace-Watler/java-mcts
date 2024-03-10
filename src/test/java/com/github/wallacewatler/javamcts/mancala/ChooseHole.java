package com.github.wallacewatler.javamcts.mancala;

import com.github.wallacewatler.javamcts.MCTS;

import java.util.Arrays;

/**
 * The only type of action in a game of mancala.
 *
 * @param hole The index of the hole to pick up pieces from.
 */
public record ChooseHole(int hole) implements MCTS.Action<MancalaState> {
    @Override
    public MancalaState applyToState(MancalaState state) {
        // Pick up pieces in chosen hole.
        int remainingPieces = state.holes[hole];
        state.holes[hole] = 0;
        int currentHole = hole;

        // Deposit pieces one-by-one counter-clockwise around the board, skipping the opponent's store.
        while(remainingPieces > 0) {
            currentHole++;
            switch(currentHole) {
                case 6 -> { if(hole > 6) currentHole = 7; }
                case 13 -> { if(hole < 6) currentHole = 0; }
                case 14 -> currentHole = 0;
                default -> {}
            }
            state.holes[currentHole]++;
            remainingPieces--;
        }

        // Pass turn to the opponent (unless the following section overrides this).
        state.activePlayer = state.activePlayer == 0 ? 1 : 0;

        // Capture pieces depending on which hole was landed on, or take another turn if a store was landed on.
        switch(currentHole) {
            case 0, 1, 2, 3, 4, 5 -> {
                if(hole < 6 && state.holes[currentHole] == 1) {
                    state.holes[6] += 1 + state.holes[12 - currentHole];
                    state.holes[currentHole] = 0;
                    state.holes[12 - currentHole] = 0;
                }
            }
            case 6 -> state.activePlayer = 0;
            case 7, 8, 9, 10, 11, 12 -> {
                if(hole > 6 && state.holes[currentHole] == 1) {
                    state.holes[13] += 1 + state.holes[12 - currentHole];
                    state.holes[currentHole] = 0;
                    state.holes[12 - currentHole] = 0;
                }
            }
            case 13 -> state.activePlayer = 1;
        }

        // Check if the game has ended; if so, capture all remaining pieces appropriately.
        final int count0 = Arrays.stream(state.holes, 0, 6).sum();
        final int count1 = Arrays.stream(state.holes, 7, 13).sum();
        if(count0 == 0 && count1 != 0) {
            state.holes[13] += count1;
            Arrays.fill(state.holes, 7, 13, 0);
        } else if(count1 == 0 && count0 != 0) {
            state.holes[6] += count0;
            Arrays.fill(state.holes, 0, 6, 0);
        }

        return state;
    }
}
