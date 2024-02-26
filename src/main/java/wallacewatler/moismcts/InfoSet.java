package wallacewatler.moismcts;

import java.util.List;
import java.util.Random;

/**
 * An information set that represents a player's knowledge of a game state. Players are not aware of other players'
 * information sets.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @param <STATE> the type of state this information set models knowledge of
 * @param <ACTION> the type of move
 *
 * @see State
 * @see Move
 */
public interface InfoSet<STATE, ACTION> {
    /**
     * @return The ID of the player that this information set belongs to. The ID must be a unique integer among all
     * players in the game.
     */
    int playerId();

    /**
     * @param rand a source of randomness
     * @return A random state that is consistent with this information set.
     */
    STATE determinize(Random rand);

    /**
     * This method can optionally be overridden to remove some moves from consideration based on domain knowledge of
     * the game. Doing so may improve search quality as time will not be wasted on moves that are obviously not
     * rewarding.
     * <p>
     * Moves must not be added to {@code availableActions}, and at least one move must remain. All moves in
     * {@code availableActions} map one-to-one to actions, so {@link Move#asAction()} is valid for each of them.
     *
     * @param availableActions the moves that are available to the player with this information set
     */
    default void removePoorActions(List<ACTION> availableActions) {}
}
