package com.github.wallacewatler.javamcts;

import java.util.List;
import java.util.Random;

/**
 * An information set that represents a player's knowledge of a game state. Players are not aware of other players'
 * information sets.
 * <p>
 * On a conceptual level, information sets are analogous to states. As moves are observed during a game, information
 * sets are updated with whatever knowledge can be gained from them, similar to how states are updated by actions.
 *
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @param <STATE> the type of state this information set models knowledge of
 * @param <ACTION> the type of actions done by the owner of this information set
 *
 * @see State
 * @see Move
 */
public interface InfoSet<STATE, ACTION> {
    /**
     * @return The player that this information set belongs to.
     */
    int owner();

    /**
     * Generate and return a randomized state that could possibly be the true game state based on the knowledge in this
     * information set. {@code rand} should be used to generate the state. Mutable parts of the returned state object
     * must be independent of this information set object, and this information set should not be mutated by this
     * method.
     *
     * @param rand a source of randomness
     *
     * @return A random state that is consistent with this information set.
     */
    STATE determinize(Random rand);

    /**
     * @return A list containing the valid actions for the owner of this information set.
     */
    List<ACTION> validActions();
}
