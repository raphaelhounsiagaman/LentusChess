# LentusChess

The current engine uses bitboard legal move generation, material evaluation, and
iterative deepening with alpha-beta negamax. Scores are in centipawns from the
side to move's perspective; mate scores favor shorter wins and longer losses.

## Requirements

- JDK 25 for building, testing, and running the supported release.
- Gradle 9.6.0 through the checked-in wrapper; no separate Gradle installation
  or IDE is required.
- Internet access on the first build to download Gradle and the test dependencies.

The build selects a Java 25 toolchain and targets Java 25 APIs and bytecode.
Install JDK 25 locally; automatic JDK downloads are not configured.

Run the following PowerShell commands from the repository root. If needed,
select your JDK 25 installation for the current terminal, replacing the example
path with its actual location:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-25'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

java -version
javac -version
.\gradlew.bat --version
```

## Build

From a clean checkout, compile the engine, run the tests, and package the release:

```powershell
.\gradlew.bat clean build --no-build-cache
```

The runnable JAR is `build\libs\LentusChess-0.1.1.jar`, with `chess.Main` as its
entry point. It has no external runtime dependencies, so it can be copied and
launched wherever Java 25 is installed.

## Run

Check the packaged engine's UCI handshake:

```powershell
"uci`nisready`nquit" | java -jar .\build\libs\LentusChess-0.1.1.jar
```

Expected output:

```text
id name LentusChess 0.1.1
id author Raphael Hounsiagaman
uciok
readyok
```

Start the interactive UCI console:

```powershell
java -jar .\build\libs\LentusChess-0.1.1.jar
```

Enter commands interactively:

```text
uci
isready
position startpos moves e2e4 e7e5
go depth 4
```

Wait for the final `info depth 4 ...` line and `bestmove ...`, then enter `quit`
to exit. It also accepts
`position fen <six FEN fields> moves ...`, `ucinewgame`, `stop`, and `quit`.
Bare `go` uses depth 4. Input remains responsive during search, and EOF shuts
down the worker. Incoming moves must be legal, including promotion suffixes
such as `b7b8q`; a rejected move list leaves the previous position intact.

This search supports fixed depths from 1 to 64 and UCI clock/movetime limits.
Pondering, quiescence, repetition and fifty-move draw detection, positional
evaluation, and transposition tables remain future work.

## Perft position suite

The bundled `perft-v1` suite combines the seven positions already used by the
perft tests with 43 selected castling, knight, bishop, rook, queen, pawn, and
promotion positions. Kiwipete and the rook-and-pawn endgame also exercise en
passant during traversal. Reference counts come from the
[Chess Programming Wiki](https://chessprogramming.org/Perft_Results) and
[Ethereal's standard perft suite](https://github.com/AndyGrant/Ethereal/blob/ed4fe7aac5b5d31dd9a75dc7ead3a5f6f89cce30/src/perft/standard.epd).
The resource records its sources and the selected source line numbers. Preserve
its positions, order, and counts within this version; use a new suite version
when changing the workload.

`PerftSuite.Load()` reads the packaged resource into an immutable list with
stable IDs, six-field FENs, and reference leaf counts for depths 0 through 5.
The loader rejects malformed entries, duplicate IDs or FENs, missing kings, and
invalid counts. The suite tests compare every position against `Perft` through
depth 4. This prepares the position data for the future benchmark command.

| Depth | Reference nodes across all 50 positions |
| --- | ---: |
| 1 | 832 |
| 2 | 16,095 |
| 3 | 472,525 |
| 4 | 15,072,328 |
| 5 | 574,453,633 |

## Benchmark runner

The Java API runs a suite on the calling thread and returns immutable results:

```java
var result = new PerftBenchmark().Run(PerftSuite.Load(), 4, 2, 5);
long totalNodes = result.TotalNodes();
double nodesPerSecond = result.NodesPerSecond();
```

The arguments after the suite are depth, warmup passes, and measured passes.
Depths 1 through 5 are supported; warmup may be zero and at least one measured
pass is required. Each pass visits all positions in their supplied order.
Positions are prepared before warmup. Only the perft traversals in measured
passes are timed; verification and result construction happen afterward.
Node counts and restored FEN state are checked after each traversal, including
warmup. A mismatch fails the run with the position ID, FEN, and count details.

Results contain each position's nodes and elapsed nanoseconds, each measured
pass, total nodes and time, aggregate nodes/second, and median pass time.
Rates use total nodes divided by total traversal time. Speed at the median time
uses the median pass duration; for an even number of passes, that duration is
the mean of the two middle times. A zero elapsed time produces `NaN` for the
rate.

## Benchmark command

After building, launch the standalone benchmark from PowerShell:

```powershell
java -jar .\build\libs\LentusChess-0.1.1.jar bench
java -jar .\build\libs\LentusChess-0.1.1.jar bench depth 4 warmup 2 repeat 5
java -jar .\build\libs\LentusChess-0.1.1.jar bench --help
```

Defaults are depth 4, two warmup passes, and five measured passes. Options may
appear in any order; unknown options, duplicate options, and invalid values
are rejected. For a quick correctness check, use `bench depth 1 warmup 0 repeat 1`.
This is a process argument, launched separately from the interactive UCI console.

The command prints the suite, engine and Java versions, OS, JVM arguments, and
run settings before starting. After the run it reports each measured pass,
total measured nodes and time, aggregate nodes/second, median pass time, speed
at the median time, and successful verification. Rates are rounded to whole
nodes/second; an unmeasurably short interval is shown as `n/a`. Report output is
outside the timed traversals. Depth 5 has roughly 38 times the nodes of depth 4.

Exit status is 0 on success, 2 for invalid arguments, and 1 for a suite-loading,
verification, or arithmetic failure. Errors go to standard error. Save the
console report with PowerShell redirection if desired; CSV export is a later
step. For comparisons, retain the source commit or JAR checksum alongside the
report and keep the suite, depth, Java flags, hardware, and power settings
consistent. Increase warmup if measured pass times are still improving.

## Tests

```powershell
.\gradlew.bat test
.\gradlew.bat test --tests chess.board.PerftTest
```

The suite includes reference perft positions, tactical search checks, comparison
against exhaustive minimax, UCI command handling, and search shutdown.

## Release reproducibility

The release version is defined in `build.gradle` and included in the JAR's
`Implementation-Version` manifest attribute. The UCI name reads that value;
running directly from compiled classes instead of the JAR reports
`LentusChess dev`.

JAR entries use fixed timestamps and deterministic file ordering. The Java
toolchain pins the major version to 25, but does not pin a vendor or patch
version. When saving a baseline, record the exact JDK vendor and patch version
from `java -version` and `javac -version`, the Gradle version from
`.\gradlew.bat --version`, and the source commit ID. Use the same build
environment when comparing JARs byte for byte.

Calculate the release checksum with:

```powershell
Get-FileHash .\build\libs\LentusChess-0.1.1.jar -Algorithm SHA256
```

Preserve the baseline JAR and its checksum outside `build`, which `clean`
deletes. A fresh checkout of the same baseline should pass the build, handshake,
and interactive search checks above without an IDE.

## License

This project is licensed under the MIT License.
See [LICENSE](LICENSE) for details.