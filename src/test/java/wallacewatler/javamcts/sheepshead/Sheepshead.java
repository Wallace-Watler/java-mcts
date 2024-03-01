package wallacewatler.javamcts.sheepshead;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public final class Sheepshead {
    public static List<Card> newDeck() {
        return Arrays.asList(
                new Card(Rank.QUEEN, Suit.CLUBS),
                new Card(Rank.QUEEN, Suit.SPADES),
                new Card(Rank.QUEEN, Suit.HEARTS),
                new Card(Rank.QUEEN, Suit.DIAMONDS),
                new Card(Rank.JACK, Suit.CLUBS),
                new Card(Rank.JACK, Suit.SPADES),
                new Card(Rank.JACK, Suit.HEARTS),
                new Card(Rank.JACK, Suit.DIAMONDS),
                new Card(Rank.ACE, Suit.DIAMONDS),
                new Card(Rank.TEN, Suit.DIAMONDS),
                new Card(Rank.KING, Suit.DIAMONDS),
                new Card(Rank.NINE, Suit.DIAMONDS),
                new Card(Rank.EIGHT, Suit.DIAMONDS),
                new Card(Rank.SEVEN, Suit.DIAMONDS),
                new Card(Rank.ACE, Suit.CLUBS),
                new Card(Rank.TEN, Suit.CLUBS),
                new Card(Rank.KING, Suit.CLUBS),
                new Card(Rank.NINE, Suit.CLUBS),
                new Card(Rank.EIGHT, Suit.CLUBS),
                new Card(Rank.ACE, Suit.SPADES),
                new Card(Rank.TEN, Suit.SPADES),
                new Card(Rank.KING, Suit.SPADES),
                new Card(Rank.NINE, Suit.SPADES),
                new Card(Rank.EIGHT, Suit.SPADES),
                new Card(Rank.ACE, Suit.HEARTS),
                new Card(Rank.TEN, Suit.HEARTS),
                new Card(Rank.KING, Suit.HEARTS),
                new Card(Rank.NINE, Suit.HEARTS),
                new Card(Rank.EIGHT, Suit.HEARTS),
                new Card(Rank.SEVEN, Suit.HEARTS)
        );
    }

    public static List<PlayCard> availableActions(Player player, Trick trickOnTable) {
        final Function<Card, PlayCard> actionMap = PlayCard::new;

        if(trickOnTable.isEmpty())
            return player.hand.stream().map(actionMap).toList();

        final Card ledCard = trickOnTable.ledCard();
        assert ledCard != null;

        if(Sheepshead.isTrump(ledCard)) {
            final List<PlayCard> actions = player.hand.stream().filter(Sheepshead::isTrump).map(actionMap).toList();
            return actions.isEmpty() ? player.hand.stream().map(actionMap).toList() : actions;
        } else {
            final List<PlayCard> actions = player.hand.stream().filter(card -> isFail(card, ledCard.suit())).map(actionMap).toList();
            return actions.isEmpty() ? player.hand.stream().map(actionMap).toList() : actions;
        }
    }

    public static boolean isTrump(Card card) {
        return card.rank() == Rank.QUEEN || card.rank() == Rank.JACK || card.suit() == Suit.DIAMONDS;
    }

    public static boolean isFail(Card card, Suit suit) {
        return !isTrump(card) && card.suit() == suit;
    }

    public static int trumpPower(Card card) {
        if(card.rank() == Rank.QUEEN) {
            return switch(card.suit()) {
                case CLUBS -> 13;
                case SPADES -> 12;
                case HEARTS -> 11;
                case DIAMONDS -> 10;
            };
        }
        if(card.rank() == Rank.JACK) {
            return switch(card.suit()) {
                case CLUBS -> 9;
                case SPADES -> 8;
                case HEARTS -> 7;
                case DIAMONDS -> 6;
            };
        }
        return card.suit() == Suit.DIAMONDS ? suitPower(card) : -1;
    }

    public static int suitPower(Card card) {
        return switch(card.rank()) {
            case ACE -> 5;
            case TEN -> 4;
            case KING -> 3;
            case NINE -> 2;
            case EIGHT -> 1;
            case SEVEN -> 0;
            default -> throw new IllegalArgumentException("invalid card rank");
        };
    }
}
