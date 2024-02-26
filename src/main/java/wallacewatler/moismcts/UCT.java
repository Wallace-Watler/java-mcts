package wallacewatler.moismcts;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Upper confidence bound policy for trees (UCT).
 * <p>
 * UCT is used in tree search algorithms to decide which branches to follow.
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
    <ACTION extends Action<?, MOVE>, MOVE> ACTION chooseAction(int player, Node<?, MOVE> node, List<ACTION> actions, Random rand) {
        if(node.visitCount == 0)
            return actions.get(rand.nextInt(actions.size()));

        final ArrayList<ACTION> maxActions = new ArrayList<>();
        double maxUctValue = Double.NEGATIVE_INFINITY;

        for(ACTION action : actions) {
            final Node<?, MOVE> child = node.child(action.observe(player));

            final double uctValue;
            if(child == null || child.availableCount == 0 || child.visitCount == 0) {
                uctValue = favorUnexplored ? Double.POSITIVE_INFINITY : (node.totalReward / node.visitCount);
            } else {
                final double exploitation = child.totalReward / child.visitCount;
                final double exploration = explorationParam * Math.sqrt(Math.log(child.availableCount) / child.visitCount);
                uctValue = exploitation + exploration;
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
}
