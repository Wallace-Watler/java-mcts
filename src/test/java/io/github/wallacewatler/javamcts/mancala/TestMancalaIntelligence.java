package io.github.wallacewatler.javamcts.mancala;

import io.github.wallacewatler.javamcts.UCT;
import io.github.wallacewatler.javamcts.SearchParameters;
import io.github.wallacewatler.javamcts.MCTS;

import java.util.Random;
import java.util.Scanner;

public final class TestMancalaIntelligence {
    public static void main(String[] args) {
        testAgainstHuman();
    }

    private static void testAgainstHuman() {
        final Scanner in = new Scanner(System.in);
        final MCTS<MancalaState, ChooseHole> mcts = new MCTS<>(2, new MancalaState());
        final SearchParameters params = new SearchParameters(1000, 3000, 1000000, new UCT(Math.sqrt(2), true), 1);

        while(!mcts.hasGameEnded()) {
            System.out.println();
            System.out.println(mcts.getRootState().boardAsString());

            ChooseHole action;
            if(mcts.getRootState().activePlayer == 0) {
                System.out.println("Your turn.");
                do {
                    action = new ChooseHole(in.nextInt());
                    if(mcts.getRootState().validActions().contains(action))
                        break;

                    System.out.println("Hole " + action.hole() + " can't be selected.");
                } while(true);
            } else {
                action = mcts.search(params, new Random()).bestAction();
            }

            mcts.advanceGame(action);
        }

        System.out.println();
        System.out.println(mcts.getRootState().boardAsString());

        final double[] scores = mcts.getRootState().scores();
        if(scores[0] == scores[1]) {
            System.out.println("Draw.");
        } else if(scores[0] > scores[1]) {
            System.out.println("Human wins!");
        } else {
            System.out.println("AI wins!");
        }
    }
}
