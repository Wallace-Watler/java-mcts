package io.github.wallacewatler.javamcts;

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
 * @param <ACTION> the type of actions taken in this state
 *
 * @see VisibleState
 */
public interface State<ACTION> {
    /**
     * @return An integer representing the player who needs to do an action in this state. Players are numbered from 0
     * to <i>n</i> - 1, where <i>n</i> is the number of players in the game.
     */
    int activePlayer();

    /**
     * @return A list containing all the valid actions for the active player. The game has ended if and only if there
     * are no valid actions.
     *
     * @apiNote A state in which the game has ended is not to be confused with a terminal state, which is merely when
     * the outcome of the game is certain regardless of future actions. It is possible for actions to be available in a
     * terminal state.
     */
    List<ACTION> validActions();

    /**
     * @return The score that each player receives for this terminal state, or null if this state is not terminal.
     *
     * @apiNote A state is terminal when the scores for each player can be determined without further actions taking
     * place. This typically occurs at the end of the game, but in some cases the winner can be decided before the game
     * is over. It is recommended that a state returns the scores in these cases so that the search time is used more
     * efficiently.
     */
    double[] scores();
}
