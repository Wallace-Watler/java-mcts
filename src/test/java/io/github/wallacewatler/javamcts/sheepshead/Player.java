package io.github.wallacewatler.javamcts.sheepshead;

import java.util.ArrayList;
import java.util.Collection;

public final class Player {
    public final ArrayList<Card> hand = new ArrayList<>(7);
    public final ArrayList<Trick> takenTricks = new ArrayList<>(7);

    public Player(Collection<Card> hand) {
        this.hand.addAll(hand);
    }

    @Override
    public String toString() {
        return "Player{" +
                "hand=" + hand +
                ", takenTricks=" + takenTricks +
                '}';
    }
}
