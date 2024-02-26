package wallacewatler.moismcts;

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
     * @return The valid actions for the player about to move. The list must have at least one element unless this state
     * is terminal.
     */
    List<ACTION> availableActions();

    /**
     * @return True if the game has ended.
     */
    boolean isTerminal();

    /**
     * @return An array containing the score that each player receives. This need only be defined if this state is
     * terminal.
     */
    double[] rewards();
}
