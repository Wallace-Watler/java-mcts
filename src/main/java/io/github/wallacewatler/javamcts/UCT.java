package io.github.wallacewatler.javamcts;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Upper confidence bound 1 policy for trees (UCT). UCT is used in tree search algorithms to decide which branches to
 * follow.
 *
 * @param explorationParam Higher values prioritize exploring more of the tree, lower values prioritize exploiting
 *                         promising branches. The recommended value is {@code sqrt(2)} if rewards are in the range
 *                         [0.0, 1.0], but this can be tweaked to improve search quality.
 * @param favorUnexplored If true, actions that have never been explored will take priority over any other. If there are
 *                        multiple such actions, one is chosen at random.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 */
public record UCT(double explorationParam, boolean favorUnexplored) {
    public <BRANCH> BRANCH selectBranch(Node<BRANCH> node, Random rand, int activePlayer, List<? extends BRANCH> branches) {
        if(node.visitCount() == 0)
            return branches.get(rand.nextInt(branches.size()));

        final ArrayList<BRANCH> maxBranches = new ArrayList<>();
        double maxUctValue = Double.NEGATIVE_INFINITY;

        node.statsLock().readLock().lock();
        for(BRANCH branch : branches) {
            final Node<BRANCH> child = node.getChild(branch);
            final double uctValue;
            if(child == null || child.availableCount() == 0 || child.visitCount() == 0) {
                uctValue = favorUnexplored() ? Double.POSITIVE_INFINITY : (node.totalScore(activePlayer) / node.visitCount());
            } else {
                child.statsLock().readLock().lock();
                final double exploitation = child.totalScore(activePlayer) / child.visitCount();
                final double exploration = explorationParam() * Math.sqrt(Math.log(child.availableCount()) / child.visitCount());
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
        node.statsLock().readLock().unlock();

        return maxBranches.get(rand.nextInt(maxBranches.size()));
    }
}
