package wallacewatler.moismcts;

import java.util.Random;

/**
 * Actions are chosen by players and applied to states to progress the game. Actions are observed by players as moves,
 * where a move represents a player's knowledge of an action.
 *
 * @since 0.1
 *
 * @see State
 * @see Move
 * @see MOISMCTS
 *
 * @author Wallace Watler
 *
 * @param <STATE> the type of state this action applies to
 * @param <MOVE> the type of move this action produces when observed by a player
 */
public interface Action<STATE, MOVE> {
    /**
     * Apply this action to a state. This action may or may not mutate {@code state} to compute the resulting state. It
     * is recommended, however, that this method mutates {@code state} and returns it as that will likely run faster
     * than constructing a new state object.
     *
     * @param state the state to apply this action to
     * @param rand a source of randomness
     *
     * @return The resulting state.
     */
    STATE applyToState(STATE state, Random rand);

    /**
     * Observe this action from a player's point of view. If the point of view is that of the player performing this
     * action, the returned move should represent full knowledge of this action.
     *
     * @param observer the player observing this action
     *
     * @return The observed move.
     */
    MOVE observe(int observer);
}
