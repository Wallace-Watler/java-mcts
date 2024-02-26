package wallacewatler.moismcts;

import java.util.HashMap;
import java.util.Map;

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
    /** Number of times this node has been visited. */
    int visitCount;

    /** Number of times this node has been available. */
    int availableCount;

    /** Total reward from going through this node for each player. */
    double totalReward;

    /**
     * The resulting node for every move taken from this node. Each move maps one-to-one to a node.
     */
    private final Map<MOVE, Node<STATE, MOVE>> children = new HashMap<>();

    Node<STATE, MOVE> child(MOVE move) {
        if(!children.containsKey(move))
            children.put(move, new Node<>());

        return children.get(move);
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
