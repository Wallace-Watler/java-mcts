package com.github.wallacewatler.javamcts;

interface TranspositionTable<STATE, NODE> {
    boolean contains(STATE state);
    NODE get(STATE state);
    void put(STATE state, NODE node);
    int size();
}
