package wallacewatler.moismcts;

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
 */
public interface Move<ACTION, INFO_SET> {
    /**
     * Apply this move to an information set. This move may or may not mutate {@code infoSet} to compute the resulting
     * information set. If the information set is an object, it is recommended that this method mutates {@code infoSet}
     * and returns it as that will likely run faster than constructing a new object.
     *
     * @param infoSet the information set to apply this move to
     *
     * @return The resulting information set.
     */
    INFO_SET applyToInfoSet(INFO_SET infoSet);

    /**
     * This method is only defined for singleton moves, i.e. moves that correspond to only one action.
     *
     * @return The corresponding action for this move.
     *
     * @throws UnsupportedOperationException if this move is not a singleton
     */
    ACTION asAction();
}
