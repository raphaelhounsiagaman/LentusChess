package chess.benchmark;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Locale;

public final class BenchmarkCommand
{
    private static final String USAGE = "bench [depth 1-" + PerftSuite.MAX_DEPTH
            + "] [warmup <passes>] [repeat <passes>]";

    private BenchmarkCommand()
    {
    }

    public static int Run(String[] args, PrintStream output, PrintStream errors)
    {
        if (args.length == 1 && (args[0].equals("help") || args[0].equals("--help")))
        {
            output.println(USAGE);
            output.println("Defaults: depth 4, warmup 2, repeat 5.");
            output.flush();
            return 0;
        }

        Options options;
        try
        {
            options = Parse(args);
        }
        catch (IllegalArgumentException exception)
        {
            errors.println("Benchmark error: " + exception.getMessage());
            errors.println("Usage: " + USAGE);
            errors.flush();
            return 2;
        }

        try
        {
            var suite = PerftSuite.Load();
            String version = BenchmarkCommand.class.getPackage().getImplementationVersion();
            output.println("Engine: LentusChess " + (version == null ? "dev" : version));
            output.println("Suite: " + PerftSuite.VERSION);
            output.println("Positions: " + suite.size());
            output.println("Mode: single-threaded perft, depth-one bulk count");
            output.println("Java: " + System.getProperty("java.vendor") + " " + System.getProperty("java.runtime.version"));
            output.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
            output.println("JVM arguments: " + String.join(" ", ManagementFactory.getRuntimeMXBean().getInputArguments()));
            output.println("Depth: " + options.depth());
            output.println("Warmup passes: " + options.warmup());
            output.println("Measured passes: " + options.repeat());
            output.println("Running warmup and measurement...");
            output.flush();

            var result = new PerftBenchmark().Run(suite, options.depth(), options.warmup(), options.repeat());
            PrintReport(result, output);
            output.println("Verification: " + suite.size() + "/" + suite.size() + " passed on every pass");
            output.flush();
            return 0;
        }
        catch (IOException | IllegalStateException | ArithmeticException exception)
        {
            errors.println("Benchmark failed: " + exception.getMessage());
            errors.flush();
            return 1;
        }
    }

    static Options Parse(String[] args)
    {
        int depth = 4;
        int warmup = 2;
        int repeat = 5;
        var seen = new HashSet<String>();
        for (int i = 0; i < args.length; i += 2)
        {
            String option = args[i];
            if (!seen.add(option))
                throw new IllegalArgumentException("Duplicate benchmark option: " + option);
            if (i + 1 == args.length)
                throw new IllegalArgumentException("Missing value for benchmark option: " + option);
            try
            {
                switch (option)
                {
                    case "depth" -> depth = Integer.parseInt(args[i + 1]);
                    case "warmup" -> warmup = Integer.parseInt(args[i + 1]);
                    case "repeat" -> repeat = Integer.parseInt(args[i + 1]);
                    default -> throw new IllegalArgumentException("Unknown benchmark option: " + option);
                }
            }
            catch (NumberFormatException exception)
            {
                throw new IllegalArgumentException("Expected an integer for " + option + ": " + args[i + 1]);
            }
        }
        return new Options(depth, warmup, repeat);
    }

    static void PrintReport(PerftBenchmark.Result result, PrintStream output)
    {
        for (var pass : result.passes())
            output.printf(Locale.ROOT, "Pass %d/%d: nodes=%d time_ms=%.3f nps=%s%n",
                    pass.number(), result.passes().size(), pass.Nodes(), pass.ElapsedNanos() / 1_000_000.0,
                    FormatRate(pass.NodesPerSecond()));
        output.println("Nodes per pass: " + result.NodesPerPass());
        output.println("Total measured nodes: " + result.TotalNodes());
        output.printf(Locale.ROOT, "Total measured time: %.3f ms%n", result.TotalNanos() / 1_000_000.0);
        output.println("Aggregate speed: " + FormatRate(result.NodesPerSecond()) + " nodes/s");
        output.printf(Locale.ROOT, "Median pass time: %.3f ms%n", result.MedianPassNanos() / 1_000_000.0);
        output.println("Speed at median pass time: " + FormatRate(result.NodesPerSecondAtMedianTime()) + " nodes/s");
    }

    private static String FormatRate(double rate)
    {
        return Double.isFinite(rate) ? String.format(Locale.ROOT, "%.0f", rate) : "n/a";
    }

    record Options(int depth, int warmup, int repeat)
    {
        Options
        {
            if (depth < 1 || depth > PerftSuite.MAX_DEPTH)
                throw new IllegalArgumentException("Benchmark depth must be between 1 and " + PerftSuite.MAX_DEPTH);
            if (warmup < 0)
                throw new IllegalArgumentException("Warmup passes must be non-negative");
            if (repeat < 1)
                throw new IllegalArgumentException("Measured passes must be positive");
        }
    }
}
