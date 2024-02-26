package wallacewatler.moismcts;

import java.util.ArrayDeque;

final class PlayerPerspective<STATE, INFO_SET, MOVE extends Move<?, INFO_SET>> {
    /*
    TODO: Pull infoSet out of this class. The info set is only required for players that this system is modeling; any
     external players should not have an info set and cannot be searched, but will still need a PlayerPerspective to
     store their decision tree. EDIT: Just throw these away when done searching.
     */
    private INFO_SET infoSet;
    private Node<STATE, MOVE> rootNode = new Node<>();

    // Pre-allocated collection used during iteration
    private final ArrayDeque<Node<STATE, MOVE>> traversedNodes = new ArrayDeque<>();

    PlayerPerspective(INFO_SET infoSet) {
        this.infoSet = infoSet;
    }

    INFO_SET getInfoSet() {
        return infoSet;
    }

    Node<STATE, MOVE> getRootNode() {
        return rootNode;
    }

    void advanceRoot(MOVE move) {
        rootNode = rootNode.child(move);
        infoSet = move.applyToInfoSet(infoSet);
    }

    void beginTraversal() {
        traversedNodes.clear();
        traversedNodes.push(rootNode);
    }

    void descend(MOVE move) {
        final Node<STATE, MOVE> child = currentNode().child(move);
        traversedNodes.push(child);
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
