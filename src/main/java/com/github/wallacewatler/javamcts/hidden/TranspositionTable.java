package com.github.wallacewatler.javamcts.hidden;

import com.github.wallacewatler.javamcts.DeterministicAction;
import com.github.wallacewatler.javamcts.VisibleState;

/**
 * Serves as a record of all states encountered during a tree search.
 * <p>
 * While searching a tree, two or more different action paths may lead to an identical state. A transposition table can
 * be used to collate stats for such states rather than keeping their stats separate. This may lead to better search
 * quality.
 *
 * @param <STATE> the type of states stored in this table
 * @param <ACTION> the type of actions linking states
 */
public interface TranspositionTable<STATE extends VisibleState<STATE, ACTION>, ACTION extends DeterministicAction<STATE>> {
    /**
     * @param state a game state
     *
     * @return True if this table contains {@code state}.
     */
    boolean contains(STATE state);

    /**
     * @param state a game state
     *
     * @return The {@link StateNode} that represents {@code state}.
     */
    StateNode<STATE, ACTION> get(STATE state);

    /**
     * Insert a state into this table.
     *
     * @param state the state to insert
     * @param node the corresponding {@link StateNode}
     */
    void put(STATE state, StateNode<STATE, ACTION> node);

    /**
     * @return The number of states in this table.
     */
    int size();
}
