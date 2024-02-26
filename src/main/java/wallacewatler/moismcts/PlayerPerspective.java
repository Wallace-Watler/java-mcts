package wallacewatler.moismcts;

import java.util.ArrayDeque;

/**
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @param <STATE>
 * @param <INFO_SET>
 * @param <MOVE>
 */
final class PlayerPerspective<STATE, INFO_SET, MOVE extends Move<?, INFO_SET>> {
    private Node<STATE, MOVE> rootNode = new Node<>();

    // Pre-allocated collection used during iteration
    private final ArrayDeque<Node<STATE, MOVE>> traversedNodes = new ArrayDeque<>();

    PlayerPerspective() {}

    Node<STATE, MOVE> getRootNode() {
        return rootNode;
    }

    void advanceRoot(MOVE move) {
        rootNode = rootNode.child(move);
    }

    void beginTraversal() {
        traversedNodes.clear();
        traversedNodes.push(rootNode);
    }

    void descend(MOVE move) {
        traversedNodes.push(currentNode().child(move));
    }

    void backPropagate(double reward) {
        for(Node<STATE, MOVE> node : traversedNodes) {
            node.visitCount++;
            node.totalReward += reward;
        }
    }

    Node<STATE, MOVE> currentNode() {
        final Node<STATE, MOVE> current = traversedNodes.peek();
        assert current != null;
        return current;
    }
}
