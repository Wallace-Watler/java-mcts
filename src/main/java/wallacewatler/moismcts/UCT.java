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
 *                         promising branches. The recommended value is {@code sqrt(2)}, but this can be tweaked
 *                         to improve search quality. Typical values are in the range [0.5, 2.0].
 * @param favorUnexplored If true, actions that have never been explored will take priority over any other. If there are
 *                        multiple such actions, one is chosen at random.
 */
public record UCT(double explorationParam, boolean favorUnexplored) {
    <MOVE> MOVE chooseMove(Node<?, MOVE> node, List<MOVE> moves, Random rand) {
        assert !moves.isEmpty();

        if(node.visitCount == 0)
            return moves.get(rand.nextInt(moves.size()));

        final ArrayList<MOVE> maxMoves = new ArrayList<>();
        double maxUctValue = Double.NEGATIVE_INFINITY;

        for(MOVE move : moves) {
            final Node<?, MOVE> child = node.child(move);

            final double uctValue;
            if(child == null || child.availableCount == 0 || child.visitCount == 0) {
                uctValue = favorUnexplored ? Double.POSITIVE_INFINITY : (node.totalReward / node.visitCount);
            } else {
                final double exploitation = child.totalReward / child.visitCount;
                final double exploration = explorationParam * Math.sqrt(Math.log(child.availableCount) / child.visitCount);
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
}
