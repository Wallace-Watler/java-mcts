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
    /** A non-functional {@link ReadWriteLock}. */
    ReadWriteLock DUMMY_LOCK = new ReadWriteLock() {
        private static final Lock LOCK = new Lock() {
            public void lock() {}
            public void lockInterruptibly() {}
            public boolean tryLock() { return true; }
            public boolean tryLock(long time, TimeUnit unit) { return true; }
            public void unlock() {}
            public Condition newCondition() { throw new UnsupportedOperationException(); }
        };
        public Lock readLock() { return LOCK; }
        public Lock writeLock() { return LOCK; }
    };

    /**
     * @return The number of times this node has been visited.
     */
    int visitCount();

    double totalScore(int activePlayer);

    SearchNode<BRANCH> getChild(BRANCH branch);

    /**
     * @return The number of times {@code branch} has been selected.
     */
    int selectCount(BRANCH branch);

    /**
     * @return The number of times {@code branch} has been available for selection.
     */
    int availableCount(BRANCH branch);

    ReadWriteLock statsLock();

    /**
     * Select a branch from a node using UCT.
     *
     * @param parent a node from which to select a branch
     * @param branches the available branches
     * @param activePlayer the player for whom to consider node scores
     * @param uct UCT parameters
     * @param rand a source of randomness
     *
     * @return A branch selected by UCT.
     *
     * @param <BRANCH> the type of connections between nodes
     */
    static <BRANCH> BRANCH selectBranch(SearchNode<? super BRANCH> parent, List<BRANCH> branches, int activePlayer, UCT uct, Random rand) {
        if(parent.visitCount() == 0)
            return branches.get(rand.nextInt(branches.size()));

        final ArrayList<BRANCH> maxBranches = new ArrayList<>();
        double maxUctValue = Double.NEGATIVE_INFINITY;

        parent.statsLock().readLock().lock();
        for(BRANCH branch : branches) {
            final SearchNode<? super BRANCH> child = parent.getChild(branch);
            final double uctValue;
            if(child == null || child.visitCount() == 0 || parent.availableCount(branch) == 0 || parent.selectCount(branch) == 0) {
                uctValue = uct.favorUnexplored() ? Double.POSITIVE_INFINITY : (parent.totalScore(activePlayer) / parent.visitCount());
            } else {
                child.statsLock().readLock().lock();
                final double exploitation = child.totalScore(activePlayer) / child.visitCount();
                final double exploration = uct.explorationParam() * Math.sqrt(Math.log(parent.availableCount(branch)) / parent.selectCount(branch));
                child.statsLock().readLock().unlock();
                uctValue = exploitation + exploration;
            }

            if(uctValue == maxUctValue) {
                maxBranches.add(branch);
            } else if(uctValue > maxUctValue) {
                maxUctValue = uctValue;
                maxBranches.clear();
                maxBranches.add(branch);
            }
        }
        parent.statsLock().readLock().unlock();

        return maxBranches.get(rand.nextInt(maxBranches.size()));
    }

    /**
     * @param node a node
     * @param branches the possible branches leading out of {@code node}
     * @param rand a source of randomness, used to break ties
     *
     * @return The most visited child of {@code node}.
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
