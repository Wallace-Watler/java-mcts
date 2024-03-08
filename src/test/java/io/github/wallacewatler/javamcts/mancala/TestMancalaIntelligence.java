package io.github.wallacewatler.javamcts.mancala;

import io.github.wallacewatler.javamcts.MCTSRP;
import io.github.wallacewatler.javamcts.UCT;
import io.github.wallacewatler.javamcts.SearchParameters;

import java.util.Random;
import java.util.Scanner;

public final class TestMancalaIntelligence {
    public static void main(String[] args) {
        testAgainstHuman();
    }

    private static void testAgainstHuman() {
        final MancalaState rootState = new MancalaState();

        final Scanner in = new Scanner(System.in);
        final SearchParameters params = new SearchParameters(1000, 3000, 1000000, new UCT(Math.sqrt(2), true), 2);

        while(!rootState.validActions().isEmpty()) {
            System.out.println();
            System.out.println(rootState.boardAsString());

            ChooseHole action;
            if(rootState.activePlayer == 0) {
                System.out.println("Your turn.");
                do {
                    action = new ChooseHole(in.nextInt());
                    if(rootState.validActions().contains(action))
                        break;

                    System.out.println("Hole " + action.hole() + " can't be selected.");
                } while(true);
            } else {
                action = new MCTSRP().search(2, rootState, params, new Random()).bestAction();
            }

            action.applyToState(rootState);
        }

        System.out.println();
        System.out.println(rootState.boardAsString());

        final double[] scores = rootState.scores();
        if(scores[0] == scores[1]) {
            System.out.println("Draw.");
        } else if(scores[0] > scores[1]) {
            System.out.println("Human wins!");
        } else {
            System.out.println("AI wins!");
        }
    }
}
