package com.github.wallacewatler.javamcts.hidden;

import com.github.wallacewatler.javamcts.DeterministicAction;
import com.github.wallacewatler.javamcts.VisibleState;

import java.util.HashMap;

/**
 * An actual {@link TranspositionTable} (as opposed to a {@link DummyTable}).
 */
public final class RealTable<STATE extends VisibleState<STATE, ACTION>, ACTION extends DeterministicAction<STATE>> implements TranspositionTable<STATE, ACTION> {
    private final HashMap<STATE, StateNode<STATE, ACTION>> table = new HashMap<>();

    @Override
    public boolean contains(STATE state) {
        return table.containsKey(state);
    }

    @Override
    public StateNode<STATE, ACTION> get(STATE state) {
        return table.get(state);
    }

    @Override
    public void put(STATE state, StateNode<STATE, ACTION> node) {
        table.put(state, node);
    }

    @Override
    public int size() {
        return table.size();
    }

    @Override
    public String toString() {
        return "size = " + table.size();
    }
}
