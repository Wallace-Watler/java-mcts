package io.github.wallacewatler.javamcts.sheepshead;

import io.github.wallacewatler.javamcts.*;

import java.util.*;
import java.util.stream.IntStream;

public final class TestSheepsheadIntelligence {
    public static void main(String[] args) {
        testAgainstHuman(new MOISMCTSTP());
    }

    private static void testAgainstHuman(MOISMCTS moismcts) {
        final Scanner in = new Scanner(System.in);
        final Random rand = new Random();
        long seed = rand.nextLong();
        rand.setSeed(seed);
        System.out.println("Seed: " + seed);

        final GameState rootState = new GameState(rand);
        final List<InfoSet> infoSets = IntStream.range(0, 4).mapToObj(i -> new InfoSet(rootState, i)).toList();
        final SearchParameters params = new SearchParameters(1000, 5000, 100000, new UCT(60 * Math.sqrt(2), true), 4);

        while(!rootState.validActions().isEmpty()) {
            System.out.println("Table: " + Arrays.toString(rootState.trickOnTable.cards) + ", led " + rootState.trickOnTable.ledCard());
            final int player = rootState.activePlayer;

            final PlayCard action;
            if(player == 3) {
                System.out.println("Hand: " + rootState.players[3].hand);
                final List<PlayCard> availableActions = rootState.validActions();
                for(int i = 0; i < availableActions.size(); i++)
                    System.out.printf("- %d) %s%n", i + 1, availableActions.get(i).card());

                action = availableActions.get(in.nextInt() - 1);
            } else {
                action = moismcts.search(4, infoSets.get(player), params, rand).bestAction();
            }
            System.out.println("Player " + player + " plays " + action.card() + ".");

            action.applyToState(rootState, rand);
            for(int pov = 0; pov < 4; pov++)
                action.observe(pov).applyToInfoSet(infoSets.get(pov));
        }

        final double[] rewards = rootState.scores();
        System.out.println("Players 0 and 2 score: " + (int) rewards[0]);
        System.out.println("Players 1 and 3 score: " + (int) rewards[1]);
    }
}
