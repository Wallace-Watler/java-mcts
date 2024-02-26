package wallacewatler.sheepshead;

public enum Suit {
    CLUBS("♣"),
    DIAMONDS("♦"),
    HEARTS("♥"),
    SPADES("♠");

    public final String symbol;

    Suit(String symbol) {
        this.symbol = symbol;
    }
}
