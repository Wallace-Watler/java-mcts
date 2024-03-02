package io.github.wallacewatler.javamcts;

import java.util.List;

/**
 * A {@code VisibleState} is a state that is fully visible to all players, or a state in which any hidden information is
 * hidden from all players and can thus be treated as a stochastic event when it is revealed.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @param <SELF> the type of this state
 * @param <ACTION> the type of actions taken in this state
 *
 * @see State
 * @see Action
 */
public interface VisibleState<SELF extends VisibleState<SELF, ACTION>, ACTION> extends State<ACTION> {
    /**
     * @return A list containing all the valid actions for the active player. The game has ended if and only if there
     * are no valid actions.
     *
     * @apiNote A state in which the game has ended is not to be confused with a terminal state, which is merely when
     * the outcome of the game is certain regardless of future actions. It is possible for actions to be available in a
     * terminal state.
     *
     * @implNote Based on domain knowledge of the game, implementations can omit some valid actions; doing so may
     * improve search quality as time will not be wasted on actions that are obviously poor.
     */
    List<ACTION> validActions();

    /**
     * @return A deep copy of this state object. Any data that changes over the course of a game must be independent of
     * each other between the original and the copy. Immutable data may be shared.
     */
    SELF copy();
}
