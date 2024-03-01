package wallacewatler.javamcts.moismcts;

import java.util.Random;

/**
 * Actions are chosen by players and applied to states to progress the game. Actions are observed by players as moves.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @param <STATE> the type of state this action applies to
 * @param <MOVE> the type of move this action produces when observed by a player
 *
 * @see State
 * @see Move
 */
public interface Action<STATE, MOVE> {
    /**
     * Apply this action to a state. The given state may be mutated to compute the resulting state. If {@code state} is
     * an object, it is recommended that {@code state} is mutated and returned as it will likely run faster than
     * creating a new object.
     *
     * @param state the state to apply this action to
     * @param rand a source of randomness
     *
     * @return The resulting state.
     */
    STATE applyToState(STATE state, Random rand);

    /**
     * Observe this action from a player's point of view. If the point of view is that of the player performing this
     * action, the returned move must represent full knowledge of this action.
     *
     * @param observer the player observing this action
     *
     * @return The observed move.
     */
    MOVE observe(int observer);
}
