package wallacewatler.javamcts.mcts;

/**
 * Actions are chosen by players and applied to states to progress the game.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @param <STATE> the type of state this action applies to
 *
 * @see State
 */
public interface Action<STATE> {
    /**
     * Apply this action to a state. The given state may be mutated to compute the resulting state. If {@code state} is
     * an object, it is recommended that {@code state} is mutated and returned as it will likely run faster than
     * creating a new object.
     *
     * @param state the state to apply this action to
     *
     * @return The resulting state.
     */
    STATE applyToState(STATE state);
}
