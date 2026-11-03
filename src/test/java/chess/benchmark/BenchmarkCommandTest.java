package chess.benchmark;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BenchmarkCommandTest
{
    @Test
    void defaultsAndReorderedOptionsAreParsed()
    {
        assertEquals(new BenchmarkCommand.Options(4, 2, 5), BenchmarkCommand.Parse(new String[0]));
        assertEquals(new BenchmarkCommand.Options(5, 0, 3),
                BenchmarkCommand.Parse(new String[]{"repeat", "3", "warmup", "0", "depth", "5"}));
    }

    @ParameterizedTest
    @ValueSource(strings = {"depth", "depth 0", "depth 6", "depth -1", "depth five", "depth 2147483648",
            "warmup -1", "repeat 0", "repeat -1", "depth 2 depth 3", "unknown 1", "repeat 2 stray",
            "--help depth 1"})
    void invalidArgumentsReturnUsageWithoutStartingABenchmark(String command)
    {
        var output = new ByteArrayOutputStream();
        var errors = new ByteArrayOutputStream();
        assertEquals(2, BenchmarkCommand.Run(command.split(" "), Stream(output), Stream(errors)));
        assertEquals(0, output.size());
        assertTrue(Text(errors).contains("Benchmark error:"));
        assertTrue(Text(errors).contains("Usage: bench"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"help", "--help"})
    void helpFinishesWithoutRunningTheSuite(String option)
    {
        var output = new ByteArrayOutputStream();
        var errors = new ByteArrayOutputStream();
        assertEquals(0, BenchmarkCommand.Run(new String[]{option}, Stream(output), Stream(errors)));
        assertTrue(Text(output).contains("Defaults: depth 4, warmup 2, repeat 5."));
        assertFalse(Text(output).contains("Running"));
        assertEquals(0, errors.size());
    }

    @Test
    void commandRunsTheEntireSuiteAndReportsOnlyMeasuredNodes()
    {
        var output = new ByteArrayOutputStream();
        var errors = new ByteArrayOutputStream();
        int status = BenchmarkCommand.Run(new String[]{"depth", "1", "warmup", "1", "repeat", "2"},
                Stream(output), Stream(errors));
        assertEquals(0, status, Text(errors));
        String report = Text(output);
        assertTrue(report.contains("Suite: perft-v1"));
        assertTrue(report.contains("Positions: 50"));
        assertTrue(report.contains("Java: "));
        assertTrue(report.contains("JVM arguments: "));
        assertTrue(report.contains("Warmup passes: 1"));
        assertTrue(report.contains("Pass 1/2: nodes=832"));
        assertTrue(report.contains("Pass 2/2: nodes=832"));
        assertTrue(report.contains("Nodes per pass: 832"));
        assertTrue(report.contains("Total measured nodes: 1664"));
        assertTrue(report.contains("Verification: 50/50 passed on every pass"));
        assertEquals(0, errors.size());
    }

    @Test
    void reportUsesAggregateRateAndMedianDuration()
    {
        var slow = new PerftBenchmark.PassResult(1,
                List.of(new PerftBenchmark.PositionResult("a", "", 100, 5_000_000_000L)));
        var fast = new PerftBenchmark.PassResult(2,
                List.of(new PerftBenchmark.PositionResult("a", "", 100, 1_000_000_000L)));
        var middle = new PerftBenchmark.PassResult(3,
                List.of(new PerftBenchmark.PositionResult("a", "", 100, 2_000_000_000L)));
        var output = new ByteArrayOutputStream();
        BenchmarkCommand.PrintReport(new PerftBenchmark.Result(1, 2, List.of(slow, fast, middle)), Stream(output));
        String report = Text(output);
        assertTrue(report.contains("Pass 1/3: nodes=100 time_ms=5000.000 nps=20"));
        assertTrue(report.contains("Total measured nodes: 300"));
        assertTrue(report.contains("Total measured time: 8000.000 ms"));
        assertTrue(report.contains("Aggregate speed: 38 nodes/s"));
        assertTrue(report.contains("Median pass time: 2000.000 ms"));
        assertTrue(report.contains("Speed at median pass time: 50 nodes/s"));
    }

    @Test
    void zeroDurationIsReportedAsUnavailable()
    {
        var pass = new PerftBenchmark.PassResult(1,
                List.of(new PerftBenchmark.PositionResult("a", "", 100, 0)));
        var output = new ByteArrayOutputStream();
        BenchmarkCommand.PrintReport(new PerftBenchmark.Result(1, 0, List.of(pass)), Stream(output));
        String report = Text(output);
        assertTrue(report.contains("nps=n/a"));
        assertTrue(report.contains("Aggregate speed: n/a nodes/s"));
        assertTrue(report.contains("Speed at median pass time: n/a nodes/s"));
        assertFalse(report.contains("NaN"));
        assertFalse(report.contains("Infinity"));
    }

    private static PrintStream Stream(ByteArrayOutputStream bytes)
    {
        return new PrintStream(bytes, true, StandardCharsets.UTF_8);
    }

    private static String Text(ByteArrayOutputStream bytes)
    {
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
