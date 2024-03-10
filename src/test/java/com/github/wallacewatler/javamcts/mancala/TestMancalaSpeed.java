package com.github.wallacewatler.javamcts.mancala;

import com.github.wallacewatler.javamcts.*;

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
    }

    private static void testItersPerMsMCTS(MCTS mcts, int threadCount) {
        final Sampler itersPerMs = new Sampler();

        for(int i = 0; i < 30; i++) {
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
