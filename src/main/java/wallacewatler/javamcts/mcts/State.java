package wallacewatler.javamcts.mcts;

import java.util.List;

/**
 * A state contains all the data pertaining to a game at a certain point in time. It also defines actions that are
 * available to players, the game end criteria, and the scoring.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @param <SELF> the type of this state
 * @param <ACTION> the type of action this state defines
 *
 * @see Action
 */
public interface State<SELF extends State<SELF, ACTION>, ACTION> {
    /**
     * @return An integer representing the player who needs to do an action in this state.
     */
    int playerAboutToMove();

    /**
     * Defines the valid actions for the player about to move. The list must have at least one element unless this state
     * is terminal. Based on domain knowledge of the game, implementations can omit some valid actions; doing so may
     * improve search quality as time will not be wasted on actions that are obviously poor.
     *
     * @return A list containing the available actions for the player about to move.
     */
    List<ACTION> availableActions();

    /**
     * @return True if the game has ended.
     */
    boolean isTerminal();

    /**
     * This method only requires a definition if this state is terminal; otherwise, it may throw
     * {@link IllegalStateException}.
     *
     * @return An array containing the score that each player receives.
     *
     * @throws IllegalStateException if this state is not terminal
     */
    double[] scores();

    /**
     * Copy this state object into a new object. Any data that changes over the course of a game must be independent of
     * each other between the original and the copy. Immutable data may be shared.
     *
     * @return A copy of this state object.
     */
    SELF copy();
}
