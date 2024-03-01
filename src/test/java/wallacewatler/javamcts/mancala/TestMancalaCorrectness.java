package wallacewatler.javamcts.mancala;

import wallacewatler.javamcts.SearchParameters;
import wallacewatler.javamcts.SearchResults;
import wallacewatler.javamcts.UCT;
import wallacewatler.javamcts.mcts.MCTS;
import wallacewatler.javamcts.olmcts.OLMCTS;

public final class TestMancalaCorrectness {
    public static void main(String[] args) {
        testMCTS();
        testOLMCTS();
    }

    private static void testMCTS() {
        final MancalaState rootState = new MancalaState();
        final SearchParameters params = new SearchParameters(0, Long.MAX_VALUE, 100, new UCT(Math.sqrt(2), true), 1);
        while(!rootState.isTerminal()) {
            System.out.println("\n" + rootState.boardAsString());
            final SearchResults<ChooseHole> results = MCTS.search(2, rootState, params);
            results.optimalAction().applyToState(rootState);
        }
        System.out.println("\n" + rootState.boardAsString());
    }

    private static void testOLMCTS() {
        final MancalaState rootState = new MancalaState();
        final SearchParameters params = new SearchParameters(0, Long.MAX_VALUE, 100, new UCT(Math.sqrt(2), true), 1);
        while(!rootState.isTerminal()) {
            System.out.println("\n" + rootState.boardAsString());
            final SearchResults<ChooseHole> results = OLMCTS.search(2, rootState, params);
            results.optimalAction().applyToState(rootState);
        }
        System.out.println("\n" + rootState.boardAsString());
    }
}
