package com.github.wallacewatler.javamcts;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A node in an MCTS tree that stores statistics regarding a search.
 *
 * @param <BRANCH> the type of object connecting nodes
 */
interface SearchNode<BRANCH> {
    SearchNode<BRANCH> getChild(BRANCH branch);

    /**
     * @return The number of times this node has been visited.
     */
    int visitCount();

    /**
     * @param node a node
     * @param branches the possible branches leading out of {@code node}
     * @param rand a source of randomness, used to break ties
     *
     * @return The most visited child of {@code node}.
     *
     * @param <BRANCH> the type of {@code node}'s branches
     */
    static <BRANCH> BRANCH mostVisited(SearchNode<? super BRANCH> node, List<BRANCH> branches, Random rand) {
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
}
