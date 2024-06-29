package com.github.wallacewatler.javamcts.hidden;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a distribution of states reached via a particular sequence of actions. Each action leading from a node
 * maps to a unique child node.
 */
public final class ActionSeqNode implements SearchNode<Object> {
    private final ReentrantReadWriteLock statsLock;
    private final ReentrantLock childCreationLock;
    private final Map<Object, ActionSeqNode> children;

    /** Number of times this node has been visited. */
    private volatile int visitCount = 0;

    /** Number of times this node has been available for selection. */
    private volatile int availableCount = 0;

    /** Total score that each player obtains by going through this node. */
    private final double[] totalScores;

    public ActionSeqNode(int numPlayers) {
        statsLock = new ReentrantReadWriteLock();
        childCreationLock = new ReentrantLock();
        children = new ConcurrentHashMap<>();
        totalScores = new double[numPlayers];
    }

    @Override
    public int visitCount() {
        return visitCount;
    }

    @Override
    public double totalScore(int activePlayer) {
        return totalScores[activePlayer];
    }

    @Override
    public ActionSeqNode getChild(Object action) {
        return children.get(action);
    }

    @Override
    public int selectCount(Object action) {
        return getChild(action).visitCount;
    }

    @Override
    public int availableCount(Object action) {
        return getChild(action).availableCount;
    }

    @Override
    public ReadWriteLock statsLock() {
        return statsLock;
    }

    public void createChildIfNotPresent(Object action) {
        childCreationLock.lock();

        if(!children.containsKey(action))
            children.put(action, new ActionSeqNode(totalScores.length));

        childCreationLock.unlock();
    }

    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    public void updateScores(double[] scores) {
        statsLock.writeLock().lock();

        visitCount++;
        for(int i = 0; i < scores.length; i++)
            totalScores[i] += scores[i];

        statsLock.writeLock().unlock();
    }

    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    public void incAvailableCount() {
        statsLock.writeLock().lock();
        availableCount++;
        statsLock.writeLock().unlock();
    }

    public int numNodes() {
        int num = 1;
        for(ActionSeqNode child : children.values())
            num += child.numNodes();

        return num;
    }

    @Override
    public String toString() {
        return "ActionSeqNode{" +
                "visitCount=" + visitCount +
                ", availableCount=" + availableCount +
                ", totalScores=" + Arrays.toString(totalScores) +
                '}';
    }
}
