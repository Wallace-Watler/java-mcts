package wallacewatler.javamcts.olmcts;

import wallacewatler.javamcts.UCT;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A node in an OLMCTS search tree. Since actions are stochastic and states may be continuous in OLMCTS, a single node
 * represents a distribution of states reached via a particular sequence of actions. Each action leading from a node
 * maps to a unique child node.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @param <ACTION> the type of action that links nodes together
 */
final class Node<ACTION> {
    private final ReentrantReadWriteLock statsLock = new ReentrantReadWriteLock();

    final Node<ACTION> parent;
    private final HashMap<ACTION, Node<ACTION>> children = new HashMap<>();

    /** Number of times this node has been visited. */
    private volatile int visitCount = 0;

    /** Number of times this node has been available for selection. */
    private volatile int availableCount = 0;

    /** Total score that each player obtains by going through this node. */
    private final double[] totalScores;

    /** Number of threads currently searching through this node. */
    final AtomicInteger threadsSearching = new AtomicInteger();

    Node(int numPlayers, Node<ACTION> parent) {
        this.parent = parent;
        totalScores = new double[numPlayers];
    }

    int getVisitCount() {
        return visitCount;
    }

    void incAvailableCount() {
        statsLock.writeLock().lock();
        availableCount++;
        statsLock.writeLock().unlock();
    }

    void backPropagate(double[] scores) {
        statsLock.writeLock().lock();
        visitCount++;
        for(int i = 0; i < scores.length; i++)
            totalScores[i] += scores[i];

        statsLock.writeLock().unlock();

        threadsSearching.getAndDecrement();

        if(parent != null)
            parent.backPropagate(scores);
    }

    Node<ACTION> getChild(ACTION action) {
        return children.get(action);
    }

    synchronized void createChildIfNotPresent(ACTION action) {
        if(!children.containsKey(action))
            children.put(action, new Node<>(totalScores.length, this));
    }

    ACTION selectAction(UCT uct, Random rand, int player, List<ACTION> availableActions) {
        if(visitCount == 0)
            return availableActions.get(rand.nextInt(availableActions.size()));

        final ArrayList<ACTION> maxActions = new ArrayList<>();
        double maxUctValue = Double.NEGATIVE_INFINITY;

        statsLock.readLock().lock();
        for(ACTION action : availableActions) {
            final Node<ACTION> child = getChild(action);
            final double uctValue;
            if(child.threadsSearching.get() > 0) {
                uctValue = Double.NEGATIVE_INFINITY;
            } else if(child.availableCount == 0 || child.visitCount == 0) {
                uctValue = uct.favorUnexplored() ? Double.POSITIVE_INFINITY : (totalScores[player] / visitCount);
            } else {
                child.statsLock.readLock().lock();
                final double exploitation = child.totalScores[player] / child.visitCount;
                final double exploration = uct.explorationParam() * Math.sqrt(Math.log(child.availableCount) / child.visitCount);
                uctValue = exploitation + exploration;
                child.statsLock.readLock().unlock();
            }

            if(uctValue == maxUctValue) {
                maxActions.add(action);
            } else if(uctValue > maxUctValue) {
                maxUctValue = uctValue;
                maxActions.clear();
                maxActions.add(action);
            }
        }
        statsLock.readLock().unlock();

        return maxActions.get(rand.nextInt(maxActions.size()));
    }

    @Override
    public String toString() {
        return "Node{" +
                "visitCount=" + visitCount +
                ", availableCount=" + availableCount +
                ", totalScores=" + Arrays.toString(totalScores) +
                '}';
    }
}
