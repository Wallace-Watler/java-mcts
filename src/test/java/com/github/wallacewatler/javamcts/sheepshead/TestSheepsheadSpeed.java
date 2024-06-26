package com.github.wallacewatler.javamcts.sheepshead;

import com.github.wallacewatler.javamcts.*;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public final class TestSheepsheadSpeed {
    public static void main(String[] args) {
        test(new MOISMCTSRP(), 1);
        test(new MOISMCTSRP(), 2);
        test(new MOISMCTSRP(), 3);
        test(new MOISMCTSRP(), 4);
        test(new MOISMCTSTP(), 1);
        test(new MOISMCTSTP(), 2);
        test(new MOISMCTSTP(), 3);
        test(new MOISMCTSTP(), 4);
    }

    private static void test(MOISMCTS moismcts, int threadCount) {
        final Sampler itersPerSec = new Sampler();
        final Sampler numNodes = new Sampler();
        final Sampler numStates = new Sampler();
        final int warmupSamples = 10;
        final int realSamples = 30;

        final Random rand = new Random();
        final SheepsheadState rootState = new SheepsheadState(rand);
        final List<InfoSet> infoSets = IntStream.range(0, 4).mapToObj(pov -> new InfoSet(rootState, pov)).toList();
        final UCT uct = new UCT(Math.sqrt(2), true);
        final SearchParameters params = new SearchParameters(0, 1000, Integer.MAX_VALUE, uct, threadCount);

        for(int i = 0; i < warmupSamples; i++) {
            final SearchResults<PlayCard> results = moismcts.search(4, infoSets.get(rootState.activePlayer), params, rand);
            itersPerSec.addSample(1000 * results.itersPerThread() * threadCount / results.duration());
            numNodes.addSample(results.numNodes());
            numStates.addSample(results.numStates());
            final String itersPerSecCI = String.format("%,.0f ± %,.0f", itersPerSec.getMean(), itersPerSec.getStdDev() * 2);
            final String numNodesCI = String.format("%,.0f ± %,.0f", numNodes.getMean(), numNodes.getStdDev() * 2);
            final String numStatesCI = String.format("%,.0f ± %,.0f", numStates.getMean(), numStates.getStdDev() * 2);
            System.out.printf("[WARMUP] %s, %d threads | %s iterations / sec | %s nodes | %s states%n", moismcts, threadCount, itersPerSecCI, numNodesCI, numStatesCI);
        }

        itersPerSec.reset();

        for(int i = 0; i < realSamples; i++) {
            final SearchResults<PlayCard> results = moismcts.search(4, infoSets.get(rootState.activePlayer), params, rand);
            itersPerSec.addSample(1000 * results.itersPerThread() * threadCount / results.duration());
            numNodes.addSample(results.numNodes());
            numStates.addSample(results.numStates());
            final String itersPerSecCI = String.format("%,.0f ± %,.0f", itersPerSec.getMean(), itersPerSec.getStdDev() * 2);
            final String numNodesCI = String.format("%,.0f ± %,.0f", numNodes.getMean(), numNodes.getStdDev() * 2);
            final String numStatesCI = String.format("%,.0f ± %,.0f", numStates.getMean(), numStates.getStdDev() * 2);
            System.out.printf("%s, %d threads | %s iterations / sec | %s nodes | %s states%n", moismcts, threadCount, itersPerSecCI, numNodesCI, numStatesCI);
        }
    }
}
