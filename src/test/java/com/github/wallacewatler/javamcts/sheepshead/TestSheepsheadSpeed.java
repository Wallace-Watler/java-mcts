package com.github.wallacewatler.javamcts.sheepshead;

import com.github.wallacewatler.javamcts.*;
import io.github.wallacewatler.javamcts.*;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public final class TestSheepsheadSpeed {
    public static void main(String[] args) {
        testItersPerMs(new MOISMCTSRP(), 1);
        testItersPerMs(new MOISMCTSRP(), 2);
        testItersPerMs(new MOISMCTSRP(), 3);
        testItersPerMs(new MOISMCTSRP(), 4);
        testItersPerMs(new MOISMCTSTP(), 1);
        testItersPerMs(new MOISMCTSTP(), 2);
        testItersPerMs(new MOISMCTSTP(), 3);
        testItersPerMs(new MOISMCTSTP(), 4);
    }

    private static void testItersPerMs(MOISMCTS moismcts, int threadCount) {
        final Sampler itersPerMs = new Sampler();

        for(int i = 0; i < 100; i++) {
            final Random rand = new Random();
            final SheepsheadState rootState = new SheepsheadState(rand);
            final List<InfoSet> infoSets = IntStream.range(0, 4).mapToObj(pov -> new InfoSet(rootState, pov)).toList();

            final SearchParameters params = new SearchParameters(0, 100, Integer.MAX_VALUE, new UCT(Math.sqrt(2), true), threadCount);
            double iters = 0;
            long duration = 0;
            while(!rootState.validActions().isEmpty()) {
                final SearchResults<PlayCard> results = moismcts.search(4, infoSets.get(rootState.activePlayer), params, rand);
                final PlayCard bestAction = results.bestAction();
                bestAction.applyToState(rootState, rand);
                for(int pov = 0; pov < 4; pov++)
                    bestAction.observe(pov).applyToInfoSet(infoSets.get(pov));

                iters += results.itersPerThread() * threadCount;
                duration += results.duration();
            }

            itersPerMs.addSample(iters / duration);
            final String itersPerMsCI = String.format("%,.3f ± %,.3f", itersPerMs.getMean(), itersPerMs.getStdDev() * 2);
            System.out.printf("%s, %d threads: %s iterations / ms%n", moismcts, threadCount, itersPerMsCI);
        }
    }
}
