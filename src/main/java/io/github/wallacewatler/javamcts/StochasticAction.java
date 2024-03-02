package io.github.wallacewatler.javamcts;

/**
 * Actions are chosen by players and applied to states to progress the game.<br>
 * <br>
 * A {@code StochasticAction} may involve randomness and/or non-discrete data and, when applied to a state, leads to one
 * of possibly many states. It is a specialized type of {@link Action} in which nothing is hidden, so all players
 * observe a {@code StochasticAction} as itself.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @param <STATE> the type of state this action applies to
 *
 * @see DeterministicAction
 * @see State
 */
public interface StochasticAction<STATE> extends Action<STATE, StochasticAction<STATE>> {
    /**
     * @return This {@code StochasticAction}. This implementation should not be overridden.
     */
    @Override
    default StochasticAction<STATE> observe(int observer) {
        return this;
    }
}
