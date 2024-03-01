package wallacewatler.javamcts.mancala;

import wallacewatler.javamcts.SearchParameters;
import wallacewatler.javamcts.UCT;
import wallacewatler.javamcts.mcts.MCTS;

import java.util.Scanner;

public final class TestMancalaIntelligence {
    public static void main(String[] args) {
        testAgainstHuman();
    }

    private static void testAgainstHuman() {
        final Scanner in = new Scanner(System.in);
        final SearchParameters params = new SearchParameters(1000, 3000, 1000000, new UCT(Math.sqrt(2), true), 1);

        final MancalaState rootState = new MancalaState();
        while(!rootState.isTerminal()) {
            System.out.println();
            System.out.println(rootState.boardAsString());

            ChooseHole action;
            if(rootState.playerAboutToMove == 0) {
                System.out.println("Your turn.");
                do {
                    action = new ChooseHole(in.nextInt());
                    if(rootState.availableActions().contains(action))
                        break;

                    System.out.println("Hole " + action.hole() + " can't be selected.");
                } while(true);
            } else {
                action = MCTS.search(2, rootState, params).optimalAction();
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
