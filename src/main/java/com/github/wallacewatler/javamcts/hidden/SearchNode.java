package com.github.wallacewatler.javamcts.hidden;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * A node in a search tree. Each node stores statistics such as the number of times it's been visited and its estimated
 * value. Locks are used to prevent data corruption from simultaneous reads and writes.
 *
 * @param <BRANCH> the type of object connecting nodes
 */
public interface SearchNode<BRANCH> {
    /**
     * @return The number of times this node has been visited.
     */
    int visitCount();

    /**
     * @param activePlayer the player who does an action from this node
     *
     * @return The estimated score for {@code activePlayer} in this node.
     */
    double totalScore(int activePlayer);

    /**
     * @param branch a branch leading out of this node
     *
     * @return The child of this node that corresponds to {@code branch}.
     */
    SearchNode<BRANCH> getChild(BRANCH branch);

    /**
     * @return The number of times {@code branch} has been selected.
     */
    int selectCount(BRANCH branch);

    /**
     * @return The number of times {@code branch} has been available for selection.
     */
    int availableCount(BRANCH branch);

    /**
     * @return The lock used to maintain the integrity of this node's statistics.
     */
    ReadWriteLock statsLock();
}
