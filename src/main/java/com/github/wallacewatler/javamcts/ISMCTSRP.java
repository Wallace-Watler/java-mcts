package com.github.wallacewatler.javamcts;

import com.github.wallacewatler.javamcts.hidden.ActionSeqNode;
import com.github.wallacewatler.javamcts.hidden.Procedures;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Information set MCTS with root parallelization. For details on how to use this class, see {@link ISMCTS}.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 */
public final class ISMCTSRP implements ISMCTS, Cloneable {
    @Override
    public
    <STATE extends State<ACTION>, ACTION extends StochasticAction<STATE>>
    SearchResults<ACTION> search(int numPlayers, InfoSet<STATE, ACTION> infoSet, SearchParameters params, Random rand) {
        if(numPlayers < 1)
            throw new IllegalArgumentException("numPlayers must be at least 1");

        if(infoSet.validMoves().isEmpty())
            return new SearchResults<>(null, 0, 0, 1, 1);

        // These are shared across threads
        final long start = System.currentTimeMillis();
        final AtomicInteger totalIters = new AtomicInteger();
        // -------------------------------

        // One search tree for each thread
        final ArrayList<ActionSeqNode> rootNodes = new ArrayList<>(params.threadCount());
        for(int i = 0; i < params.threadCount(); i++)
            rootNodes.add(new ActionSeqNode(numPlayers));

        // Start parallel searches.
        final Thread[] workers = new Thread[params.threadCount()];
        for(int workerNum = 0; workerNum < workers.length; workerNum++) {
            final ActionSeqNode rootNode = rootNodes.get(workerNum);
            workers[workerNum] = new Thread(() -> {
                long now = System.currentTimeMillis();
                int iters = 0;
                while(!Thread.interrupted() && now - start <= params.maxTime() && (now - start < params.minTime() || iters < params.maxIters())) {
                    Procedures.iterISMCTS(infoSet, rootNode, params.uct(), rand);
                    iters++;
                    now = System.currentTimeMillis();
                }
                totalIters.addAndGet(iters);
            }, "ismctsrp" + workerNum);
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

        // Recommend the most selected action by majority voting.
        final HashMap<ACTION, Integer> votes = new HashMap<>();
        int numNodes = 0;
        for(ActionSeqNode root : rootNodes) {
            final ACTION action = Procedures.mostVisited(root, infoSet.validMoves(), rand);
            votes.put(action, votes.getOrDefault(action, 0) + 1);
            numNodes += root.numNodes();
        }

        final ACTION bestAction = votes.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .get()
                .getKey();

        final double itersPerThread = (double) totalIters.get() / params.threadCount();
        return new SearchResults<>(bestAction, itersPerThread, System.currentTimeMillis() - start, numNodes, 0);
    }

    @Override
    public String toString() {
        return "ISMCTS-RP";
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
