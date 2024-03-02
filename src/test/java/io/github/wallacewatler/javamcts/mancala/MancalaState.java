package io.github.wallacewatler.javamcts.mancala;

import io.github.wallacewatler.javamcts.InfoSet;
import io.github.wallacewatler.javamcts.VisibleState;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public final class MancalaState implements VisibleState<MancalaState, ChooseHole>, InfoSet<MancalaState, ChooseHole> {
    public int activePlayer;

    // The 14 holes of a mancala board. Holes are numbered counter-clockwise, starting with player 0's first hole.
    public final int[] holes;

    public MancalaState() {
        activePlayer = 0;
        holes = new int[] { 4, 4, 4, 4, 4, 4, 0, 4, 4, 4, 4, 4, 4, 0 };
    }

    public MancalaState(MancalaState state) {
        activePlayer = state.activePlayer;
        holes = Arrays.copyOf(state.holes, 14);
    }

    public String boardAsString() {
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
    public int owner() {
        return activePlayer;
    }

    @Override
    public MancalaState determinize(Random rand) {
        return copy();
    }
}
