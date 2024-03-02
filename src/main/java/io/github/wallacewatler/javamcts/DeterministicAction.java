package io.github.wallacewatler.javamcts;

import java.util.Random;

/**
 * Actions are chosen by players and applied to states to progress the game.<br>
 * <br>
 * A {@code DeterministicAction} has a pre-determined effect on a game state. It is a specialized type of
 * {@link StochasticAction} in which there is only one possible outcome.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @param <STATE> the type of state this action applies to
 *
 * @see Action
 * @see State
 */
public interface DeterministicAction<STATE> extends StochasticAction<STATE> {
    /**
     * Compute and return the result of applying this action to {@code state}. {@code state} may be mutated by this
     * method.
     *
     * @param state the state to apply this action to
     *
     * @return The result of applying this action to a state.
     *
     * @implNote If {@code state} is an object, it is recommended that {@code state} is mutated into the resulting state
     * and returned as it will likely run faster than creating a new object.
     */
    STATE applyToState(STATE state);

    /**
     * Forwards the call to {@link DeterministicAction#applyToState(STATE)}. This method should not be overridden.
     */
    @Override
    default STATE applyToState(STATE state, Random rand) {
        return applyToState(state);
    }
}
