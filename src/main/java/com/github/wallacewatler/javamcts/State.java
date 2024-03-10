package com.github.wallacewatler.javamcts;

import java.util.List;

/**
 * A {@code State} represents a game at a certain point in time. It stores all persistent data, defines the actions
 * that can be done by the player about to act, and defines the score that each player receives if the state is
 * terminal. A state is terminal when the scores for each player can be determined without further actions taking
 * place. This typically occurs at the end of the game, but in some cases the winner can be decided before the game
 * is over.
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
     */
    List<ACTION> validActions();

    /**
     * @return The score that each player receives for this terminal state, or {@code null} if this state is not
     * terminal.
     */
    double[] scores();
}
