package com.github.wallacewatler.javamcts.mancala;

import com.github.wallacewatler.javamcts.*;

import java.util.Random;

final class TestMancalaSpeed {
    public static void main(String[] args) {
        testItersPerMs(new MCTSRP(), 1);
        testItersPerMs(new MCTSRP(), 2);
        testItersPerMs(new MCTSRP(), 3);
        testItersPerMs(new MCTSRP(), 4);
        testItersPerMs(new MCTSTP(), 1);
        testItersPerMs(new MCTSTP(), 2);
        testItersPerMs(new MCTSTP(), 3);
        testItersPerMs(new MCTSTP(), 4);
    }

    private static void testItersPerMs(MCTS mcts, int threadCount) {
        final Sampler itersPerMs = new Sampler();
        final int numSamples = 30;

        for(int i = 0; i < numSamples; i++) {
            final MancalaState rootState = new MancalaState();
            final UCT uct = new UCT(Math.sqrt(2), true);
            final SearchParameters params = new SearchParameters(0, 100, Integer.MAX_VALUE, uct, threadCount);
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
}
