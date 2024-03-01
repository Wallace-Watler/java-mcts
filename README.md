# JavaMCTS
Various implementations of Monte Carlo Tree Search (MCTS) in Java:
- Closed loop MCTS
- Open loop MCTS
- Multiple-observer information set MCTS

All algorithms support any number of players, configurable limits on search time and number of iterations, and
tree-parallelized search with a configurable number of threads.

## Algorithm details
### Closed loop MCTS
This is classic MCTS, effective on deterministic games of perfect information. The entire game state and all players'
actions are visible to everyone, and every action has a pre-determined effect on the state. Examples of games in this
category are Tic-Tac-Toe, chess, and mancala.

#### Usage
TODO

### Open loop MCTS
This is effective on stochastic games of perfect information or on games of hidden information where said information is
hidden from all players. An action may involve randomness such that it can lead to one of many possible states. The
revealing of hidden information can be treated as a random event. Examples of games in this category are backgammon and
solitaire.

#### Usage
TODO

### Multiple-observer information set MCTS
A variant of MCTS that is effective on games of imperfect information, where knowledge of the game state can vary
between each player. Furthermore, a player's actions may be only partially visible to other players or even completely
hidden. Simultaneous actions can be modeled as sequential actions that are hidden from all other players until some
event reveals them at once. Examples of games in this category are hearts, cribbage, and, poker.

#### Usage
TODO

## Planned features
- Seedable RNG to allow for reproducible single-threaded runs
- Chance nodes and transposition tables for closed loop MCTS
- Tree reuse for closed loop MCTS and open loop MCTS
- Tools for modeling information sets