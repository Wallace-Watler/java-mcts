package io.github.wallacewatler.javamcts;

/**
 * Moves are actions as seen from the viewpoint of a specific player. Moves are applied to information sets to update a
 * player's knowledge of the game state. Since other players' actions may be partially or completely hidden, it is
 * possible for multiple different actions to map to the same move. A player will always have full view of their own
 * actions, so the corresponding moves map one-to-one and are said to be singletons.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @param <ACTION> the type of action this move models knowledge of
 * @param <INFO_SET> the type of information set this move applies to
 *
 * @see Action
 * @see InfoSet
 * @see State
 */
public interface Move<ACTION, INFO_SET> {
    /**
     * Compute and return the result of applying this move to {@code infoSet}. {@code infoSet} may be mutated by this
     * method.
     *
     * @param infoSet the information set to apply this move to
     *
     * @return The result of applying this move to {@code infoSet}.
     *
     * @implNote If {@code infoSet} is an object, it is recommended that {@code infoSet} is mutated into the resulting
     * information set and returned as it will likely run faster than creating a new object.
     */
    INFO_SET applyToInfoSet(INFO_SET infoSet);

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
