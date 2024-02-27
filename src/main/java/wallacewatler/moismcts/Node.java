package wallacewatler.moismcts;

import java.util.*;

/**
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @param <STATE>
 * @param <MOVE>
 */
final class Node<STATE, MOVE> {
    private final Object childLock = new Object();

    /** Number of times this node has been visited. */
    private volatile int visitCount = 0;

    /** Number of times this node has been available for selection. */
    private volatile int availableCount = 0;

    /** Total reward from going through this node. */
    private volatile double totalReward = 0.0;

    /**
     * The resulting node for every move taken from this node. Each move maps one-to-one to a node.
     */
    private final HashMap<MOVE, Node<STATE, MOVE>> children = new HashMap<>();

    public int getVisitCount() {
        return visitCount;
    }

    synchronized void incAvailableCount() {
        availableCount++;
    }

    synchronized void incVisitCount() {
        visitCount++;
    }

    synchronized void addReward(double reward) {
        totalReward += reward;
    }

    Node<STATE, MOVE> child(MOVE move) {
        synchronized(childLock) {
            if(!children.containsKey(move))
                children.put(move, new Node<>());

            return children.get(move);
        }
    }

    synchronized <ACTION extends Action<?, MOVE>> ACTION chooseAction(int player, List<ACTION> actions, Random rand, UCT uct) {
        if(visitCount == 0)
            return actions.get(rand.nextInt(actions.size()));

        final ArrayList<ACTION> maxActions = new ArrayList<>();
        double maxUctValue = Double.NEGATIVE_INFINITY;

        for(ACTION action : actions) {
            final Node<?, MOVE> child = child(action.observe(player));
            final double uctValue;
            synchronized(child) {
                if(child.availableCount == 0 || child.visitCount == 0) {
                    uctValue = uct.favorUnexplored() ? Double.POSITIVE_INFINITY : (totalReward / visitCount);
                } else {
                    final double exploitation = child.totalReward / child.visitCount;
                    final double exploration = uct.explorationParam() * Math.sqrt(Math.log(child.availableCount) / child.visitCount);
                    uctValue = exploitation + exploration;
                }
            }

            if(uctValue == maxUctValue) {
                maxActions.add(action);
            } else if(uctValue > maxUctValue) {
                maxUctValue = uctValue;
                maxActions.clear();
                maxActions.add(action);
            }
        }

        return maxActions.get(rand.nextInt(maxActions.size()));
    }

    @Override
    public String toString() {
        return "Node{" +
                "visitCount=" + visitCount +
                ", availableCount=" + availableCount +
                ", totalReward=" + totalReward +
                '}';
    }
}
