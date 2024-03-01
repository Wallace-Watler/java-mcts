package wallacewatler.javamcts.mancala;

import java.util.Arrays;
import java.util.Random;

public record ChooseHole(int hole) implements wallacewatler.javamcts.mcts.Action<MancalaState>, wallacewatler.javamcts.olmcts.Action<MancalaState> {
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

        // Pass turn to the opponent.
        state.playerAboutToMove = state.playerAboutToMove == 0 ? 1 : 0;

        // Various effects depending on which hole was ended on.
        switch(currentHole) {
            case  0 -> { if(hole < 6 && state.holes[ 0] == 1) { state.holes[ 6] += 1 + state.holes[12]; state.holes[ 0] = 0; state.holes[12] = 0; } }
            case  1 -> { if(hole < 6 && state.holes[ 1] == 1) { state.holes[ 6] += 1 + state.holes[11]; state.holes[ 1] = 0; state.holes[11] = 0; } }
            case  2 -> { if(hole < 6 && state.holes[ 2] == 1) { state.holes[ 6] += 1 + state.holes[10]; state.holes[ 2] = 0; state.holes[10] = 0; } }
            case  3 -> { if(hole < 6 && state.holes[ 3] == 1) { state.holes[ 6] += 1 + state.holes[ 9]; state.holes[ 3] = 0; state.holes[ 9] = 0; } }
            case  4 -> { if(hole < 6 && state.holes[ 4] == 1) { state.holes[ 6] += 1 + state.holes[ 8]; state.holes[ 4] = 0; state.holes[ 8] = 0; } }
            case  5 -> { if(hole < 6 && state.holes[ 5] == 1) { state.holes[ 6] += 1 + state.holes[ 7]; state.holes[ 5] = 0; state.holes[ 7] = 0; } }
            case  6 -> state.playerAboutToMove = 0;
            case  7 -> { if(hole > 6 && state.holes[ 7] == 1) { state.holes[13] += 1 + state.holes[ 5]; state.holes[ 7] = 0; state.holes[ 5] = 0; } }
            case  8 -> { if(hole > 6 && state.holes[ 8] == 1) { state.holes[13] += 1 + state.holes[ 4]; state.holes[ 8] = 0; state.holes[ 4] = 0; } }
            case  9 -> { if(hole > 6 && state.holes[ 9] == 1) { state.holes[13] += 1 + state.holes[ 3]; state.holes[ 9] = 0; state.holes[ 3] = 0; } }
            case 10 -> { if(hole > 6 && state.holes[10] == 1) { state.holes[13] += 1 + state.holes[ 2]; state.holes[10] = 0; state.holes[ 2] = 0; } }
            case 11 -> { if(hole > 6 && state.holes[11] == 1) { state.holes[13] += 1 + state.holes[ 1]; state.holes[11] = 0; state.holes[ 1] = 0; } }
            case 12 -> { if(hole > 6 && state.holes[12] == 1) { state.holes[13] += 1 + state.holes[ 0]; state.holes[12] = 0; state.holes[ 0] = 0; } }
            case 13 -> state.playerAboutToMove = 1;
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

    @Override
    public MancalaState applyToState(MancalaState state, Random rand) {
        return applyToState(state);
    }
}
