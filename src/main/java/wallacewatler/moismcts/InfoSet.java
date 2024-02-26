package wallacewatler.moismcts;

import java.util.List;
import java.util.Random;

/**
 * An information set that represents a player's knowledge of a game state. Players are not aware of other players'
 * information sets.
 *
 * @since 0.1
 *
 * @see State
 * @see Move
 * @see MOISMCTS
 *
 * @author Wallace Watler
 *
 * @param <STATE> the type of state this information set models knowledge of
 * @param <MOVE> the type of move
 */
public interface InfoSet<STATE, MOVE> {
    /**
     * Create a random state that is consistent with this information set.
     * @param rand a source of randomness
     * @return A random state consistent with this information set.
     */
    STATE determinize(Random rand);

    /**
     * This method can optionally be overridden to remove some available moves from further consideration based on
     * domain knowledge of the game. Doing so can improve search quality as iterations will not be wasted on moves that
     * are obviously not rewarding.
     * <p>
     * Moves must not be added to {@code availableMoves}, and at least one move must remain. All moves in
     * {@code availableMoves} map one-to-one to actions, so {@link Move#asAction()} is valid for each of them.
     *
     * @param availableMoves the moves that are available to the player with this info set
     */
    default void removePoorMoves(List<MOVE> availableMoves) {}
}
