package wallacewatler.javamcts.mancala;

import wallacewatler.javamcts.Sampler;
import wallacewatler.javamcts.SearchParameters;
import wallacewatler.javamcts.SearchResults;
import wallacewatler.javamcts.UCT;
import wallacewatler.javamcts.mcts.MCTS;
import wallacewatler.javamcts.olmcts.OLMCTS;

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
    }

    private static void testItersPerMsMCTS(int threadCount) {
        final Sampler itersPerMs = new Sampler();

        for(int i = 0; i < 10; i++) {
            final MancalaState rootState = new MancalaState();
            final SearchParameters params = new SearchParameters(0, 100, Integer.MAX_VALUE, new UCT(Math.sqrt(2), true), threadCount);
            int iters = 0;
            long duration = 0;
            while(!rootState.isTerminal()) {
                final SearchResults<ChooseHole> results = MCTS.search(2, rootState, params);
                results.optimalAction().applyToState(rootState);
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
            while(!rootState.isTerminal()) {
                final SearchResults<ChooseHole> results = OLMCTS.search(2, rootState, params);
                results.optimalAction().applyToState(rootState);
                iters += results.iters();
                duration += results.duration();
            }

            itersPerMs.addSample((double) iters / duration);
            final String itersPerMsCI = String.format("%,.0f ± %,.0f", itersPerMs.getMean(), itersPerMs.getStdDev() * 2);
            System.out.printf("OLMCTS, %d threads: %s iterations / ms%n", threadCount, itersPerMsCI);
        }
    }
}
