package com.github.wallacewatler.javamcts.mancala;

import com.github.wallacewatler.javamcts.*;

import java.util.Random;

final class TestMancalaCorrectness {
    public static void main(String[] args) {
        test(new MCTSRP());
        test(new MCTSRPT());
        test(new MCTSTP());
    }

    private static void test(MCTS mcts) {
        final MancalaState rootState = new MancalaState();

        final UCT uct = new UCT(Math.sqrt(2), true);
        final SearchParameters params = new SearchParameters(1000, 3000, 1000000, uct, 1);

        while(!rootState.validActions().isEmpty()) {
            System.out.println("\n" + rootState.displayString());
            final SearchResults<ChooseHole> results = mcts.search(2, rootState, params, new Random());
            results.bestAction().applyToState(rootState);
        }
        System.out.println("\n" + rootState.displayString());
    }
}
