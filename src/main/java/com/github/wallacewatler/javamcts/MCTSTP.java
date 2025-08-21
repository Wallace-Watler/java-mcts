package com.github.wallacewatler.javamcts;

import com.github.wallacewatler.javamcts.hidden.*;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Closed loop MCTS with tree parallelization. For details on how to use this class, see {@link MCTS}.
 *
 * @since 0.1.0
 *
 * @author Wallace Watler
 */
public final class MCTSTP implements MCTS, Cloneable {
    @Override
    public
    <STATE extends VisibleState<STATE, ACTION>, ACTION extends DeterministicAction<STATE>>
    SearchResults<ACTION> search(int numPlayers, STATE rootState, SearchParameters params, Random rand, boolean useTable) {
        if(numPlayers < 1)
            throw new IllegalArgumentException("numPlayers must be at least 1");

        // These are shared across threads
        final long start = System.currentTimeMillis();
        final AtomicInteger iters = new AtomicInteger();
        final Semaphore iterAllowance = new Semaphore(params.maxIters());
        final StateNode<STATE, ACTION> rootNode = new StateNode<>(numPlayers, rootState);
        final TranspositionTable<STATE, ACTION> table = useTable ? new RealTable<>() : new DummyTable<>();
        // -------------------------------

        if(rootNode.validActions().isEmpty())
            return new SearchResults<>(null, 0, 0, 1, 1);

        // Start parallel searches.
        final Thread[] workers = new Thread[params.threadCount()];
        for(int workerNum = 0; workerNum < workers.length; workerNum++) {
            workers[workerNum] = new Thread(() -> {
                long now = System.currentTimeMillis();
                while(!Thread.interrupted() && now - start <= params.maxTime() && (now - start < params.minTime() || iterAllowance.tryAcquire())) {
                    Procedures.iterMCTS(rootNode, params.uct(), rand, table);
                    iters.getAndIncrement();
                    now = System.currentTimeMillis();
                }
            }, "mctstp" + workerNum);
            workers[workerNum].start();
        }

        // Wait for all threads to finish
        try {
            for(Thread worker : workers)
                worker.join();
        } catch(InterruptedException ignored) {
            for(Thread worker : workers)
                worker.interrupt();
        }

        // Recommend the most selected action.
        final double itersPerThread = (double) iters.get() / params.threadCount();
        final ACTION bestAction = Procedures.mostVisited(rootNode, rootNode.validActions(), rand);
        return new SearchResults<>(bestAction, itersPerThread, System.currentTimeMillis() - start, rootNode.numNodes(), table.size());
    }

    @Override
    public String toString() {
        return "MCTS-TP";
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
