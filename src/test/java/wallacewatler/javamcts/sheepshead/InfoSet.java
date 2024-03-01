package wallacewatler.javamcts.sheepshead;

import java.util.*;
import java.util.function.Predicate;

public final class InfoSet implements wallacewatler.javamcts.moismcts.InfoSet<GameState, PlayCard> {
    public final int pov;
    public int playerAboutToMove;
    public final PlayerInfo[] playerInfos = new PlayerInfo[4];
    public final ArrayList<Card> possiblyInBlind;
    public final ArrayList<Card> confirmedInBlind;
    public Trick trickOnTable;

    public InfoSet(GameState initialState, int pov) {
        this.pov = pov;
        playerAboutToMove = initialState.playerAboutToMove;
        for(int i = 0; i < 4; i++) {
            final PlayerInfo playerInfo = new PlayerInfo();
            playerInfos[i] = playerInfo;
            if(i == pov) {
                playerInfo.confirmedInHand.addAll(initialState.players[pov].hand);
                playerInfo.possiblyInHand.clear();
            } else {
                playerInfo.possiblyInHand.removeAll(initialState.players[pov].hand);
            }
        }
        possiblyInBlind = new ArrayList<>(Sheepshead.newDeck());
        possiblyInBlind.removeAll(initialState.players[pov].hand);
        confirmedInBlind = new ArrayList<>(2);
        trickOnTable = new Trick(initialState.trickOnTable);
    }

    public InfoSet(InfoSet infoSet) {
        pov = infoSet.pov;
        playerAboutToMove = infoSet.playerAboutToMove;
        playerInfos[0] = new PlayerInfo(infoSet.playerInfos[0]);
        playerInfos[1] = new PlayerInfo(infoSet.playerInfos[1]);
        playerInfos[2] = new PlayerInfo(infoSet.playerInfos[2]);
        playerInfos[3] = new PlayerInfo(infoSet.playerInfos[3]);
        possiblyInBlind = new ArrayList<>(infoSet.possiblyInBlind);
        confirmedInBlind = new ArrayList<>(infoSet.confirmedInBlind);
        trickOnTable = new Trick(infoSet.trickOnTable);
    }

    private void checkIfCardsConfirmed(Collection<Card> cards) {
        final ArrayList<Card> toConfirm = new ArrayList<>(cards);
        toNextCard: while(!toConfirm.isEmpty()) {
            final Card card = toConfirm.remove(toConfirm.size() - 1);

            // Check all possible locations of the card
            int locationFlags = possiblyInBlind.contains(card) ? 1 : 0;
            for(PlayerInfo playerInfo : playerInfos)
                locationFlags = (locationFlags << 1) | (playerInfo.possiblyInHand.contains(card) ? 1 : 0);

            // If only possible in the blind
            if(locationFlags == 0b10000) {
                confirmedInBlind.add(card);
                possiblyInBlind.remove(card);
                if(confirmedInBlind.size() == 2) {
                    toConfirm.addAll(possiblyInBlind);
                    possiblyInBlind.clear();
                }
                continue toNextCard;
            }

            // If only possible in one player's hand
            for(int i = 0; i < 4; i++) {
                if(locationFlags == 1 << (3 - i)) {
                    playerInfos[i].confirmedInHand.add(card);
                    playerInfos[i].possiblyInHand.remove(card);
                    if(playerInfos[i].confirmedInHand.size() == playerInfos[i].handSize) {
                        toConfirm.addAll(playerInfos[i].possiblyInHand);
                        playerInfos[i].possiblyInHand.clear();
                    }
                    continue toNextCard;
                }
            }
        }
    }

    public void cannotHave(PlayerInfo player, Predicate<Card> predicate) {
        final List<Card> cards = player.possiblyInHand.stream().filter(predicate).toList();
        player.possiblyInHand.removeAll(cards);
        if(player.confirmedInHand.size() + player.possiblyInHand.size() == player.handSize) {
            player.confirmedInHand.addAll(player.possiblyInHand);
            removeCardsFromPossibleSets(player.possiblyInHand);
        }
        checkIfCardsConfirmed(cards);
    }

    public void cardPlayed(Card card) {
        removeCardsFromPossibleSets(Collections.singleton(card));
    }

    private void removeCardsFromPossibleSets(Collection<Card> cards) {
        final ArrayList<Card> toRemove = new ArrayList<>(cards);
        while(!toRemove.isEmpty()) {
            final Card card = toRemove.remove(toRemove.size() - 1);
            if(possiblyInBlind.remove(card) && confirmedInBlind.size() + possiblyInBlind.size() == 2) {
                confirmedInBlind.addAll(possiblyInBlind);
                toRemove.addAll(possiblyInBlind);
                possiblyInBlind.clear();
            }
            for(PlayerInfo player : playerInfos) {
                if(player.possiblyInHand.remove(card) && player.confirmedInHand.size() + player.possiblyInHand.size() == player.handSize) {
                    player.confirmedInHand.addAll(player.possiblyInHand);
                    toRemove.addAll(player.possiblyInHand);
                    player.possiblyInHand.clear();
                }
            }
        }
    }

    @Override
    public int playerNumber() {
        return pov;
    }

    @Override
    public GameState determinize(Random rand) {
        if(pov != playerAboutToMove)
            throw new IllegalStateException("should not be determinizing state when not about to move");

        final InfoSet copy = new InfoSet(this);

        final GameState state = new GameState();
        state.playerAboutToMove = copy.playerAboutToMove;
        for(int i = 0; i < 4; i++) {
            final PlayerInfo playerInfo = copy.playerInfos[i];
            final List<Card> randomPossibles = randomSublist(playerInfo.possiblyInHand, playerInfo.handSize - playerInfo.confirmedInHand.size(), rand);
            playerInfo.confirmedInHand.addAll(randomPossibles);
            copy.removeCardsFromPossibleSets(playerInfo.possiblyInHand);

            state.players[i] = new Player(playerInfo.confirmedInHand);

            for(Trick trick : copy.playerInfos[i].takenTricks)
                state.players[i].takenTricks.add(new Trick(trick));
        }
        assert copy.possiblyInBlind.size() == 0;
        assert copy.confirmedInBlind.size() == 2;
        state.blind[0] = copy.confirmedInBlind.get(0);
        state.blind[1] = copy.confirmedInBlind.get(1);
        state.trickOnTable = new Trick(copy.trickOnTable);
        return state;
    }

    private static <T> List<T> randomSublist(List<T> list, int n, Random rand) {
        final ArrayList<T> arr = new ArrayList<>(list);
        Collections.shuffle(arr, rand);
        return arr.subList(0, n);
    }
}
