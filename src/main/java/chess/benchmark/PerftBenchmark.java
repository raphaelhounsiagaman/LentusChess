package chess.benchmark;

import chess.board.Perft;
import chess.board.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;

public final class PerftBenchmark
{
    private final LongSupplier m_NanoTime;

    public PerftBenchmark()
    {
        this(System::nanoTime);
    }

    // A controllable clock lets tests check the accounting without wall-clock timing assertions.
    PerftBenchmark(LongSupplier nanoTime)
    {
        m_NanoTime = Objects.requireNonNull(nanoTime);
    }

    public Result Run(List<PerftSuite.Entry> suite, int depth, int warmupPasses, int measuredPasses)
    {
        ValidateSettings(depth, warmupPasses, measuredPasses);
        if (suite.isEmpty())
            throw new IllegalArgumentException("Benchmark suite is empty");

        List<PreparedPosition> positions = new ArrayList<>();
        for (PerftSuite.Entry entry : List.copyOf(suite))
        {
            if (entry.nodes().size() <= depth || entry.ExpectedNodes(depth) < 0)
                throw new IllegalArgumentException("Missing or invalid reference count for " + entry.id()
                        + " at depth " + depth);
            Position position = new Position();
            position.SetPosition(entry.fen());
            positions.add(new PreparedPosition(entry, position, position.ToFen()));
        }

        Perft perft = new Perft();
        for (int pass = 0; pass < warmupPasses; pass++)
            for (PreparedPosition position : positions)
                Verify(position, depth, perft.Count(position.position(), depth));

        List<PassResult> passes = new ArrayList<>();
        for (int pass = 0; pass < measuredPasses; pass++)
        {
            List<PositionResult> measurements = new ArrayList<>();
            for (PreparedPosition position : positions)
            {
                long start = m_NanoTime.getAsLong();
                long nodes = perft.Count(position.position(), depth);
                long elapsed = m_NanoTime.getAsLong() - start;

                Verify(position, depth, nodes);
                measurements.add(new PositionResult(position.entry().id(), position.entry().fen(), nodes, elapsed));
            }
            passes.add(new PassResult(pass + 1, measurements));
        }
        return new Result(depth, warmupPasses, passes);
    }

    private static void ValidateSettings(int depth, int warmupPasses, int measuredPasses)
    {
        if (depth < 1 || depth > PerftSuite.MAX_DEPTH)
            throw new IllegalArgumentException("Benchmark depth must be between 1 and " + PerftSuite.MAX_DEPTH);
        if (warmupPasses < 0)
            throw new IllegalArgumentException("Warmup passes must be non-negative");
        if (measuredPasses < 1)
            throw new IllegalArgumentException("Measured passes must be positive");
    }

    private static void Verify(PreparedPosition position, int depth, long nodes)
    {
        PerftSuite.Entry entry = position.entry();
        if (nodes != entry.ExpectedNodes(depth))
            throw new IllegalStateException("Perft mismatch for " + entry.id() + " at depth " + depth
                    + ": expected " + entry.ExpectedNodes(depth) + ", actual " + nodes
                    + "\nFEN: " + entry.fen());
        if (!position.position().ToFen().equals(position.initialFen()))
            throw new IllegalStateException("Perft did not restore " + entry.id() + "\nFEN: " + entry.fen());
    }

    private static double Rate(long nodes, double nanos)
    {
        return nanos > 0 ? nodes * 1_000_000_000.0 / nanos : Double.NaN;
    }

    private record PreparedPosition(PerftSuite.Entry entry, Position position, String initialFen)
    {
    }

    public record PositionResult(String id, String fen, long nodes, long elapsedNanos)
    {
        public PositionResult
        {
            if (nodes < 0 || elapsedNanos < 0)
                throw new IllegalArgumentException("Node counts and elapsed time must be non-negative");
        }
    }

    public record PassResult(int number, List<PositionResult> positions)
    {
        public PassResult
        {
            positions = List.copyOf(positions);
            if (number < 1 || positions.isEmpty())
                throw new IllegalArgumentException("A measured pass needs a positive number and at least one position");
        }

        public long Nodes()
        {
            return positions.stream().mapToLong(PositionResult::nodes).reduce(0L, Math::addExact);
        }

        public long ElapsedNanos()
        {
            return positions.stream().mapToLong(PositionResult::elapsedNanos).reduce(0L, Math::addExact);
        }

        /** Returns total nodes divided by total traversal time, or NaN if the time is zero. */
        public double NodesPerSecond()
        {
            return Rate(Nodes(), ElapsedNanos());
        }
    }

    public record Result(int depth, int warmupPasses, List<PassResult> passes)
    {
        public Result
        {
            passes = List.copyOf(passes);
            ValidateSettings(depth, warmupPasses, passes.size());
            long nodes = passes.getFirst().Nodes();
            if (passes.stream().anyMatch(pass -> pass.Nodes() != nodes))
                throw new IllegalArgumentException("Every measured pass must have the same node count");
        }

        public long NodesPerPass()
        {
            return passes.getFirst().Nodes();
        }

        public long TotalNodes()
        {
            return passes.stream().mapToLong(PassResult::Nodes).reduce(0L, Math::addExact);
        }

        public long TotalNanos()
        {
            return passes.stream().mapToLong(PassResult::ElapsedNanos).reduce(0L, Math::addExact);
        }

        /** Returns the aggregate rate across measured passes, or NaN if the time is zero. */
        public double NodesPerSecond()
        {
            return Rate(TotalNodes(), TotalNanos());
        }

        public double MedianPassNanos()
        {
            long[] times = passes.stream().mapToLong(PassResult::ElapsedNanos).sorted().toArray();
            int middle = times.length / 2;
            return times.length % 2 == 1 ? times[middle] : times[middle - 1] / 2.0 + times[middle] / 2.0;
        }

        /** With even repeat counts, this uses the mean of the two middle pass times. */
        public double NodesPerSecondAtMedianTime()
        {
            return Rate(NodesPerPass(), MedianPassNanos());
        }
    }
}
