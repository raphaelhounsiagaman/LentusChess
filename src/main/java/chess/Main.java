package chess;

import chess.benchmark.BenchmarkCommand;
import chess.uci.UCI;

import java.util.Arrays;

public class Main
{
    public static void main(String[] args)
    {
        if (args.length > 0 && args[0].equals("bench"))
        {
            int status = BenchmarkCommand.Run(Arrays.copyOfRange(args, 1, args.length), System.out, System.err);
            if (status != 0)
                System.exit(status);
            return;
        }
        Engine engine = new Engine();
        UCI uci = new UCI(engine);
        uci.run();
    }
}
