# LentusChess

The current engine uses bitboard legal move generation, material evaluation, and
iterative deepening with alpha-beta negamax. Scores are in centipawns from the
side to move's perspective; mate scores favor shorter wins and longer losses.

## Run

Compile and start the UCI console (Java 21 or newer):

```powershell
.\gradlew.bat classes
java -cp build/classes/java/main chess.Main
```

Enter commands interactively:

```text
uci
isready
position startpos moves e2e4 e7e5
go depth 4
```

The engine prints a final `info` line and `bestmove`. It also accepts
`position fen <six FEN fields> moves ...`, `ucinewgame`, `stop`, and `quit`.
Bare `go` uses depth 4. Input remains responsive during search, and EOF shuts
down the worker. Incoming moves must be legal, including promotion suffixes
such as `b7b8q`; a rejected move list leaves the previous position intact.

This search supports fixed depths from 1 to 64 and UCI clock/movetime limits.
Pondering, quiescence, repetition and fifty-move draw detection, positional
evaluation, and transposition tables remain future work.

## Tests

```powershell
.\gradlew.bat test
.\gradlew.bat test --tests chess.board.PerftTest
```

The suite includes reference perft positions, tactical search checks, comparison
against exhaustive minimax, UCI command handling, and search shutdown.
