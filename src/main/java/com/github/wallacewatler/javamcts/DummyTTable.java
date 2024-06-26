package com.github.wallacewatler.javamcts;

final class DummyTTable<STATE, NODE> implements TranspositionTable<STATE, NODE> {
    @Override
    public boolean contains(STATE state) {
        return false;
    }

    @Override
    public NODE get(STATE state) {
        return null;
    }

    @Override
    public void put(STATE state, NODE node) {}

    @Override
    public int size() {
        return 0;
    }
}
