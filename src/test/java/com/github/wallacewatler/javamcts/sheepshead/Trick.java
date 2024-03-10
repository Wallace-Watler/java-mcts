package com.github.wallacewatler.javamcts.sheepshead;

import java.util.Arrays;
import java.util.Objects;

public final class Trick {
    public final Card[] cards = new Card[4];

    /** The player who led the trick, or -1 if a card hasn't been led yet. */
    public int leader = -1;

    public Trick() {}

    public Trick(Trick trick) {
        cards[0] = trick.cards[0];
        cards[1] = trick.cards[1];
        cards[2] = trick.cards[2];
        cards[3] = trick.cards[3];
        leader = trick.leader;
    }

    public void layCard(int player, Card card) {
        cards[player] = card;
        if(isEmpty())
            leader = player;
    }

    public boolean isEmpty() {
        return leader < 0;
    }

    public boolean isComplete() {
        return Arrays.stream(cards).noneMatch(Objects::isNull);
    }

    public Card ledCard() {
        return isEmpty() ? null : cards[leader];
    }

    public int winner() {
        int highest = leader;

        for(int i = 0; i < 4; i++) {
            final Card card = cards[i];
            final Card highestCard = cards[highest];
            final int trumpPower = Sheepshead.trumpPower(card);
            final int highestTrumpPower = Sheepshead.trumpPower(highestCard);

            if(trumpPower < 0 && highestTrumpPower < 0) {
                if(card.suit() == highestCard.suit() && Sheepshead.suitPower(card) > Sheepshead.suitPower(highestCard))
                    highest = i;
            } else if(trumpPower > highestTrumpPower) {
                highest = i;
            }
        }

        return highest;
    }

    @Override
    public String toString() {
        return "Trick{" +
                "cards=" + Arrays.toString(cards) +
                ", leader=" + leader +
                '}';
    }
}
