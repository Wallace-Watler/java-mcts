package io.github.wallacewatler.javamcts;

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
 * @param <ACTION> the type of actions done by the owner of this information set
 *
 * @see State
 * @see Action
 */
public interface InfoSet<STATE, ACTION> {
    /**
     * @return The player that this information set belongs to.
     */
    int owner();

    /**
     * Generate and return a randomized state that could possibly be the true game state based on the knowledge in this
     * information set. {@code rand} should be used to generate the state. The returned state must not be the same
     * object as this information set, and this information set should not be mutated by this method.
     *
     * @param rand a source of randomness
     *
     * @return A random state that is consistent with this information set.
     */
    STATE determinize(Random rand);

    // TODO: Maybe just have InfoSet define considerableActions()
    /**
     * Remove actions from {@code validActions} that are obviously not rewarding. The default behavior is to keep all
     * valid actions. Actions must not be added to {@code validActions}, and at least one action must remain.
     *
     * @param validActions the actions that are available to the owner of this information set
     *
     * @implNote This method can be overridden to remove some actions from consideration based on domain knowledge of
     * the game. Doing so may improve search quality as time will not be wasted on actions that are obviously poor.
     */
    default void removePoorActions(List<? extends ACTION> validActions) {}
}
