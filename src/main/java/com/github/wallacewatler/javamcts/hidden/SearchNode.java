package com.github.wallacewatler.javamcts.hidden;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A node in any MCTS tree that stores statistics of a search.
 *
 * @param <BRANCH> the type of object connecting nodes
 */
public interface SearchNode<BRANCH> {
    /** A non-functional {@link ReadWriteLock}. */
    ReadWriteLock DUMMY_RW_LOCK = new ReadWriteLock() {
        public Lock readLock() { return DUMMY_LOCK; }
        public Lock writeLock() { return DUMMY_LOCK; }
    };

    /** A non-functional {@link Lock}. */
    Lock DUMMY_LOCK = new Lock() {
        public void lock() {}
        public void lockInterruptibly() {}
        public boolean tryLock() { return true; }
        public boolean tryLock(long time, TimeUnit unit) { return true; }
        public void unlock() {}
        public Condition newCondition() { throw new UnsupportedOperationException(); }
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
}
