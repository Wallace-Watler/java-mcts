package com.github.wallacewatler.javamcts;

import java.util.List;

/**
 * A game state that is fully visible to all players.
 * <p>
 * A {@code VisibleState} does not contain any information that is hidden from players. Games containing information
 * that <i>is</i> hidden from <i>all</i> players (e.g. solitaire, in the form of face-down cards) can still be modeled
 * using {@code VisibleState} by treating the reveal of hidden information as a random event (use {@link OLMCTS} for
 * that). This trick does not work if the information is only hidden from some players but not others (see
 * {@link MOISMCTS} for handling that category of games).
 *
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @param <SELF> the type of this state
 * @param <ACTION> the type of actions taken in this state
 *
 * @see State
 */
public interface VisibleState<SELF extends VisibleState<SELF, ACTION>, ACTION> extends State<ACTION> {
    /**
     * @implNote Based on domain knowledge of the game, implementations can omit actions that are obviously poor; doing
     * so may improve search quality as time will not be wasted on those actions.
     */
    List<ACTION> validActions();

    /**
     * @return A copy of this state object. Any data that changes over the course of a game must be deep-copied; other
     * data may be shared between the original and the copy.
     */
    SELF copy();
}
