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
Each algorithm has an interface with two provided implementations, one for root-parallelized search and one for
tree-parallelized search. The names of root-parallelized variants are suffixed with `RP`, and those of tree-parallelized
are suffixed with `TP`. You can perform the search by calling `search` on any algorithm object. See the docs for further
details.

```java
// Specify time constraints, number of iterations, UCT policy, and number of threads
SearchParameters params = new SearchParameters(0, 1000, 1000000, new UCT(), 2);

MyState rootState = new MyState(/* ... */);
SearchResults results = new MCTSTP().search(2, rootState, params, new Random(), true);
MyAction best = results.bestAction();
```

### Closed Loop MCTS
This is the classic form of MCTS and is effective on deterministic games of perfect information. The entire game state
and all players' actions are visible to everyone, and every action has a pre-determined effect on the state. Examples of
games in this category are Tic-Tac-Toe (a.k.a. Naughts and Crosses), chess, and mancala.

To use Closed Loop MCTS, you'll need to implement two interfaces: `VisibleState` and `DeterministicAction`. These
represent game states and actions, respectively. You can then use the provided algorithm implementations, `MCTSRP` and
`MCTSTP`.

```java
class MyState implements VisibleState<MyState, MyAction> {
  // Your game's state here

  public int activePlayer() { /* ... */ }
  
  public List<MyAction> validActions() { /* ... */ }
  
  public double[] scores() { /* ... */ }

  public MyState copy() { /* ... */ }
}

class MyAction implements DeterministicAction<MyState> {
  // Your action data here
  
  public MyState applyToState(MyState state) { /* ... */ }
}
```

### Open Loop MCTS
This is effective on stochastic games of perfect information, games involving non-discrete states, and games of hidden
information where said information is hidden from all players. An action may involve randomness such that it can lead to
one of many possible states.

To use Open Loop MCTS, you'll need to implement two interfaces: `VisibleState` and `StochasticAction`. These represent
game states and actions, respectively. You can then use the provided algorithm implementations, `OLMCTSRP` and
`OLMCTSTP`. To handle information that is hidden from all players, you can treat its reveal as a random event; that is,
you can bake it into `StochasticAction`.

```java
class MyState implements VisibleState<MyState, MyAction> {
  // Your game's state here

  public int activePlayer() { /* ... */ }

  public List<MyAction> validActions() { /* ... */ }

  public double[] scores() { /* ... */ }

  public MyState copy() { /* ... */ }
}

class MyAction implements StochasticAction<MyState> {
  // Your action data here

  public MyState applyToState(MyState state, Random rand) { /* ... */ }
}
```

### Information Set MCTS
This is effective on games of imperfect information, where knowledge of the game state can vary between players. Each
player maintains an information set that represents this knowledge. As actions are done, knowledge may be gained from
them.

To use Information Set MCTS, you'll need to implement three interfaces: `State`, `StochasticAction`, and `InfoSet`.
These represent game states, actions, and information sets, respectively. You can then use the provided algorithm
implementations, `ISMCTSRP` and `ISMCTSTP`.

```java
class MyState implements State<MyAction> {
  // Your game's state here

  public int activePlayer() { /* ... */ }

  public List<MyAction> validActions() { /* ... */ }

  public double[] scores() { /* ... */ }
}

class MyAction implements StochasticAction<MyState> {
  // Your action data here
  
  public MyState applyToState(MyState state, Random rand) { /* ... */ }
}

class MyInfoSet implements InfoSet<MyState, MyAction> {
  // Your information set data here
  
  public int owner() { /* ... */ }
  
  public MyState determinize(Random rand) { /* ... */ }
  
  public List<MyAction> validActions() { /* ... */ }
}
```

### Multiple-observer Information Set MCTS (MO-ISMCTS)
This is effective on games of imperfect information, where knowledge of the game state can vary between players. Each
player maintains an information set that represents this knowledge. Furthermore, players do not see other players'
actions directly since certain aspects of those actions may be hidden. Instead, players observe actions as "moves,"
which are equivalence classes on actions. It is assumed that every player has full view of their own actions. Examples
of games that MO-ISMCTS can handle are hearts, cribbage, and poker.

To use MO-ISMCTS, you'll need to implement three interfaces: `State`, `ObservableAction`, and `InfoSet`. These represent
game states, actions, and information sets, respectively. You can then use the provided algorithm implementations,
`MOISMCTSRP` and `MOISMCTSTP`. Simultaneous actions can be modeled as sequential actions that are hidden from all other
players until some event reveals them at once.

```java
class MyState implements State<MyAction> {
  // Your game's state here

  public int activePlayer() { /* ... */ }

  public List<MyAction> validActions() { /* ... */ }

  public double[] scores() { /* ... */ }
}

class MyAction implements ObservableAction<MyState> {
  // Your action data here
  
  public MyState applyToState(MyState state, Random rand) { /* ... */ }
  
  public Object observe(int observer) { /* ... */ }
}

class MyInfoSet implements InfoSet<MyState, MyAction> {
  // Your information set data here
  
  public int owner() { /* ... */ }
  
  public MyState determinize(Random rand) { /* ... */ }
  
  public List<MyAction> validActions() { /* ... */ }
}
```
