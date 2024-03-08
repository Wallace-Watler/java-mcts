package io.github.wallacewatler.javamcts.sheepshead;

import io.github.wallacewatler.javamcts.*;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public final class TestSheepsheadCorrectness {
    public static void main(String[] args) {
        testMOISMCTS(new MOISMCTSRP());
        testMOISMCTS(new MOISMCTSTP());
    }

    private static void testMOISMCTS(MOISMCTS moismcts) {
        final Random rand = new Random();
        final long seed = rand.nextLong();
        rand.setSeed(seed);
        System.out.println("Seed: " + seed);

        final GameState rootState = new GameState(rand);
        final List<InfoSet> infoSets = IntStream.range(0, 4).mapToObj(i -> new InfoSet(rootState, i)).toList();
        final SearchParameters params = new SearchParameters(0, Long.MAX_VALUE, 1000, new UCT(Math.sqrt(2), true), 3);
        while(!rootState.validActions().isEmpty()) {
            System.out.println("Table: " + Arrays.toString(rootState.trickOnTable.cards) + ", led " + rootState.trickOnTable.ledCard());
            final SearchResults<PlayCard> results = moismcts.search(4, infoSets.get(rootState.activePlayer), params, new Random());
            final PlayCard bestAction = results.bestAction();
            bestAction.applyToState(rootState, rand);
            for(int pov = 0; pov < 4; pov++)
                bestAction.applyToInfoSet(infoSets.get(pov));
        }
        System.out.println("Table: " + Arrays.toString(rootState.trickOnTable.cards) + ", led " + rootState.trickOnTable.ledCard());
    }
}
