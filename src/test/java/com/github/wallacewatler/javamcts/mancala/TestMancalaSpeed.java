package com.github.wallacewatler.javamcts.mancala;

import com.github.wallacewatler.javamcts.*;

import java.util.Random;

final class TestMancalaSpeed {
    public static void main(String[] args) {
        test(new MCTSRP(), false, false, 1);
        test(new MCTSRP(), false, false, 2);
        test(new MCTSRP(), false, false, 3);
        test(new MCTSRP(), false, false, 4);
        test(new MCTSRP(), false,  true, 1);
        test(new MCTSRP(), false,  true, 2);
        test(new MCTSRP(), false,  true, 3);
        test(new MCTSRP(), false,  true, 4);
        test(new MCTSRP(),  true, false, 1);
        test(new MCTSRP(),  true, false, 2);
        test(new MCTSRP(),  true, false, 3);
        test(new MCTSRP(),  true, false, 4);
        test(new MCTSRP(),  true,  true, 1);
        test(new MCTSRP(),  true,  true, 2);
        test(new MCTSRP(),  true,  true, 3);
        test(new MCTSRP(),  true,  true, 4);

        test(new MCTSTP(), false, false, 1);
        test(new MCTSTP(), false, false, 2);
        test(new MCTSTP(), false, false, 3);
        test(new MCTSTP(), false, false, 4);
        test(new MCTSTP(), false,  true, 1);
        test(new MCTSTP(), false,  true, 2);
        test(new MCTSTP(), false,  true, 3);
        test(new MCTSTP(), false,  true, 4);
        test(new MCTSTP(),  true, false, 1);
        test(new MCTSTP(),  true, false, 2);
        test(new MCTSTP(),  true, false, 3);
        test(new MCTSTP(),  true, false, 4);
        test(new MCTSTP(),  true,  true, 1);
        test(new MCTSTP(),  true,  true, 2);
        test(new MCTSTP(),  true,  true, 3);
        test(new MCTSTP(),  true,  true, 4);
    }

    private static void test(MCTS mcts, boolean favorUnexplored, boolean useTable, int threadCount) {
        final Sampler itersPerSec = new Sampler();
        final Sampler numNodes = new Sampler();
        final Sampler numStates = new Sampler();
        final int warmupSamples = 10;
        final int realSamples = 30;

        final MancalaState rootState = new MancalaState();
        final UCT uct = new UCT(Math.sqrt(2), favorUnexplored);
        final SearchParameters params = new SearchParameters(0, 1000, Integer.MAX_VALUE, uct, threadCount);

        System.out.println("---- BEGIN TEST ----");
        System.out.println("Algorithm: " + mcts);
        System.out.println("Favor unexplored: " + favorUnexplored);
        System.out.println("Use table: " + useTable);
        System.out.println("Threads: " + threadCount);

        for(int i = 0; i < warmupSamples; i++) {
            final SearchResults<ChooseHole> results = mcts.search(2, rootState, params, new Random(), useTable);
            itersPerSec.addSample(1000 * results.itersPerThread() * threadCount / results.duration());
            numNodes.addSample(results.numNodes());
            numStates.addSample(results.numStates());
            final String itersPerSecCI = String.format("%,.0f ± %,.0f", itersPerSec.getMean(), itersPerSec.getStdDev() * 2);
            final String numNodesCI = String.format("%,.0f ± %,.0f", numNodes.getMean(), numNodes.getStdDev() * 2);
            final String numStatesCI = String.format("%,.0f ± %,.0f", numStates.getMean(), numStates.getStdDev() * 2);
            System.out.printf("[WARMUP] %s iterations / sec | %s nodes | %s states%n", itersPerSecCI, numNodesCI, numStatesCI);
        }

        itersPerSec.reset();

        for(int i = 0; i < realSamples; i++) {
            final SearchResults<ChooseHole> results = mcts.search(2, rootState, params, new Random(), useTable);
            itersPerSec.addSample(1000 * results.itersPerThread() * threadCount / results.duration());
            numNodes.addSample(results.numNodes());
            numStates.addSample(results.numStates());
            final String itersPerSecCI = String.format("%,.0f ± %,.0f", itersPerSec.getMean(), itersPerSec.getStdDev() * 2);
            final String numNodesCI = String.format("%,.0f ± %,.0f", numNodes.getMean(), numNodes.getStdDev() * 2);
            final String numStatesCI = String.format("%,.0f ± %,.0f", numStates.getMean(), numStates.getStdDev() * 2);
            System.out.printf("%s iterations / sec | %s nodes | %s states%n", itersPerSecCI, numNodesCI, numStatesCI);
        }

        System.out.println("----- END TEST -----");
    }
}
