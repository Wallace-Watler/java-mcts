package wallacewatler.javamcts.mcts;

import wallacewatler.javamcts.UCT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A node in an MCTS search tree. The tree is structured such that each node stores a game state, and each action
 * leading from a node maps to a unique child node.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @param <STATE> the type of state this node stores
 * @param <ACTION> the type of action that links nodes together
 */
final class Node<STATE extends State<STATE, ACTION>, ACTION extends Action<STATE>> {
    private final ReentrantReadWriteLock statsLock = new ReentrantReadWriteLock();

    final Node<STATE, ACTION> parent;
    final STATE state;
    final ArrayList<ACTION> availableActions;
    private final ArrayList<Node<STATE, ACTION>> children;

    /** Number of times this node has been visited. */
    private volatile int visitCount = 0;

    /** Total score that each player obtains by going through this node. */
    private final double[] totalScores;

    /** Number of threads currently searching through this node. */
    final AtomicInteger threadsSearching = new AtomicInteger();

    Node(int numPlayers, STATE state, Node<STATE, ACTION> parent) {
        this.parent = parent;
        this.state = state;
        availableActions = new ArrayList<>(state.availableActions());
        totalScores = new double[numPlayers];

        children = new ArrayList<>(availableActions.size());
        for(int i = 0; i < availableActions.size(); i++)
            children.add(null);
    }

    int getVisitCount() {
        return visitCount;
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

    Node<STATE, ACTION> getChild(int action) {
        return children.get(action);
    }

    synchronized Node<STATE, ACTION> getOrCreateChild(int action) {
        if(children.get(action) == null) {
            final STATE state = availableActions.get(action).applyToState(this.state.copy());
            children.set(action, new Node<>(totalScores.length, state, this));
        }
        return children.get(action);
    }

    int selectAction(UCT uct, Random rand) {
        if(visitCount == 0)
            return rand.nextInt(availableActions.size());

        final int player = state.playerAboutToMove();
        final ArrayList<Integer> maxActions = new ArrayList<>();
        double maxUctValue = Double.NEGATIVE_INFINITY;

        statsLock.readLock().lock();
        for(int i = 0; i < availableActions.size(); i++) {
            final Node<STATE, ACTION> child = children.get(i);
            final double uctValue;
            if(child == null) {
                uctValue = uct.favorUnexplored() ? Double.POSITIVE_INFINITY : (totalScores[player] / visitCount);
            } else if(child.threadsSearching.get() > 0) {
                uctValue = Double.NEGATIVE_INFINITY;
            } else if(child.visitCount == 0) {
                uctValue = uct.favorUnexplored() ? Double.POSITIVE_INFINITY : (totalScores[player] / visitCount);
            } else {
                child.statsLock.readLock().lock();
                final double exploitation = child.totalScores[player] / child.visitCount;
                final double exploration = uct.explorationParam() * Math.sqrt(Math.log(visitCount) / child.visitCount);
                uctValue = exploitation + exploration;
                child.statsLock.readLock().unlock();
            }

            if(uctValue == maxUctValue) {
                maxActions.add(i);
            } else if(uctValue > maxUctValue) {
                maxUctValue = uctValue;
                maxActions.clear();
                maxActions.add(i);
            }
        }
        statsLock.readLock().unlock();

        return maxActions.get(rand.nextInt(maxActions.size()));
    }

    @Override
    public String toString() {
        return "Node{" +
                "visitCount=" + visitCount +
                ", totalReward=" + Arrays.toString(totalScores) +
                '}';
    }
}
