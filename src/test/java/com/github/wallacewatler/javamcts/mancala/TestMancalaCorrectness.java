package com.github.wallacewatler.javamcts.mancala;

import com.github.wallacewatler.javamcts.*;

import java.util.Random;

final class TestMancalaCorrectness {
    public static void main(String[] args) {
        test(new MCTSRP(), false, false);
        test(new MCTSRP(), false, true);
        test(new MCTSRP(), true, false);
        test(new MCTSRP(), true, true);

        test(new MCTSTP(), false, false);
        test(new MCTSTP(), false, true);
        test(new MCTSTP(), true, false);
        test(new MCTSTP(), true, true);
    }

    private static void test(MCTS mcts, boolean favorUnexplored, boolean useTable) {
        final MancalaState rootState = new MancalaState();

        final UCT uct = new UCT(Math.sqrt(2), favorUnexplored);
        final SearchParameters params = new SearchParameters(0, Long.MAX_VALUE, 1000, uct, 1);

        while(!rootState.validActions().isEmpty()) {
            System.out.println("\n" + rootState.displayString());
            final SearchResults<ChooseHole> results = mcts.search(2, rootState, params, new Random(), useTable);
            results.bestAction().applyToState(rootState);
        }
        System.out.println("\n" + rootState.displayString());
    }
}
