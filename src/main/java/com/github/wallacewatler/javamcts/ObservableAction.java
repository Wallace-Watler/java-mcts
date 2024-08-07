package com.github.wallacewatler.javamcts;

import java.util.Random;

/**
 * Actions are chosen by players and applied to states to progress the game. {@code ObservableAction} represents an
 * action in which randomness may be involved and aspects of the action may be partially or entirely hidden from other
 * players. It is possible that two different actions are indistinguishable from a certain player's point of view, so
 * actions are said to be observed by players as moves.
 * <p>
 * Implementations of {@code ObservableAction} should include {@link Object#equals} and {@link Object#hashCode} since
 * actions are internally used as keys in hashmaps.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @param <STATE> the type of state this action applies to
 * @param <MOVE> the type of move this action appears as when observed by a player
 *
 * @see State
 * @see Move
 */
public interface ObservableAction<STATE, MOVE> {
    /**
     * Compute and return the result of applying this action to {@code state}. {@code rand} should be used to compute
     * stochastic game mechanics. {@code state} may be mutated by this method. If {@code state} is an object, it is
     * recommended that {@code state} is mutated into the resulting state and returned as it will likely run faster than
     * creating a new object.
     *
     * @param state the state to apply this action to
     * @param rand a source of randomness
     *
     * @return The result of applying this action to a state.
     */
    STATE applyToState(STATE state, Random rand);

    /**
     * @param observer the player observing this action
     *
     * @return A move representing this action as observed from {@code observer}'s point of view. If {@code observer} is
     * the player performing this action, the returned move must represent full knowledge of this action (i.e. the move
     * must be a singleton).
     */
    MOVE observe(int observer);
}
