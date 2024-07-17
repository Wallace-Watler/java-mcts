# JavaMCTS
Several implementations of Monte Carlo Tree Search (MCTS) in Java:
- Closed Loop MCTS
- Open Loop MCTS
- Information Set MCTS
- Multiple-observer Information Set MCTS

Between these algorithms, JavaMCTS can handle deterministic actions, stochastic actions, non-discrete state spaces,
hidden information, and simultaneous actions. All algorithms support any number of players, configurable limits on
search time and number of iterations, and parallelized search (either root- or tree-parallelized, your choice) with a
configurable number of threads. Closed Loop MCTS supports transposition tables.

Example code can be found in the package `src.test.java.com.github.wallacewatler.javamcts`.

## Planned features
- Chance nodes for Closed Loop MCTS (Open Loop can be used in the meantime, but having chance nodes is nice if the
  number of possible outcomes is small)

## Usage
### Closed Loop MCTS
This is the classic form of MCTS and is effective on deterministic games of perfect information. The entire game state
and all players' actions are visible to everyone, and every action has a pre-determined effect on the state. Examples of
games in this category are Tic-Tac-Toe (a.k.a. Naughts and Crosses), chess, and mancala.

To use Closed Loop MCTS, you'll need to implement two interfaces: `VisibleState` and `DeterministicAction`. You can then
perform the search by calling `search` on one of the provided `MCTS` implementations: `MCTSRP` or `MCTSTP` for
root-parallelized or tree-parallelized search, respectively.

```java
class MyState implements VisibleState<MyState, MyAction> {
  // Your game's state here

  public List<MyAction> validActions() {
    return /* the available actions in this state */;
  }

  public MyState copy() {
    return /* a copy of this state */;
  }
}

class MyAction implements DeterministicAction<MyState> {
  // Action data here
  
  public MyState applyToState(MyState state) {
    return /* the updated state */;
  }
}

// Specify time constraints, iterations, UCT policy, and number of threads
SearchParameters params = new SearchParameters(0, 1000, 1000000, new UCT(), 2);
MyState rootState = new MyState(/* ... */);
SearchResults results = new MCTSTP().search(2, rootState, params, new Random(), true);
MyAction best = results.bestAction();
```

### Open Loop MCTS
TODO

### Information Set MCTS
TODO

### Multiple-observer Information Set MCTS
TODO
