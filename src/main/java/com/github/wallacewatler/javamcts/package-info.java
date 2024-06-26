/**
 * <h1>JavaMCTS</h1>
 * Several implementations of Monte Carlo Tree Search (MCTS) in Java:
 * <ul>
 *     <li>Closed Loop MCTS</li>
 *     <li>Open Loop MCTS</li>
 *     <li>Multiple-observer Information Set MCTS</li>
 * </ul>
 * Between these algorithms, JavaMCTS can handle deterministic actions, stochastic actions, non-discrete state spaces,
 * hidden information, and simultaneous actions. All algorithms support any number of players, configurable limits on
 * search time and number of iterations, and parallelized search (either root- or tree-parallelized, your choice) with a
 * configurable number of threads. Closed Loop MCTS supports transposition tables.
 *
 * @see com.github.wallacewatler.javamcts.MCTS
 * @see com.github.wallacewatler.javamcts.OLMCTS
 * @see com.github.wallacewatler.javamcts.MOISMCTS
 */
package com.github.wallacewatler.javamcts;
