package com.github.wallacewatler.javamcts.hidden;

import com.github.wallacewatler.javamcts.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Procedures that don't belong to any one class. */
public final class Procedures {
    /**
     * Do an iteration of MCTS.
     */
    public static <STATE extends VisibleState<STATE, ACTION>, ACTION extends DeterministicAction<STATE>>
    void iterMCTS(StateNode<STATE, ACTION> rootNode, UCT uct, Random rand, TranspositionTable<STATE, ACTION> table) {
        // Stores the path of traversal through the search tree
        final ArrayDeque<StateNode<STATE, ACTION>> nodePath = new ArrayDeque<>();

        StateNode<STATE, ACTION> currentNode = rootNode;
        nodePath.add(currentNode);

        // Selection and Expansion - Select child nodes using UCT, expanding where necessary.
        boolean continueSelection = true;
        while(currentNode.scores() == null && continueSelection) {
            final ACTION selectedAction = uctSelection(currentNode, currentNode.validActions(), currentNode.state.activePlayer(), uct, rand);

            currentNode.createChildIfNotPresent(selectedAction, table);
            final StateNode<STATE, ACTION> selectedChild = currentNode.getChild(selectedAction);

            if(selectedChild.visitCount() == 0)
                continueSelection = false;

            currentNode = selectedChild;
            nodePath.add(currentNode);
        }

        // Simulation - Choose a random action until the game is decided.
        STATE simulatedState = currentNode.state.copy();
        while(simulatedState.scores() == null) {
            final List<ACTION> validActions = simulatedState.validActions();
            final ACTION action = validActions.get(rand.nextInt(validActions.size()));
            simulatedState = action.applyToState(simulatedState);
        }

        // Backpropagation - Update all nodes that were selected with the results of simulation.
        final double[] scores = simulatedState.scores();
        while(!nodePath.isEmpty())
            nodePath.removeLast().updateScores(scores);
    }

    /**
     * Do an iteration of OLMCTS.
     */
    public static <STATE extends VisibleState<STATE, ACTION>, ACTION extends StochasticAction<STATE>>
    void iterOLMCTS(STATE rootState, ActionSeqNode rootNode, UCT uct, Random rand) {
        final STATE simulatedState = rootState.copy();
        iterActionSeq(simulatedState, rootNode, uct, rand);
    }

    /**
     * Do an iteration of ISMCTS.
     */
    public static <STATE extends State<ACTION>, ACTION extends StochasticAction<STATE>>
    void iterISMCTS(InfoSet<STATE, ACTION> infoSet, ActionSeqNode rootNode, UCT uct, Random rand) {
        // Choose a randomly determinized state consistent with the information set of the player searching the tree.
        final STATE simulatedState = infoSet.determinize(rand);
        iterActionSeq(simulatedState, rootNode, uct, rand);
    }

    /**
     * Do an iteration of MO-ISMCTS.
     */
    public static <STATE extends State<ACTION>, ACTION extends ObservableAction<STATE, MOVE>, MOVE extends Move<ACTION>>
    void iterMOISMCTS(InfoSet<STATE, MOVE> infoSet, ArrayList<MoveSeqNode> rootNodes, UCT uct, Random rand) {
        /*
        Stores the path of traversal through each player's search tree. The first element of the outer list is the root
        nodes, the second element is the nodes one level down, etc.
         */
        final ArrayDeque<ArrayList<MoveSeqNode>> nodeLevels = new ArrayDeque<>();
        nodeLevels.add(rootNodes);

        // The node at i is the current node in player i's tree.
        ArrayList<MoveSeqNode> currentNodes = rootNodes;

        // Current node in the tree of the active player
        MoveSeqNode activeNode = currentNodes.get(infoSet.owner());

        // Choose a random determinized state consistent with the information set of the player searching the tree.
        STATE simulatedState = infoSet.determinize(rand);
        List<MOVE> validMoves = stateValidMoves(simulatedState);

        // Selection and Expansion - Select child nodes using UCT, expanding where necessary.
        boolean continueSelection = true;
        while(simulatedState.scores() == null && continueSelection) {
            final MOVE selectedMove = uctSelection(activeNode, validMoves, simulatedState.activePlayer(), uct, rand);

            for(MOVE move : validMoves) {
                activeNode.createChildIfNotPresent(move);
                activeNode.getChild(move).incAvailableCount();
            }

            final MoveSeqNode selectedChild = activeNode.getChild(selectedMove);
            if(selectedChild.visitCount() == 0)
                continueSelection = false;

            final ACTION selectedAction = selectedMove.asAction();

            // Use the selected action to descend through each player's tree.
            final ArrayList<MoveSeqNode> nextLevel = new ArrayList<>(currentNodes.size());
            nodeLevels.add(nextLevel);
            for(int pov = 0; pov < currentNodes.size(); pov++) {
                final MOVE move = selectedAction.observe(pov);
                final MoveSeqNode node = currentNodes.get(pov);
                node.createChildIfNotPresent(move);
                nextLevel.add(node.getChild(move));
            }
            currentNodes = nextLevel;

            // Set up for next selection
            simulatedState = selectedAction.applyToState(simulatedState, rand);
            validMoves = stateValidMoves(simulatedState);
            activeNode = currentNodes.get(simulatedState.activePlayer());
        }

        // Simulation - Choose a random action until the game is decided.
        while(simulatedState.scores() == null) {
            final ACTION action = validMoves.get(rand.nextInt(validMoves.size())).asAction();
            simulatedState = action.applyToState(simulatedState, rand);
            validMoves = stateValidMoves(simulatedState);
        }

        // Backpropagation - Update all nodes that were selected with the results of simulation.
        final double[] scores = simulatedState.scores();
        while(!nodeLevels.isEmpty()) {
            final ArrayList<MoveSeqNode> nodeLevel = nodeLevels.removeLast();
            for(int pov = 0; pov < nodeLevel.size(); pov++)
                nodeLevel.get(pov).updateScore(scores[pov]);
        }
    }

    /**
     * Select a branch from a node using UCT.
     *
     * @param parent a node from which to select a branch
     * @param branches the available branches
     * @param activePlayer the player for whom to consider node scores
     * @param uct UCT parameters
     * @param rand a source of randomness
     *
     * @return A branch selected by UCT.
     *
     * @param <BRANCH> the type of connections between nodes
     */
    public static <BRANCH> BRANCH uctSelection(SearchNode<? super BRANCH> parent, List<BRANCH> branches, int activePlayer, UCT uct, Random rand) {
        if(parent.visitCount() == 0)
            return branches.get(rand.nextInt(branches.size()));

        final ArrayList<BRANCH> maxBranches = new ArrayList<>();
        double maxUctValue = Double.NEGATIVE_INFINITY;

        parent.statsLock().readLock().lock();
        for(BRANCH branch : branches) {
            final SearchNode<? super BRANCH> child = parent.getChild(branch);
            final double uctValue;
            if(child == null || child.visitCount() == 0 || parent.availableCount(branch) == 0 || parent.selectCount(branch) == 0) {
                uctValue = uct.favorUnexplored() ? Double.POSITIVE_INFINITY : (parent.totalScore(activePlayer) / parent.visitCount());
            } else {
                child.statsLock().readLock().lock();
                final double exploitation = child.totalScore(activePlayer) / child.visitCount();
                final double exploration = uct.explorationParam() * Math.sqrt(Math.log(parent.availableCount(branch)) / parent.selectCount(branch));
                child.statsLock().readLock().unlock();
                uctValue = exploitation + exploration;
            }

            if(uctValue == maxUctValue) {
                maxBranches.add(branch);
            } else if(uctValue > maxUctValue) {
                maxUctValue = uctValue;
                maxBranches.clear();
                maxBranches.add(branch);
            }
        }
        parent.statsLock().readLock().unlock();

        return maxBranches.get(rand.nextInt(maxBranches.size()));
    }

    /**
     * @param node a node
     * @param branches the possible branches leading out of {@code node}
     * @param rand a source of randomness, used to break ties
     *
     * @return The most visited child of {@code node}.
     *
     * @param <BRANCH> the type of {@code node}'s branches
     */
    public static <BRANCH> BRANCH mostVisited(SearchNode<? super BRANCH> node, List<BRANCH> branches, Random rand) {
        final ArrayList<BRANCH> maxBranches = new ArrayList<>();
        int maxVisits = 0;
        for(BRANCH branch : branches) {
            final SearchNode<? super BRANCH> child = node.getChild(branch);
            final int visitCount = child == null ? 0 : child.visitCount();
            if(visitCount > maxVisits) {
                maxVisits = visitCount;
                maxBranches.clear();
                maxBranches.add(branch);
            } else if(visitCount == maxVisits) {
                maxBranches.add(branch);
            }
        }
        return maxBranches.get(rand.nextInt(maxBranches.size()));
    }

    /**
     * Shared by OLMCTS and ISMCTS. Does an iteration using a simulated state and action sequence nodes.
     */
    private static <STATE extends State<ACTION>, ACTION extends StochasticAction<STATE>>
    void iterActionSeq(STATE simulatedState, ActionSeqNode rootNode, UCT uct, Random rand) {
        // Stores the path of traversal through the search tree
        final ArrayDeque<ActionSeqNode> nodePath = new ArrayDeque<>();

        ActionSeqNode currentNode = rootNode;
        nodePath.add(currentNode);

        List<ACTION> validActions = simulatedState.validActions();

        // Selection and Expansion - Select child nodes using UCT, expanding where necessary.
        boolean continueSelection = true;
        while(simulatedState.scores() == null && continueSelection) {
            final ACTION selectedAction = uctSelection(currentNode, validActions, simulatedState.activePlayer(), uct, rand);

            for(ACTION action : validActions) {
                currentNode.createChildIfNotPresent(action);
                currentNode.getChild(action).incAvailableCount();
            }

            final ActionSeqNode selectedChild = currentNode.getChild(selectedAction);
            if(selectedChild.visitCount() == 0)
                continueSelection = false;

            simulatedState = selectedAction.applyToState(simulatedState, rand);
            validActions = simulatedState.validActions();

            currentNode = selectedChild;
            nodePath.add(currentNode);
        }

        // Simulation - Choose a random action until the game is decided.
        while(simulatedState.scores() == null) {
            final ACTION action = validActions.get(rand.nextInt(validActions.size()));
            simulatedState = action.applyToState(simulatedState, rand);
            validActions = simulatedState.validActions();
        }

        // Backpropagation - Update all nodes that were selected with the results of simulation.
        final double[] scores = simulatedState.scores();
        while(!nodePath.isEmpty())
            nodePath.removeLast().updateScores(scores);
    }

    /** Computes the valid moves for a state. */
    private static <MOVE> List<MOVE> stateValidMoves(State<? extends ObservableAction<?, MOVE>> state) {
        return state.validActions().stream()
                .map(action -> action.observe(state.activePlayer()))
                .toList();
    }
}
