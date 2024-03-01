package wallacewatler.javamcts.sheepshead;

import wallacewatler.javamcts.moismcts.Action;
import wallacewatler.javamcts.moismcts.Move;

import java.util.Random;

public record PlayCard(Card card) implements Action<GameState, PlayCard>, Move<PlayCard, InfoSet> {
    @Override
    public GameState applyToState(GameState state, Random rand) {
        final Player player = state.players[state.playerAboutToMove];
        player.hand.remove(card);

        state.trickOnTable.layCard(state.playerAboutToMove, card);
        if(state.trickOnTable.isComplete()) {
            final int winner = state.trickOnTable.winner();
            state.players[winner].takenTricks.add(state.trickOnTable);
            state.trickOnTable = new Trick();
            state.playerAboutToMove = winner;
        } else {
            state.playerAboutToMove = (state.playerAboutToMove + 1) % 4;
        }
        return state;
    }

    @Override
    public PlayCard observe(int observer) {
        return this;
    }

    @Override
    public InfoSet applyToInfoSet(InfoSet infoSet) {
        final PlayerInfo player = infoSet.playerInfos[infoSet.playerAboutToMove];
        player.handSize--;
        player.confirmedInHand.remove(card);
        infoSet.cardPlayed(card);

        if(!infoSet.trickOnTable.isEmpty()) {
            final Card ledCard = infoSet.trickOnTable.ledCard();
            assert ledCard != null;
            if(Sheepshead.isTrump(ledCard) && !Sheepshead.isTrump(card)) {
                infoSet.cannotHave(player, Sheepshead::isTrump);
            } else if(!Sheepshead.isTrump(ledCard) && !Sheepshead.isFail(card, ledCard.suit())) {
                infoSet.cannotHave(player, c -> Sheepshead.isFail(c, ledCard.suit()));
            }
        }

        infoSet.trickOnTable.layCard(infoSet.playerAboutToMove, card);
        if(infoSet.trickOnTable.isComplete()) {
            final int winner = infoSet.trickOnTable.winner();
            infoSet.playerInfos[winner].takenTricks.add(infoSet.trickOnTable);
            infoSet.trickOnTable = new Trick();
            infoSet.playerAboutToMove = winner;
        } else {
            infoSet.playerAboutToMove = (infoSet.playerAboutToMove + 1) % 4;
        }

        return infoSet;
    }

    @Override
    public PlayCard asAction() {
        return this;
    }
}
