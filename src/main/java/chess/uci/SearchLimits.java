package chess.uci;

import chess.board.Piece;
import chess.search.Search;

record SearchLimits(int depth, long timeMillis)
{
    static SearchLimits Parse(String[] tokens, int side)
    {
        int depth = 4;
        boolean explicitDepth = false;
        long white = -1, black = -1, whiteIncrement = 0, blackIncrement = 0;
        long moveTime = -1, movesToGo = 30;
        for (int i = 1; i < tokens.length; i += 2)
        {
            if (i + 1 == tokens.length)
                throw new IllegalArgumentException("Missing value for " + tokens[i]);

            long value = Long.parseLong(tokens[i + 1]);

            if (value < 0 || 86_400_000 < value)
                throw new IllegalArgumentException("Invalid go value: " + tokens[i + 1]);

            switch (tokens[i])
            {
                case "depth":
                    if (value < 1 || Search.MAX_DEPTH < value)
                        throw new IllegalArgumentException("Depth must be between 1 and " + Search.MAX_DEPTH);
                    depth = (int) value;
                    explicitDepth = true;
                    break;

                case "wtime": white = value; break;
                case "btime": black = value; break;
                case "winc": whiteIncrement = value; break;
                case "binc": blackIncrement = value; break;
                case "movetime": moveTime = value; break;
                case "movestogo":
                    if (value == 0)
                        throw new IllegalArgumentException("movestogo must be positive");
                    movesToGo = value;
                    break;
                default: throw new IllegalArgumentException("Unsupported go option: " + tokens[i]);
            }
        }
        long remaining = side == Piece.WHITE ? white : black;
        long increment = side == Piece.WHITE ? whiteIncrement : blackIncrement;
        long budget = 0;

        if (remaining >= 0)
        {
            // Leave transport/scheduling margin and do not spend the next increment in advance.
            long usable = Math.max(1, remaining - 200);
            budget = Math.min(usable, Math.max(1, Math.min(5_000, remaining / movesToGo + increment / 2)));
        }
        if (moveTime >= 0)
            budget = budget == 0 ? Math.max(1, moveTime) : Math.min(budget, Math.max(1, moveTime));

        return new SearchLimits(!explicitDepth && budget > 0 ? Search.MAX_DEPTH : depth, budget);
    }
}
