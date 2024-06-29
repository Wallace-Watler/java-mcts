package com.github.wallacewatler.javamcts;

import java.util.Random;

/**
 * Actions are chosen by players and applied to states to progress the game. A {@code StochasticAction} may involve
 * randomness.
 * <p>
 * Implementations of {@code StochasticAction} should include {@link Object#equals} and {@link Object#hashCode} since
 * actions are internally used as keys in hashmaps.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @param <STATE> the type of state this action applies to
 */
public interface StochasticAction<STATE> {
    /**
     * Compute and return the result of applying this action to {@code state}. {@code rand} should be used to compute
     * stochastic game mechanics. {@code state} may be mutated by this method. If {@code state} is an object, it is
     * recommended that {@code state} is mutated into the resulting state and returned as it will likely run faster than
     * creating a new object.
     *
     * @param state the state to apply this action to
     * @param rand a source of randomness
     *
     * @return The result of applying this action to a state.
     */
    STATE applyToState(STATE state, Random rand);
}
