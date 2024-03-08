package io.github.wallacewatler.javamcts.mancala;

import io.github.wallacewatler.javamcts.*;

import java.util.Random;

public final class TestMancalaCorrectness {
    public static void main(String[] args) {
        testMCTS(new MCTSRP());
        testMCTS(new MCTSTP());
        testOLMCTS(new OLMCTSRP());
        testOLMCTS(new OLMCTSTP());
        testMOISMCTS(new MOISMCTSRP());
        testMOISMCTS(new MOISMCTSTP());
    }

    private static void testMCTS(MCTS mcts) {
        final MancalaState rootState = new MancalaState();
        final SearchParameters params = new SearchParameters(0, Long.MAX_VALUE, 1000, new UCT(Math.sqrt(2), true), 1);
        while(!rootState.validActions().isEmpty()) {
            System.out.println("\n" + rootState.boardAsString());
            final SearchResults<ChooseHole> results = mcts.search(2, rootState, params, new Random());
            results.bestAction().applyToState(rootState);
        }
        System.out.println("\n" + rootState.boardAsString());
    }

    private static void testOLMCTS(OLMCTS olmcts) {
        final MancalaState rootState = new MancalaState();
        final SearchParameters params = new SearchParameters(0, Long.MAX_VALUE, 1000, new UCT(Math.sqrt(2), true), 1);
        while(!rootState.validActions().isEmpty()) {
            System.out.println("\n" + rootState.boardAsString());
            final SearchResults<ChooseHole> results = olmcts.search(2, rootState, params, new Random());
            results.bestAction().applyToState(rootState);
        }
        System.out.println("\n" + rootState.boardAsString());
    }

    private static void testMOISMCTS(MOISMCTS moismcts) {
        final MancalaState rootState = new MancalaState();
        final SearchParameters params = new SearchParameters(0, Long.MAX_VALUE, 1000, new UCT(Math.sqrt(2), true), 1);
        while(!rootState.validActions().isEmpty()) {
            System.out.println("\n" + rootState.boardAsString());
            final SearchResults<ChooseHole> results = moismcts.search(2, rootState, params, new Random());
            results.bestAction().applyToState(rootState);
        }
        System.out.println("\n" + rootState.boardAsString());
    }
}
