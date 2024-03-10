package com.github.wallacewatler.javamcts;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * A node in an MCTS tree that stores statistics regarding a search.
 *
 * @param <BRANCH> the type of object connecting nodes
 */
interface SearchNode<BRANCH> {
    SearchNode<BRANCH> getChild(BRANCH branch);

    void createChild(BRANCH branch);

    /**
     * @return The number of times this node has been visited.
     */
    int visitCount();

    /**
     * @return The number of times this node has been available for selection.
     */
    int availableCount();

    double totalScore(int activePlayer);

    ReadWriteLock statsLock();

    /** A lock that is always acquirable. */
    ReadWriteLock DUMMY_LOCK = new ReadWriteLock() {
        private static final Lock DUMMY_LOCK = new Lock() {
            public void lock() {}
            public void lockInterruptibly() {}
            public boolean tryLock() { return true; }
            public boolean tryLock(long time, TimeUnit unit) { return true; }
            public void unlock() {}
            public Condition newCondition() { throw new UnsupportedOperationException(); }
        };
        public Lock readLock() { return DUMMY_LOCK; }
        public Lock writeLock() { return DUMMY_LOCK; }
    };

    /**
     * @param node a node
     * @param branches the possible branches leading out of {@code node}
     * @param rand a source of randomness, used to break ties
     *
     * @return The most visited child of {@code node}
     *
     * @param <BRANCH> the type of {@code node}'s branches
     */
    static <BRANCH> BRANCH mostVisited(SearchNode<? super BRANCH> node, List<BRANCH> branches, Random rand) {
        final ArrayList<BRANCH> maxBranches = new ArrayList<>();
        int maxVisits = 0;
        for(BRANCH branch : branches) {
            final SearchNode<? super BRANCH> child = node.getChild(branch);
            final int visitCount = child == null ? 0 : child.visitCount();
            if(visitCount > maxVisits) {
                maxVisits = visitCount;
                maxBranches.clear();
                maxBranches.add(branch);
            } else if(visitCount == maxVisits) {
                maxBranches.add(branch);
            }
        }
        return maxBranches.get(rand.nextInt(maxBranches.size()));
    }
}
