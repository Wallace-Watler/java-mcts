package io.github.wallacewatler.javamcts.mancala;

import io.github.wallacewatler.javamcts.*;

import java.util.Random;

public final class TestMancalaSpeed {
    public static void main(String[] args) {
        testItersPerMsMCTS(new MCTSRP(), 1);
        testItersPerMsMCTS(new MCTSRP(), 2);
        testItersPerMsMCTS(new MCTSRP(), 3);
        testItersPerMsMCTS(new MCTSRP(), 4);
        testItersPerMsMCTS(new MCTSTP(), 1);
        testItersPerMsMCTS(new MCTSTP(), 2);
        testItersPerMsMCTS(new MCTSTP(), 3);
        testItersPerMsMCTS(new MCTSTP(), 4);
        testItersPerMsOLMCTS(new OLMCTSRP(), 1);
        testItersPerMsOLMCTS(new OLMCTSRP(), 2);
        testItersPerMsOLMCTS(new OLMCTSRP(), 3);
        testItersPerMsOLMCTS(new OLMCTSRP(), 4);
        testItersPerMsOLMCTS(new OLMCTSTP(), 1);
        testItersPerMsOLMCTS(new OLMCTSTP(), 2);
        testItersPerMsOLMCTS(new OLMCTSTP(), 3);
        testItersPerMsOLMCTS(new OLMCTSTP(), 4);
        testItersPerMsMOISMCTS(new MOISMCTSRP(), 1);
        testItersPerMsMOISMCTS(new MOISMCTSRP(), 2);
        testItersPerMsMOISMCTS(new MOISMCTSRP(), 3);
        testItersPerMsMOISMCTS(new MOISMCTSRP(), 4);
        testItersPerMsMOISMCTS(new MOISMCTSTP(), 1);
        testItersPerMsMOISMCTS(new MOISMCTSTP(), 2);
        testItersPerMsMOISMCTS(new MOISMCTSTP(), 3);
        testItersPerMsMOISMCTS(new MOISMCTSTP(), 4);
    }

    private static void testItersPerMsMCTS(MCTS mcts, int threadCount) {
        final Sampler itersPerMs = new Sampler();

        for(int i = 0; i < 30; i++) {
            final MancalaState rootState = new MancalaState();
            final SearchParameters params = new SearchParameters(0, 100, Integer.MAX_VALUE, new UCT(Math.sqrt(2), true), threadCount);
            double iters = 0;
            long duration = 0;
            while(!rootState.validActions().isEmpty()) {
                final SearchResults<ChooseHole> results = mcts.search(2, rootState, params, new Random());
                results.bestAction().applyToState(rootState);
                iters += results.itersPerThread() * threadCount;
                duration += results.duration();
            }

            itersPerMs.addSample(iters / duration);
            final String itersPerMsCI = String.format("%,.0f ± %,.0f", itersPerMs.getMean(), itersPerMs.getStdDev() * 2);
            System.out.printf("%s, %d threads: %s iterations / ms%n", mcts, threadCount, itersPerMsCI);
        }
    }

    private static void testItersPerMsOLMCTS(OLMCTS olmcts, int threadCount) {
        final Sampler itersPerMs = new Sampler();

        for(int i = 0; i < 30; i++) {
            final MancalaState rootState = new MancalaState();
            final SearchParameters params = new SearchParameters(0, 100, Integer.MAX_VALUE, new UCT(Math.sqrt(2), true), threadCount);
            double iters = 0;
            long duration = 0;
            while(!rootState.validActions().isEmpty()) {
                final SearchResults<ChooseHole> results = olmcts.search(2, rootState, params, new Random());
                results.bestAction().applyToState(rootState);
                iters += results.itersPerThread() * threadCount;
                duration += results.duration();
            }

            itersPerMs.addSample(iters / duration);
            final String itersPerMsCI = String.format("%,.0f ± %,.0f", itersPerMs.getMean(), itersPerMs.getStdDev() * 2);
            System.out.printf("%s, %d threads: %s iterations / ms%n", olmcts, threadCount, itersPerMsCI);
        }
    }

    private static void testItersPerMsMOISMCTS(MOISMCTS moismcts, int threadCount) {
        final Sampler itersPerMs = new Sampler();

        for(int i = 0; i < 30; i++) {
            final MancalaState rootState = new MancalaState();
            final SearchParameters params = new SearchParameters(0, 100, Integer.MAX_VALUE, new UCT(Math.sqrt(2), true), threadCount);
            double iters = 0;
            long duration = 0;
            while(!rootState.validActions().isEmpty()) {
                final SearchResults<ChooseHole> results = moismcts.search(2, rootState, params, new Random());
                results.bestAction().applyToState(rootState);
                iters += results.itersPerThread() * threadCount;
                duration += results.duration();
            }

            itersPerMs.addSample(iters / duration);
            final String itersPerMsCI = String.format("%,.0f ± %,.0f", itersPerMs.getMean(), itersPerMs.getStdDev() * 2);
            System.out.printf("%s, %d threads: %s iterations / ms%n", moismcts, threadCount, itersPerMsCI);
        }
    }
}
