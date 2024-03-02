package io.github.wallacewatler.javamcts.mancala;

import io.github.wallacewatler.javamcts.*;

import java.util.Random;

public final class TestMancalaSpeed {
    public static void main(String[] args) {
        testItersPerMsMCTS(1);
        testItersPerMsMCTS(2);
        testItersPerMsMCTS(3);
        testItersPerMsMCTS(4);
        testItersPerMsOLMCTS(1);
        testItersPerMsOLMCTS(2);
        testItersPerMsOLMCTS(3);
        testItersPerMsOLMCTS(4);
        testItersPerMsMOISMCTS(1);
        testItersPerMsMOISMCTS(2);
        testItersPerMsMOISMCTS(3);
        testItersPerMsMOISMCTS(4);
    }

    private static void testItersPerMsMCTS(int threadCount) {
        final Sampler itersPerMs = new Sampler();

        for(int i = 0; i < 10; i++) {
            final MCTS<MancalaState, ChooseHole> mcts = new MCTS<>(2, new MancalaState());
            final SearchParameters params = new SearchParameters(0, 100, Integer.MAX_VALUE, new UCT(Math.sqrt(2), true), threadCount);
            int iters = 0;
            long duration = 0;
            while(!mcts.hasGameEnded()) {
                final SearchResults<ChooseHole> results = mcts.search(params, new Random());
                mcts.advanceGame(results.bestAction());
                iters += results.iters();
                duration += results.duration();
            }

            itersPerMs.addSample((double) iters / duration);
            final String itersPerMsCI = String.format("%,.0f ± %,.0f", itersPerMs.getMean(), itersPerMs.getStdDev() * 2);
            System.out.printf("MCTS, %d threads: %s iterations / ms%n", threadCount, itersPerMsCI);
        }
    }

    private static void testItersPerMsOLMCTS(int threadCount) {
        final Sampler itersPerMs = new Sampler();

        for(int i = 0; i < 10; i++) {
            final MancalaState rootState = new MancalaState();
            final SearchParameters params = new SearchParameters(0, 100, Integer.MAX_VALUE, new UCT(Math.sqrt(2), true), threadCount);
            int iters = 0;
            long duration = 0;
            while(!rootState.validActions().isEmpty()) {
                final SearchResults<ChooseHole> results = OLMCTS.search(2, rootState, params, new Random());
                results.bestAction().applyToState(rootState);
                iters += results.iters();
                duration += results.duration();
            }

            itersPerMs.addSample((double) iters / duration);
            final String itersPerMsCI = String.format("%,.0f ± %,.0f", itersPerMs.getMean(), itersPerMs.getStdDev() * 2);
            System.out.printf("OLMCTS, %d threads: %s iterations / ms%n", threadCount, itersPerMsCI);
        }
    }

    private static void testItersPerMsMOISMCTS(int threadCount) {
        final Sampler itersPerMs = new Sampler();

        for(int i = 0; i < 10; i++) {
            final MancalaState rootState = new MancalaState();
            final SearchParameters params = new SearchParameters(0, 100, Integer.MAX_VALUE, new UCT(Math.sqrt(2), true), threadCount);
            int iters = 0;
            long duration = 0;
            while(!rootState.validActions().isEmpty()) {
                final SearchResults<ChooseHole> results = MOISMCTS.search(2, rootState, params, new Random());
                results.bestAction().applyToState(rootState);
                iters += results.iters();
                duration += results.duration();
            }

            itersPerMs.addSample((double) iters / duration);
            final String itersPerMsCI = String.format("%,.0f ± %,.0f", itersPerMs.getMean(), itersPerMs.getStdDev() * 2);
            System.out.printf("MO-ISMCTS, %d threads: %s iterations / ms%n", threadCount, itersPerMsCI);
        }
    }
}
