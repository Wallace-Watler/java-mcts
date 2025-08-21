package com.github.wallacewatler.javamcts;

/**
 * Actions are chosen by players and applied to states to progress the game. A {@code DeterministicAction} does not
 * involve any randomness and is fully visible to everyone, so its outcome is completely predictable.
 * <p>
 * Implementations of {@code DeterministicAction} should include {@link Object#equals} and {@link Object#hashCode} since
 * actions are internally used as keys in hashmaps.
 *
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @param <STATE> the type of state this action applies to
 */
public interface DeterministicAction<STATE> {
    /**
     * Compute and return the result of applying this action to {@code state}. {@code state} may be mutated by this
     * method. If {@code state} is an object, it is recommended that {@code state} is mutated into the resulting state
     * and returned as it will likely run faster than creating a new object.
     *
     * @param state the state to apply this action to
     *
     * @return The result of applying this action to a state.
     */
    STATE applyToState(STATE state);
}
