package io.github.wallacewatler.javamcts.sheepshead;

import io.github.wallacewatler.javamcts.State;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class GameState implements State<PlayCard> {
    public int activePlayer;
    public final Player[] players = new Player[4];
    public final Card[] blind = new Card[2];
    public Trick trickOnTable;

    public GameState(Random rand) {
        final List<Card> deck = Sheepshead.newDeck();
        Collections.shuffle(deck, rand);

        activePlayer = 0;
        players[0] = new Player(deck.subList(0, 7));
        players[1] = new Player(deck.subList(7, 14));
        players[2] = new Player(deck.subList(14, 21));
        players[3] = new Player(deck.subList(21, 28));
        blind[0] = deck.get(28);
        blind[1] = deck.get(29);
        trickOnTable = new Trick();
    }

    public GameState() {}

    @Override
    public int activePlayer() {
        return activePlayer;
    }

    @Override
    public List<PlayCard> validActions() {
        return Sheepshead.validActions(players[activePlayer], trickOnTable);
    }

    @Override
    public double[] scores() {
        if(!players[activePlayer].hand.isEmpty())
            return null;

        final double[] rewards = new double[4];
        for(int i = 0; i < 4; i++) {
            final int ii = (i + 2) % 4;
            for(Trick trick : players[i].takenTricks) {
                for(Card card : trick.cards) {
                    switch(card.rank()) {
                        case ACE -> { rewards[i] += 11; rewards[ii] += 11; }
                        case TEN -> { rewards[i] += 10; rewards[ii] += 10; }
                        case KING -> { rewards[i] += 4; rewards[ii] += 4; }
                        case QUEEN -> { rewards[i] += 3; rewards[ii] += 3; }
                        case JACK -> { rewards[i] += 2; rewards[ii] += 2; }
                    }
                }
            }
        }
        return rewards;
    }

    @Override
    public String toString() {
        return "GameState{" +
                "activePlayer=" + activePlayer +
                ", players=" + Arrays.toString(players) +
                ", blind=" + Arrays.toString(blind) +
                ", trickOnTable=" + trickOnTable +
                '}';
    }
}
