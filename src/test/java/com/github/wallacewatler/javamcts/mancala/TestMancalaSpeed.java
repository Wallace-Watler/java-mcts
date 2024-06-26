package com.github.wallacewatler.javamcts.mancala;

import com.github.wallacewatler.javamcts.*;

import java.util.Random;

final class TestMancalaSpeed {
    public static void main(String[] args) {
        test(new MCTSRP(), 1);
        test(new MCTSRP(), 2);
        test(new MCTSRP(), 3);
        test(new MCTSRP(), 4);
        test(new MCTSTP(), 1);
        test(new MCTSTP(), 2);
        test(new MCTSTP(), 3);
        test(new MCTSTP(), 4);
    }

    private static void test(MCTS mcts, int threadCount) {
        final Sampler itersPerSec = new Sampler();
        final Sampler numNodes = new Sampler();
        final Sampler numStates = new Sampler();
        final int warmupSamples = 10;
        final int realSamples = 30;

        final MancalaState rootState = new MancalaState();
        final UCT uct = new UCT(Math.sqrt(2), true);
        final SearchParameters params = new SearchParameters(0, 1000, Integer.MAX_VALUE, uct, threadCount);
        final boolean useTTable = true;

        for(int i = 0; i < warmupSamples; i++) {
            final SearchResults<ChooseHole> results = mcts.search(2, rootState, params, new Random(), useTTable);
            itersPerSec.addSample(1000 * results.itersPerThread() * threadCount / results.duration());
            numNodes.addSample(results.numNodes());
            numStates.addSample(results.numStates());
            final String itersPerSecCI = String.format("%,.0f ± %,.0f", itersPerSec.getMean(), itersPerSec.getStdDev() * 2);
            final String numNodesCI = String.format("%,.0f ± %,.0f", numNodes.getMean(), numNodes.getStdDev() * 2);
            final String numStatesCI = String.format("%,.0f ± %,.0f", numStates.getMean(), numStates.getStdDev() * 2);
            System.out.printf("[WARMUP] %s, %d threads | %s iterations / sec | %s nodes | %s states%n", mcts, threadCount, itersPerSecCI, numNodesCI, numStatesCI);
        }

        itersPerSec.reset();

        for(int i = 0; i < realSamples; i++) {
            final SearchResults<ChooseHole> results = mcts.search(2, rootState, params, new Random(), useTTable);
            itersPerSec.addSample(1000 * results.itersPerThread() * threadCount / results.duration());
            numNodes.addSample(results.numNodes());
            numStates.addSample(results.numStates());
            final String itersPerSecCI = String.format("%,.0f ± %,.0f", itersPerSec.getMean(), itersPerSec.getStdDev() * 2);
            final String numNodesCI = String.format("%,.0f ± %,.0f", numNodes.getMean(), numNodes.getStdDev() * 2);
            final String numStatesCI = String.format("%,.0f ± %,.0f", numStates.getMean(), numStates.getStdDev() * 2);
            System.out.printf("%s, %d threads | %s iterations / sec | %s nodes | %s states%n", mcts, threadCount, itersPerSecCI, numNodesCI, numStatesCI);
        }
    }
}
