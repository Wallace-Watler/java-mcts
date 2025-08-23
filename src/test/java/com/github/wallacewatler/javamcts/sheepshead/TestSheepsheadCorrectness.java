package com.github.wallacewatler.javamcts.sheepshead;

import com.github.wallacewatler.javamcts.*;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public final class TestSheepsheadCorrectness {
    public static void main(String[] args) {
        testISMCTS(new ISMCTSRP());
        testISMCTS(new ISMCTSTP());
    }

    private static void testISMCTS(ISMCTS ismcts) {
        final Random rand = new Random();

        final SheepsheadState rootState = new SheepsheadState(rand);
        final List<InfoSet> infoSets = IntStream.range(0, 4).mapToObj(i -> new InfoSet(rootState, i)).toList();

        final UCT uct = new UCT(Math.sqrt(2), true);
        final SearchParameters params = new SearchParameters(0, Long.MAX_VALUE, 1000, uct, 4);

        while(!rootState.validActions().isEmpty()) {
            System.out.println("Table: " + Arrays.toString(rootState.trickOnTable.cards) + ", led " + rootState.trickOnTable.ledCard());

            final SearchResults<PlayCard> results = ismcts.search(4, infoSets.get(rootState.activePlayer), params, rand);
            final PlayCard bestAction = results.bestAction();
            bestAction.applyToState(rootState, rand);
            for(int pov = 0; pov < 4; pov++)
                bestAction.applyToInfoSet(infoSets.get(pov));
        }
        System.out.println("Table: " + Arrays.toString(rootState.trickOnTable.cards) + ", led " + rootState.trickOnTable.ledCard());
    }
}
