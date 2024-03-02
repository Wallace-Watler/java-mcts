package io.github.wallacewatler.javamcts.sheepshead;

import java.util.ArrayList;

public final class PlayerInfo {
    public int handSize = 7;
    public final ArrayList<Card> possiblyInHand;
    public final ArrayList<Card> confirmedInHand;
    public final ArrayList<Trick> takenTricks;

    public PlayerInfo() {
        possiblyInHand = new ArrayList<>(Sheepshead.newDeck());
        confirmedInHand = new ArrayList<>(7);
        takenTricks = new ArrayList<>(7);
    }

    public PlayerInfo(PlayerInfo playerInfo) {
        handSize = playerInfo.handSize;
        possiblyInHand = new ArrayList<>(playerInfo.possiblyInHand);
        confirmedInHand = new ArrayList<>(playerInfo.confirmedInHand);
        takenTricks = new ArrayList<>(playerInfo.takenTricks.size());
        for(Trick trick : playerInfo.takenTricks)
            takenTricks.add(new Trick(trick));
    }

    @Override
    public String toString() {
        return "PlayerInfo{" +
                "handSize=" + handSize +
                ", possiblyInHand=" + possiblyInHand +
                ", confirmedInHand=" + confirmedInHand +
                ", takenTricks=" + takenTricks +
                '}';
    }
}
