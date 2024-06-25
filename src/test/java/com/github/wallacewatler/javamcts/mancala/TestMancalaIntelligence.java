package com.github.wallacewatler.javamcts.mancala;

import com.github.wallacewatler.javamcts.UCT;
import com.github.wallacewatler.javamcts.MCTSRP;
import com.github.wallacewatler.javamcts.SearchParameters;

import java.util.Random;
import java.util.Scanner;

final class TestMancalaIntelligence {
    public static void main(String[] args) {
        final MancalaState rootState = new MancalaState();

        final UCT uct = new UCT(Math.sqrt(2), true);
        final SearchParameters params = new SearchParameters(0, 3000, Integer.MAX_VALUE, uct, 4);

        final Scanner in = new Scanner(System.in);
        while(!rootState.validActions().isEmpty()) {
            System.out.println();
            System.out.println(rootState.displayString());

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
        System.out.println(rootState.displayString());

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
