package com.github.wallacewatler.javamcts;

/**
 * <p>
 *     Moves are actions as seen from the viewpoint of a specific player. Since other players' actions may be partially
 *     or completely hidden, it is possible for multiple different actions to map to the same move. A player will always
 *     have full view of their own actions, so the corresponding moves map one-to-one and are said to be singletons.
 * </p>
 * <p>
 *     Implementations of {@code Move} should include {@link Object#equals} and {@link Object#hashCode} since moves are
 *     internally used as keys in hashmaps.
 * </p>
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @param <ACTION> the type of action this move models knowledge of
 */
public interface Move<ACTION> {
    /**
     * Return the one action in this singleton move. This method should only be defined for singleton moves, i.e. moves
     * that correspond to only one action.
     *
     * @return The corresponding action for this singleton move.
     *
     * @throws UnsupportedOperationException if this move is not a singleton
     */
    ACTION asAction();
}
