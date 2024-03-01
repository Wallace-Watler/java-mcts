package wallacewatler.javamcts.mancala;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public final class MancalaState implements wallacewatler.javamcts.mcts.State<MancalaState, ChooseHole>, wallacewatler.javamcts.olmcts.State<MancalaState, ChooseHole> {
    public int playerAboutToMove;

    /**
     * The 14 holes of a mancala board. Holes are numbered counter-clockwise, starting with player 0's first hole.
     */
    public final int[] holes;

    public MancalaState() {
        playerAboutToMove = 0;
        holes = new int[] { 4, 4, 4, 4, 4, 4, 0, 4, 4, 4, 4, 4, 4, 0 };
    }

    public MancalaState(MancalaState state) {
        playerAboutToMove = state.playerAboutToMove;
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
    public int playerAboutToMove() {
        return playerAboutToMove;
    }

    @Override
    public List<ChooseHole> availableActions() {
        return switch(playerAboutToMove) {
            case 0 -> IntStream.range(0, 6).filter(i -> holes[i] > 0).mapToObj(ChooseHole::new).toList();
            case 1 -> IntStream.range(7, 13).filter(i -> holes[i] > 0).mapToObj(ChooseHole::new).toList();
            default -> throw new IllegalStateException("unexpected value for player: " + playerAboutToMove);
        };
    }

    @Override
    public boolean isTerminal() {
        return holes[6] + holes[13] == 48;
    }

    @Override
    public double[] scores() {
        final int captured0 = holes[6];
        final int captured1 = holes[13];
        if(captured0 == captured1) {
            return new double[] { 0.5, 0.5 };
        } else if(captured0 > captured1) {
            return new double[] { 1.0, 0.0 };
        } else {
            return new double[] { 0.0, 1.0 };
        }
    }

    @Override
    public MancalaState copy() {
        return new MancalaState(this);
    }
}
