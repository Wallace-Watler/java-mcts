# JavaMCTS
Several implementations of Monte Carlo Tree Search (MCTS) in Java:
- Closed Loop MCTS
- Open Loop MCTS
- Multiple-observer Information Set MCTS

Between these algorithms, JavaMCTS can handle deterministic actions, stochastic actions, non-discrete state spaces,
hidden information, and simultaneous actions. All algorithms support any number of players, configurable limits on
search time and number of iterations, and parallelized search (either root- or tree-parallelized, your choice) with a
configurable number of threads.

Example code can be found in the package `src.test.java.com.github.wallacewatler.javamcts`.

## Planned features
- Chance nodes for Closed Loop MCTS (Open Loop can be used in the meantime, but having chance nodes is nice if the
  number of possible outcomes is small)
- Transposition tables for Closed Loop MCTS
- Tools for modeling information sets
