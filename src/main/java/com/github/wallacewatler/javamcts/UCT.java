package com.github.wallacewatler.javamcts;

/**
 * Upper confidence bound 1 policy for trees (UCT). UCT is used in tree search algorithms to decide which branches to
 * follow.
 *
 * @param explorationParam Higher values prioritize exploring more of the tree, lower values prioritize exploiting
 *                         promising branches. The recommended value is {@code Math.sqrt(2)} if rewards are in the range
 *                         [0.0, 1.0], but this can be tweaked to improve search quality.
 * @param favorUnexplored If true, actions that have never been explored will take priority over any other. If there are
 *                        multiple such actions, one is chosen at random. The recommended value is {@code true} unless
 *                        your application has a very high branching factor.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 */
public record UCT(double explorationParam, boolean favorUnexplored) {
    /**
     * Convenience constructor for a UCT with the recommended defaults of {@code explorationParam = Math.sqrt(2)} and
     * {@code favorUnexplored = true}.
     */
    public UCT() {
        this(Math.sqrt(2), true);
    }
}
