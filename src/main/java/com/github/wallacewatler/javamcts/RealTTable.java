package com.github.wallacewatler.javamcts;

import java.util.HashMap;

final class RealTTable<STATE, NODE> implements TranspositionTable<STATE, NODE> {
    private final HashMap<STATE, NODE> table = new HashMap<>();

    @Override
    public boolean contains(STATE state) {
        return table.containsKey(state);
    }

    @Override
    public NODE get(STATE state) {
        return table.get(state);
    }

    @Override
    public void put(STATE state, NODE node) {
        table.put(state, node);
    }

    @Override
    public int size() {
        return table.size();
    }
}
