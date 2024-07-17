package com.github.wallacewatler.javamcts;

import com.github.wallacewatler.javamcts.hidden.ActionSeqNode;
import com.github.wallacewatler.javamcts.hidden.Procedures;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Open-loop MCTS with tree-parallelization. For details on how to use this class, see {@link OLMCTS}.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @see OLMCTS
 * @see OLMCTSRP
 */
public final class OLMCTSTP implements OLMCTS, Cloneable {
    @Override
    public
    <STATE extends VisibleState<STATE, ACTION>, ACTION extends StochasticAction<STATE>>
    SearchResults<ACTION> search(int numPlayers, STATE rootState, SearchParameters params, Random rand) {
        if(numPlayers < 1)
            throw new IllegalArgumentException("numPlayers must be at least 1");

        if(rootState.validActions().isEmpty())
            throw new IllegalArgumentException("rootState has no valid actions");

        // These are shared across threads
        final long start = System.currentTimeMillis();
        final ActionSeqNode rootNode = new ActionSeqNode(numPlayers);
        final AtomicInteger iters = new AtomicInteger();
        final Semaphore iterAllowance = new Semaphore(params.maxIters());
        // -------------------------------

        // Start parallel searches.
        final Thread[] workers = new Thread[params.threadCount()];
        for(int workerNum = 0; workerNum < workers.length; workerNum++) {
            workers[workerNum] = new Thread(() -> {
                long now = System.currentTimeMillis();
                while(!Thread.interrupted() && now - start <= params.maxTime() && (now - start < params.minTime() || iterAllowance.tryAcquire())) {
                    Procedures.iterOLMCTS(rootState, rootNode, params.uct(), rand);
                    iters.getAndIncrement();
                    now = System.currentTimeMillis();
                }
            }, "olmctstp" + workerNum);
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

        final ACTION bestAction = Procedures.mostVisited(rootNode, rootState.validActions(), rand);
        final double itersPerThread = (double) iters.get() / params.threadCount();
        return new SearchResults<>(bestAction, itersPerThread, System.currentTimeMillis() - start, rootNode.numNodes(), 0);
    }

    @Override
    public String toString() {
        return "OLMCTS-TP";
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
