package io.github.wallacewatler.javamcts.mancala;

import io.github.wallacewatler.javamcts.*;

import java.util.Random;

public final class TestMancalaCorrectness {
    public static void main(String[] args) {
        testMCTS();
        testOLMCTS();
        testMOISMCTS();
    }

    private static void testMCTS() {
        final MCTS<MancalaState, ChooseHole> mcts = new MCTS<>(2, new MancalaState());
        final SearchParameters params = new SearchParameters(0, Long.MAX_VALUE, 1000, new UCT(Math.sqrt(2), true), 1);
        while(!mcts.hasGameEnded()) {
            System.out.println("\n" + mcts.getRootState().boardAsString());
            final SearchResults<ChooseHole> results = mcts.search(params, new Random());
            mcts.advanceGame(results.bestAction());
        }
        System.out.println("\n" + mcts.getRootState().boardAsString());
    }

    private static void testOLMCTS() {
        final MancalaState rootState = new MancalaState();
        final SearchParameters params = new SearchParameters(0, Long.MAX_VALUE, 1000, new UCT(Math.sqrt(2), true), 1);
        while(!rootState.validActions().isEmpty()) {
            System.out.println("\n" + rootState.boardAsString());
            final SearchResults<ChooseHole> results = OLMCTS.search(2, rootState, params, new Random());
            results.bestAction().applyToState(rootState);
        }
        System.out.println("\n" + rootState.boardAsString());
    }

    private static void testMOISMCTS() {
        final MancalaState rootState = new MancalaState();
        final SearchParameters params = new SearchParameters(0, Long.MAX_VALUE, 1000, new UCT(Math.sqrt(2), true), 1);
        while(!rootState.validActions().isEmpty()) {
            System.out.println("\n" + rootState.boardAsString());
            final SearchResults<ChooseHole> results = MOISMCTS.search(2, rootState, params, new Random());
            results.bestAction().applyToState(rootState);
        }
        System.out.println("\n" + rootState.boardAsString());
    }
}
