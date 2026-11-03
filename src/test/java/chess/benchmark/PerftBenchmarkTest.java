package chess.benchmark;

import chess.board.Position;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.*;

class PerftBenchmarkTest
{
    private static final PerftSuite.Entry START = new PerftSuite.Entry("startpos", Position.STARTING_FEN,
            List.of(1L, 20L, 400L, 8902L, 197281L, 4865609L));
    private static final PerftSuite.Entry MATE = new PerftSuite.Entry("mate",
            "7k/6Q1/5K2/8/8/8/8/8 b - - 0 1", List.of(1L, 0L, 0L, 0L, 0L, 0L));

    @Test
    void warmupAndGapsBetweenTraversalsAreExcludedFromMeasuredResults()
    {
        var clock = new ScriptedClock(0, 1_000_000_000L, 8_000_000_000L, 11_000_000_000L,
                20_000_000_000L, 22_000_000_000L, 30_000_000_000L, 34_000_000_000L);
        var result = new PerftBenchmark(clock).Run(List.of(START, MATE), 1, 3, 2);

        assertEquals(8, clock.reads);
        assertEquals(1, result.depth());
        assertEquals(3, result.warmupPasses());
        assertEquals(2, result.passes().size());
        assertEquals(20, result.NodesPerPass());
        assertEquals(40, result.TotalNodes());
        assertEquals(10_000_000_000L, result.TotalNanos());
        assertEquals(4.0, result.NodesPerSecond());
        assertEquals(5_000_000_000.0, result.MedianPassNanos());
        assertEquals(4.0, result.NodesPerSecondAtMedianTime());
        assertEquals(4_000_000_000L, result.passes().getFirst().ElapsedNanos());
        assertEquals(5.0, result.passes().getFirst().NodesPerSecond());

        for (int i = 0; i < result.passes().size(); i++)
        {
            var pass = result.passes().get(i);
            assertEquals(i + 1, pass.number());
            assertEquals(List.of("startpos", "mate"),
                    pass.positions().stream().map(PerftBenchmark.PositionResult::id).toList());
            assertEquals(Position.STARTING_FEN, pass.positions().getFirst().fen());
            assertEquals(20, pass.Nodes());
        }
        assertThrows(UnsupportedOperationException.class, result.passes()::clear);
        assertThrows(UnsupportedOperationException.class, result.passes().getFirst().positions()::clear);
    }

    @Test
    void bundledSuiteRunsWithoutWarmupAndReturnsAllFiftyPositions() throws IOException
    {
        var clock = new AtomicLong();
        var result = new PerftBenchmark(clock::getAndIncrement).Run(PerftSuite.Load(), 1, 0, 1);

        assertEquals(50, result.passes().getFirst().positions().size());
        assertEquals(832, result.NodesPerPass());
        assertEquals(832, result.TotalNodes());
        assertEquals(50, result.TotalNanos());
        assertEquals(50.0, result.MedianPassNanos());
        assertEquals(100, clock.get());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void incorrectCountsFailDuringWarmupOrMeasurementWithUsefulDetails(int warmup)
    {
        var incorrect = new PerftSuite.Entry("bad-reference", Position.STARTING_FEN, List.of(1L, 21L));
        var clock = new ScriptedClock(0, 100);
        var exception = assertThrows(IllegalStateException.class,
                () -> new PerftBenchmark(clock).Run(List.of(incorrect), 1, warmup, 1));

        assertTrue(exception.getMessage().contains("bad-reference at depth 1: expected 21, actual 20"));
        assertTrue(exception.getMessage().contains(Position.STARTING_FEN));
        assertEquals(warmup == 0 ? 2 : 0, clock.reads);
    }

    @ParameterizedTest
    @CsvSource({"0, 0, 1", "-1, 0, 1", "6, 0, 1", "1, -1, 1", "1, 0, 0", "1, 0, -1"})
    void invalidSettingsAreRejectedBeforeMeasurement(int depth, int warmup, int repeat)
    {
        var clock = new ScriptedClock();
        assertThrows(IllegalArgumentException.class,
                () -> new PerftBenchmark(clock).Run(List.of(START), depth, warmup, repeat));
        assertEquals(0, clock.reads);
    }

    @Test
    void emptySuitesAndMissingReferencesAreRejectedBeforeAnyPositionIsMeasured()
    {
        var clock = new ScriptedClock();
        var benchmark = new PerftBenchmark(clock);
        assertThrows(IllegalArgumentException.class, () -> benchmark.Run(List.of(), 1, 0, 1));
        var missing = new PerftSuite.Entry("missing-reference", Position.STARTING_FEN, List.of(1L));
        var exception = assertThrows(IllegalArgumentException.class,
                () -> benchmark.Run(List.of(START, missing), 1, 0, 1));
        assertTrue(exception.getMessage().contains("missing-reference at depth 1"));
        assertEquals(0, clock.reads);
    }

    @Test
    void rateUsesTotalWorkAndTimeWithoutOverflowingIntegerCounts()
    {
        var pass = new PerftBenchmark.PassResult(1, List.of(
                new PerftBenchmark.PositionResult("a", "", 3_000_000_000L, 1_000_000_000L),
                new PerftBenchmark.PositionResult("b", "", 9_000_000_000L, 9_000_000_000L)));
        assertEquals(12_000_000_000L, pass.Nodes());
        assertEquals(10_000_000_000L, pass.ElapsedNanos());
        assertEquals(1_200_000_000.0, pass.NodesPerSecond());
        var result = new PerftBenchmark.Result(1, 0, List.of(pass, pass));
        assertEquals(24_000_000_000L, result.TotalNodes());
        assertEquals(20_000_000_000L, result.TotalNanos());
        assertEquals(1_200_000_000.0, result.NodesPerSecond());
    }

    @Test
    void medianUsesSortedPassTimesForOddAndEvenRepeatCounts()
    {
        var slow = Pass(1, 100, 5_000_000_000L);
        var fast = Pass(2, 100, 1_000_000_000L);
        var middle = Pass(3, 100, 2_000_000_000L);
        var odd = new PerftBenchmark.Result(1, 0, List.of(slow, fast, middle));
        assertEquals(2_000_000_000.0, odd.MedianPassNanos());
        assertEquals(50.0, odd.NodesPerSecondAtMedianTime());
        assertEquals(300, odd.TotalNodes());
        assertEquals(8_000_000_000L, odd.TotalNanos());
        assertEquals(37.5, odd.NodesPerSecond());

        var even = new PerftBenchmark.Result(1, 0, List.of(slow, fast));
        assertEquals(3_000_000_000.0, even.MedianPassNanos());
        assertEquals(100.0 / 3, even.NodesPerSecondAtMedianTime(), 0.000001);
    }

    @Test
    void zeroMeasuredTimeHasAnUnavailableRate()
    {
        var result = new PerftBenchmark(() -> 123L).Run(List.of(START), 1, 0, 1);
        assertEquals(20, result.TotalNodes());
        assertEquals(0, result.TotalNanos());
        assertTrue(Double.isNaN(result.NodesPerSecond()));
        assertTrue(Double.isNaN(result.NodesPerSecondAtMedianTime()));
        assertTrue(Double.isNaN(result.passes().getFirst().NodesPerSecond()));
    }

    @Test
    void resultRejectsPassesWithDifferentNodeTotals()
    {
        assertThrows(IllegalArgumentException.class, () -> new PerftBenchmark.Result(1, 0,
                List.of(Pass(1, 100, 1), Pass(2, 101, 1))));
    }

    private static PerftBenchmark.PassResult Pass(int number, long nodes, long nanos)
    {
        return new PerftBenchmark.PassResult(number,
                List.of(new PerftBenchmark.PositionResult("position", "", nodes, nanos)));
    }

    private static class ScriptedClock implements LongSupplier
    {
        private final long[] ticks;
        private int reads;

        private ScriptedClock(long... ticks)
        {
            this.ticks = ticks;
        }

        @Override
        public long getAsLong()
        {
            assertTrue(reads < ticks.length, "Unexpected timing call");
            return ticks[reads++];
        }
    }
}
