package com.github.wallacewatler.javamcts.hidden;

import com.github.wallacewatler.javamcts.DeterministicAction;
import com.github.wallacewatler.javamcts.VisibleState;

public interface TranspositionTable<STATE extends VisibleState<STATE, ACTION>, ACTION extends DeterministicAction<STATE>> {
    boolean contains(STATE state);
    StateNode<STATE, ACTION> get(STATE state);
    void put(STATE state, StateNode<STATE, ACTION> node);
    int size();
}
