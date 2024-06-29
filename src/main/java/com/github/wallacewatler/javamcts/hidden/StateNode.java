package com.github.wallacewatler.javamcts.hidden;

import com.github.wallacewatler.javamcts.DeterministicAction;
import com.github.wallacewatler.javamcts.VisibleState;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A node in a search tree that represents a particular game state. Each node stores a game state, and each action
 * leading from a node maps to a unique child node. Locks are used to prevent data corruption from simultaneous
 * reads and writes.
 */
public final class StateNode<STATE extends VisibleState<STATE, ACTION>, ACTION extends DeterministicAction<STATE>> implements SearchNode<ACTION> {
    private final ReentrantReadWriteLock statsLock = new ReentrantReadWriteLock();
    private final ReentrantLock childCreationLock = new ReentrantLock();
    private final ConcurrentHashMap<ACTION, StateNode<STATE, ACTION>> children = new ConcurrentHashMap<>();
    public final STATE state;

    /** Number of times this node has been visited. */
    private volatile int visitCount = 0;

    /** Total score that each player obtains by going through this node. */
    private final double[] totalScores;

    public StateNode(int numPlayers, STATE state) {
        this.state = state;
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
    public StateNode<STATE, ACTION> getChild(ACTION action) {
        return children.get(action);
    }

    @Override
    public int selectCount(ACTION action) {
        return getChild(action).visitCount;
    }

    @Override
    public int availableCount(ACTION action) {
        return visitCount;
    }

    @Override
    public ReadWriteLock statsLock() {
        return statsLock;
    }

    public void createChildIfNotPresent(ACTION action, TranspositionTable<STATE, ACTION> table) {
        childCreationLock.lock();
        if(!children.containsKey(action)) {
            final STATE state = action.applyToState(this.state.copy());
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (table) {
                if(table.contains(state)) {
                    children.put(action, table.get(state));
                } else {
                    final StateNode<STATE, ACTION> child = new StateNode<>(totalScores.length, state);
                    children.put(action, child);
                    table.put(state, child);
                }
            }
        }
        childCreationLock.unlock();
    }

    public List<ACTION> validActions() {
        return state.validActions();
    }

    public double[] scores() {
        return state.scores();
    }

    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    public void updateScores(double[] scores) {
        statsLock.writeLock().lock();
        visitCount++;
        for(int i = 0; i < scores.length; i++)
            totalScores[i] += scores[i];

        statsLock.writeLock().unlock();
    }

    /**
     * @return The number of nodes in this tree.
     */
    public int numNodes() {
        int num = 1;
        for(StateNode<STATE, ACTION> child : children.values())
            num += child.numNodes();

        return num;
    }

    @Override
    public String toString() {
        return "StateNode{" +
                "visitCount=" + visitCount +
                ", totalReward=" + Arrays.toString(totalScores) +
                '}';
    }
}
