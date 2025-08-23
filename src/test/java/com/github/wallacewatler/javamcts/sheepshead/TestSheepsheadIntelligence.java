package com.github.wallacewatler.javamcts.sheepshead;

import com.github.wallacewatler.javamcts.ISMCTSTP;
import com.github.wallacewatler.javamcts.SearchParameters;
import com.github.wallacewatler.javamcts.UCT;

import java.util.*;
import java.util.stream.IntStream;

public final class TestSheepsheadIntelligence {
    public static void main(String[] args) {
        final Scanner in = new Scanner(System.in);
        final Random rand = new Random();

        final SheepsheadState rootState = new SheepsheadState(rand);
        final List<InfoSet> infoSets = IntStream.range(0, 4).mapToObj(i -> new InfoSet(rootState, i)).toList();

        final UCT uct = new UCT(60 * Math.sqrt(2), true);
        final SearchParameters params = new SearchParameters(1000, 5000, 100000, uct, 4);

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
                action = new ISMCTSTP().search(4, infoSets.get(player), params, rand).bestAction();
            }
            System.out.println("Player " + player + " plays " + action.card() + ".");

            for(int pov = 0; pov < 4; pov++)
                action.applyToInfoSet(infoSets.get(pov));

            action.applyToState(rootState, rand);
        }

        final double[] rewards = rootState.scores();
        System.out.println("Players 0 and 2 score: " + (int) rewards[0]);
        System.out.println("Players 1 and 3 score: " + (int) rewards[1]);
    }
}
