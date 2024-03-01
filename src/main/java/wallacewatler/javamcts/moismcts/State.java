package wallacewatler.javamcts.moismcts;

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
 * @param <ACTION> the type of action this state defines
 */
public interface State<ACTION> {
    /**
     * @return An integer representing the player who needs to do an action in this state.
     */
    int playerAboutToMove();

    /**
     * Defines the valid actions for the player about to move. The list must have at least one element unless this state
     * is terminal.
     *
     * @return A list containing the available actions for the player about to move.
     */
    List<ACTION> availableActions();

    /**
     * @return True if the game has ended.
     */
    boolean isTerminal();

    /**
     * This method only requires a definition if this state is terminal.
     *
     * @return An array containing the score that each player receives.
     */
    double[] scores();
}
