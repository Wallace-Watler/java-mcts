package com.github.wallacewatler.javamcts;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * MO-ISMCTS with root parallelization.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @see MOISMCTS
 * @see MOISMCTSTP
 */
public final class MOISMCTSRP implements MOISMCTS {
    @Override
    public
    <STATE extends State<ACTION>, ACTION extends Action<STATE, MOVE>, MOVE extends Move<ACTION>>
    SearchResults<ACTION> search(int numPlayers, InfoSet<STATE, MOVE> infoSet, SearchParameters params, Random rand) {
        if(numPlayers < 1)
            throw new IllegalArgumentException("numPlayers must be at least 1");

        if(infoSet.validMoves().isEmpty())
            return new SearchResults<>(null, 0, 0);

        // These are shared across threads
        final long start = System.currentTimeMillis();
        final AtomicInteger totalIters = new AtomicInteger();
        // -------------------------------

        // The root node for each player's tree for each thread
        final ArrayList<ArrayList<Node>> trees = new ArrayList<>(params.threadCount());
        for(int thread = 0; thread < params.threadCount(); thread++) {
            final ArrayList<Node> rootNodes = new ArrayList<>(numPlayers);
            for(int i = 0; i < numPlayers; i++)
                rootNodes.add(new Node(null));

            trees.add(rootNodes);
        }

        // Start parallel searches.
        final Thread[] workers = new Thread[params.threadCount()];
        for(int workerNum = 0; workerNum < workers.length; workerNum++) {
            final ArrayList<Node> tree = trees.get(workerNum);
            workers[workerNum] = new Thread(() -> totalIters.addAndGet(rootParallelSearch(infoSet, tree, params, rand, start)), "moismctsrp" + workerNum);
            workers[workerNum].start();
        }

        // Wait for all threads to finish
        try {
            for(Thread worker : workers)
                worker.join();
        } catch(InterruptedException ignored) {
            for(Thread worker : workers)
                worker.interrupt();
        }

        // Recommend the most selected action by majority voting.
        final HashMap<MOVE, Integer> votes = new HashMap<>();
        for(ArrayList<Node> rootNodes : trees) {
            final Node rootNode = rootNodes.get(infoSet.owner());
            final MOVE action = SearchNode.mostVisited(rootNode, infoSet.validMoves(), rand);
            votes.put(action, votes.getOrDefault(action, 0) + 1);
        }
        final ACTION bestAction = votes.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).get().getKey().asAction();
        final double itersPerThread = (double) totalIters.get() / params.threadCount();
        return new SearchResults<>(bestAction, itersPerThread, System.currentTimeMillis() - start);
    }

    private static
    <STATE extends State<ACTION>, ACTION extends Action<STATE, MOVE>, MOVE extends Move<ACTION>>
    int rootParallelSearch(InfoSet<STATE, MOVE> infoSet, ArrayList<Node> rootNodes, SearchParameters params, Random rand, long start) {
        long now = System.currentTimeMillis();

        // Pre-allocated list of nodes. The node at i is the current node in player i's tree.
        final ArrayList<Node> currentNodes = new ArrayList<>(rootNodes.size());

        final Function<STATE, List<MOVE>> stateValidMoves = state -> state.validActions().stream()
                .map(action -> action.observe(state.activePlayer()))
                .toList();

        int iters = 0;
        while(!Thread.interrupted() && now - start <= params.maxTime() && (now - start < params.minTime() || iters < params.maxIters())) {
            iters++;

            currentNodes.clear();
            currentNodes.addAll(rootNodes);

            // Current node in the tree of the active player
            Node activeNode = currentNodes.get(infoSet.owner());

            // Choose a random determinized state consistent with the information set of the player searching the tree.
            STATE simulatedState = infoSet.determinize(rand);
            List<MOVE> validMoves = stateValidMoves.apply(simulatedState);

            // Selection and Expansion - Select child nodes using UCT, expanding where necessary.
            boolean continueSelection = true;
            while(simulatedState.scores() == null && continueSelection) {
                for(MOVE move : validMoves)
                    activeNode.createChildIfNotPresent(move);

                final MOVE selectedMove = selectMove(activeNode, rand, validMoves, params.uct());
                final ACTION selectedAction = selectedMove.asAction();

                final Node selectedChild = activeNode.getChild(selectedMove);
                if(selectedChild.visitCount == 0)
                    continueSelection = false;

                for(MOVE move : validMoves)
                    activeNode.getChild(move).availableCount++;

                // Descend through each player's tree.
                for(int pov = 0; pov < currentNodes.size(); pov++) {
                    final MOVE move = selectedAction.observe(pov);
                    final Node node = currentNodes.get(pov);
                    node.createChildIfNotPresent(move);
                    final Node child = node.getChild(move);
                    currentNodes.set(pov, child);
                }

                simulatedState = selectedAction.applyToState(simulatedState, rand);
                validMoves = stateValidMoves.apply(simulatedState);

                activeNode = currentNodes.get(simulatedState.activePlayer());
            }

            // Simulation - Choose a random action until the game is decided.
            while(simulatedState.scores() == null) {
                final ACTION action = validMoves.get(rand.nextInt(validMoves.size())).asAction();
                simulatedState = action.applyToState(simulatedState, rand);
                validMoves = stateValidMoves.apply(simulatedState);
            }

            // Backpropagation - Update all nodes that were selected with the results of simulation.
            final double[] scores = simulatedState.scores();
            for(int pov = 0; pov < currentNodes.size(); pov++)
                currentNodes.get(pov).backPropagate(scores[pov]);

            now = System.currentTimeMillis();
        }
        return iters;
    }

    private static
    <STATE extends State<ACTION>, ACTION extends Action<STATE, MOVE>, MOVE extends Move<ACTION>>
    MOVE selectMove(Node parent, Random rand, List<MOVE> moves, UCT uct) {
        if(parent.visitCount == 0)
            return moves.get(rand.nextInt(moves.size()));

        final ArrayList<MOVE> maxMoves = new ArrayList<>();
        double maxUctValue = Double.NEGATIVE_INFINITY;

        for(MOVE move : moves) {
            final Node child = parent.getChild(move);
            final double uctValue;
            if(child == null || child.availableCount == 0 || child.visitCount == 0) {
                uctValue = uct.favorUnexplored() ? Double.POSITIVE_INFINITY : (parent.totalScore / parent.visitCount);
            } else {
                final double exploitation = child.totalScore / child.visitCount;
                final double exploration = uct.explorationParam() * Math.sqrt(Math.log(child.availableCount) / child.visitCount);
                uctValue = exploitation + exploration;
            }

            if(uctValue == maxUctValue) {
                maxMoves.add(move);
            } else if(uctValue > maxUctValue) {
                maxUctValue = uctValue;
                maxMoves.clear();
                maxMoves.add(move);
            }
        }

        return maxMoves.get(rand.nextInt(maxMoves.size()));
    }

    /**
     * A node in an MO-ISMCTS-RP search tree. A node represents a sequence of moves from the beginning of the game. Each
     * move leading from a node maps to a unique child node.
     */
    private static final class Node implements SearchNode<Object> {
        private final HashMap<Object, Node> children = new HashMap<>();
        
        private final Node parent;

        /** Number of times this node has been visited. */
        private int visitCount = 0;

        /** Number of times this node has been available for selection. */
        private int availableCount = 0;

        /** Total score from going through this node. */
        private double totalScore = 0.0;
        
        public Node(Node parent) {
            this.parent = parent;
        }

        @Override
        public Node getChild(Object move) {
            return children.get(move);
        }

        public void createChildIfNotPresent(Object move) {
            if(children.get(move) == null)
                children.put(move, new Node(this));
        }

        @Override
        public int visitCount() {
            return visitCount;
        }

        private void backPropagate(double score) {
            visitCount++;
            totalScore += score;

            if(parent != null)
                parent.backPropagate(score);
        }

        @Override
        public String toString() {
            return "Node{" +
                    "visitCount=" + visitCount +
                    ", availableCount=" + availableCount +
                    ", totalScore=" + totalScore +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "MO-ISMCTS-RP";
    }
}
