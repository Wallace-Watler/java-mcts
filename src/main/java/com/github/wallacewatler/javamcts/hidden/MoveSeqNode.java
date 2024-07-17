package com.github.wallacewatler.javamcts.hidden;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a distribution of states reached via a particular sequence of moves. Each move leading from a node maps to
 * a unique child node. Nodes of this type only pertain to a single player since moves themselves do as well.
 */
public final class MoveSeqNode implements SearchNode<Object> {
    private final ReentrantReadWriteLock statsLock = new ReentrantReadWriteLock();
    private final ReentrantLock childCreationLock = new ReentrantLock();
    private final ConcurrentHashMap<Object, MoveSeqNode> children = new ConcurrentHashMap<>();

    private volatile int visitCount = 0;
    private volatile int availableCount = 0;
    private volatile double totalScore = 0.0;

    @Override
    public int visitCount() {
        return visitCount;
    }

    @Override
    public double totalScore(int activePlayer) {
        // activePlayer is irrelevant for this type of node
        return totalScore;
    }

    @Override
    public MoveSeqNode getChild(Object move) {
        return children.get(move);
    }

    @Override
    public int selectCount(Object move) {
        return getChild(move).visitCount;
    }

    @Override
    public int availableCount(Object move) {
        return getChild(move).availableCount;
    }

    @Override
    public ReadWriteLock statsLock() {
        return statsLock;
    }

    public void createChildIfNotPresent(Object move) {
        childCreationLock.lock();

        if(!children.containsKey(move))
            children.put(move, new MoveSeqNode());

        childCreationLock.unlock();
    }

    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    public void incAvailableCount() {
        statsLock.writeLock().lock();
        availableCount++;
        statsLock.writeLock().unlock();
    }

    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    public void updateScore(double score) {
        statsLock.writeLock().lock();

        visitCount++;
        totalScore += score;

        statsLock.writeLock().unlock();
    }

    public int numNodes() {
        int num = 1;
        for(MoveSeqNode child : children.values())
            num += child.numNodes();

        return num;
    }

    @Override
    public String toString() {
        return "MoveSeqNode{" +
                "visitCount=" + visitCount +
                ", availableCount=" + availableCount +
                ", totalScore=" + totalScore +
                '}';
    }
}
