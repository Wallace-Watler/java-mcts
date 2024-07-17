package com.github.wallacewatler.javamcts.hidden;

import com.github.wallacewatler.javamcts.DeterministicAction;
import com.github.wallacewatler.javamcts.VisibleState;

/**
 * A {@link TranspositionTable} that doesn't do anything. Useful for code that requires a table but shouldn't use it.
 */
public final class DummyTable<STATE extends VisibleState<STATE, ACTION>, ACTION extends DeterministicAction<STATE>> implements TranspositionTable<STATE, ACTION> {
    @Override
    public boolean contains(STATE state) {
        return false;
    }

    @Override
    public StateNode<STATE, ACTION> get(STATE state) {
        return null;
    }

    @Override
    public void put(STATE state, StateNode<STATE, ACTION> node) {}

    @Override
    public int size() {
        return 0;
    }
}
