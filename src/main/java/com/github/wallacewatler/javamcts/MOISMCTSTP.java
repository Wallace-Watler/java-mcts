package com.github.wallacewatler.javamcts;

import com.github.wallacewatler.javamcts.hidden.MoveSeqNode;
import com.github.wallacewatler.javamcts.hidden.Procedures;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MO-ISMCTS with tree parallelization. For details on how to use this class, see {@link MOISMCTS}.
 *
 * @since 0.1.0
 *
 * @author Wallace Watler
 */
public final class MOISMCTSTP implements MOISMCTS, Cloneable {
    @Override
    public
    <STATE extends State<ACTION>, ACTION extends ObservableAction<STATE>>
    SearchResults<ACTION> search(int numPlayers, InfoSet<STATE, ACTION> infoSet, SearchParameters params, Random rand) {
        if(numPlayers < 1)
            throw new IllegalArgumentException("numPlayers must be at least 1");

        final List<ACTION> validActions = infoSet.validActions();
        if(validActions.isEmpty())
            return new SearchResults<>(null, 0, 0, 1, 1);

        // These are shared across threads
        final long start = System.currentTimeMillis();
        final AtomicInteger iters = new AtomicInteger();
        final Semaphore iterAllowance = new Semaphore(params.maxIters());
        // The root node for each player's tree
        final ArrayList<MoveSeqNode> rootNodes = new ArrayList<>(numPlayers);
        for(int i = 0; i < numPlayers; i++)
            rootNodes.add(new MoveSeqNode());
        // -------------------------------

        // Start parallel searches.
        final Thread[] workers = new Thread[params.threadCount()];
        for(int workerNum = 0; workerNum < workers.length; workerNum++) {
            workers[workerNum] = new Thread(() -> {
                long now = System.currentTimeMillis();
                while(!Thread.interrupted() && now - start <= params.maxTime() && (now - start < params.minTime() || iterAllowance.tryAcquire())) {
                    Procedures.iterMOISMCTS(infoSet, rootNodes, params.uct(), rand);
                    iters.getAndIncrement();
                    now = System.currentTimeMillis();
                }
            }, "moismctstp" + workerNum);
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

        // Recommend the most selected action. Ties are broken by randomness.
        final MoveSeqNode rootNode = rootNodes.get(infoSet.owner());
        final ACTION bestAction = Procedures.mostVisited(rootNode, validActions, rand);
        final double itersPerThread = (double) iters.get() / params.threadCount();
        return new SearchResults<>(bestAction, itersPerThread, System.currentTimeMillis() - start, rootNode.numNodes(), 0);
    }

    @Override
    public String toString() {
        return "MO-ISMCTS-TP";
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
