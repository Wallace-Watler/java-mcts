package com.github.wallacewatler.javamcts.mancala;

import com.github.wallacewatler.javamcts.VisibleState;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public final class MancalaState implements VisibleState<MancalaState, ChooseHole> {
    /** The player about to act. */
    public int activePlayer;

    /**
     * The 14 holes of a mancala board. Holes are numbered counter-clockwise, starting with player 0's first hole. Hole
     * 6 is player 0's store, and hole 13 is player 1's store.
     */
    public final int[] holes;

    /** Set up the board. */
    public MancalaState() {
        activePlayer = 0;
        holes = new int[] { 4, 4, 4, 4, 4, 4, 0, 4, 4, 4, 4, 4, 4, 0 };
    }

    /** Copy a state. */
    public MancalaState(MancalaState state) {
        activePlayer = state.activePlayer;
        holes = Arrays.copyOf(state.holes, 14);
    }

    public String displayString() {
        return String.format(
                "   |%2d|%2d|%2d|%2d|%2d|%2d|   \n%2d +--+--+--+--+--+--+ %2d\n   |%2d|%2d|%2d|%2d|%2d|%2d|   ",
                holes[12], holes[11], holes[10], holes[9], holes[8], holes[7],
                holes[13], holes[6],
                holes[0], holes[1], holes[2], holes[3], holes[4], holes[5]
        );
    }

    @Override
    public int activePlayer() {
        return activePlayer;
    }

    @Override
    public List<ChooseHole> validActions() {
        return switch(activePlayer) {
            case 0 -> IntStream.range(0, 6).filter(i -> holes[i] > 0).mapToObj(ChooseHole::new).toList();
            case 1 -> IntStream.range(7, 13).filter(i -> holes[i] > 0).mapToObj(ChooseHole::new).toList();
            default -> throw new IllegalStateException("unexpected value for player: " + activePlayer);
        };
    }

    @Override
    public double[] scores() {
        final int numOnBoard = Arrays.stream(holes, 0, 6).sum() + Arrays.stream(holes, 7, 13).sum();

        if(holes[6] > holes[13] + numOnBoard)
            return new double[] { 1.0, 0.0 };

        if(holes[13] > holes[6] + numOnBoard)
            return new double[] { 0.0, 1.0 };

        if(holes[6] == holes[13] && numOnBoard == 0)
            return new double[] { 0.5, 0.5 };

        return null;
    }

    @Override
    public MancalaState copy() {
        return new MancalaState(this);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        MancalaState that = (MancalaState) o;
        return activePlayer == that.activePlayer && Arrays.equals(holes, that.holes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(activePlayer);
        result = 31 * result + Arrays.hashCode(holes);
        return result;
    }
}
