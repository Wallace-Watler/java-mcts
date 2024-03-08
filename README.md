# JavaMCTS
Various implementations of Monte Carlo Tree Search (MCTS) in Java:
- Closed Loop MCTS
- Open Loop MCTS
- Multiple-observer Information Set MCTS

All algorithms support any number of players, configurable limits on search time and number of iterations, and
tree-parallelized search with a configurable number of threads.

## Algorithm details
### Closed Loop MCTS
This is classic MCTS, effective on deterministic games of perfect information. The entire game state and all players'
actions are visible to everyone, and every action has a pre-determined effect on the state. Examples of games in this
category are Tic-Tac-Toe, chess, and mancala.

#### Usage
TODO

### Open Loop MCTS
Open Loop MCTS is effective on stochastic games of perfect information, games involving non-discrete states, and games
of hidden information where said information is hidden from all players. An action may involve randomness such that it
can lead to one of many possible states. The revealing of hidden information can be treated as a random event.

#### Usage
TODO

### Multiple-observer Information Set MCTS
A variant of MCTS that is effective on games of imperfect information, where knowledge of the game state can vary
between each player. Furthermore, a player's actions may be partially or completely hidden from other players.
Simultaneous actions can be modeled as sequential actions that are hidden from all other players until some event
reveals them at once. Examples of games in this category are hearts, cribbage, and poker.

#### Usage
TODO

## Planned features
- Root parallelization
- Transposition tables for closed loop MCTS
- Tools for modeling information sets