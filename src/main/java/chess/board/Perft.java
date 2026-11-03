package chess.board;

import java.util.List;

public final class Perft
{
    private final MoveGenerator m_MoveGenerator = new MoveGenerator();

    /**
     * Counts leaf nodes at exactly the requested depth, restoring the supplied position afterward.
     * Depth zero counts one leaf, including in terminal positions. At depth one, legal moves
     * are counted directly without recursing into each leaf.
     *
     * @throws IllegalArgumentException if depth is negative
     */
    public long Count(Position position, int depth)
    {
        if (depth < 0)
            throw new IllegalArgumentException("Perft depth must be non-negative");
        return CountNodes(position, depth);
    }

    private long CountNodes(Position position, int depth)
    {
        if (depth == 0)
            return 1;
        List<Integer> moves = m_MoveGenerator.GenerateLegalMoves(position);
        if (depth == 1)
            return moves.size();

        long nodes = 0;
        Position.UndoState undo = new Position.UndoState();
        for (int move : moves)
        {
            position.MakeMove(move, undo);
            try
            {
                nodes += CountNodes(position, depth - 1);
            }
            finally
            {
                position.UnmakeMove(undo);
            }
        }
        return nodes;
    }
}
