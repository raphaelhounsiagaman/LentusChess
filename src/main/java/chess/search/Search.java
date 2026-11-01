package chess.search;

import chess.board.Move;
import chess.board.MoveGenerator;
import chess.board.Position;
import chess.eval.Evaluator;

import java.util.Collections;
import java.util.List;

public class Search
{
    public static final int MATE_SCORE = 30_000;
    public static final int MAX_DEPTH = 64;
    private static final int INFINITY = 32_000;

    private final MoveGenerator m_MoveGenerator = new MoveGenerator();
    private final Evaluator m_Evaluator = new Evaluator();
    private volatile boolean m_Stop;
    private long m_Nodes;

    public record Result(int bestMove, int score, int depth, long nodes)
    {
    }

    public Result FindBestMove(Position position, int maxDepth)
    {
        if (maxDepth < 1 || maxDepth > MAX_DEPTH)
            throw new IllegalArgumentException("Search depth must be between 1 and " + MAX_DEPTH);

        m_Nodes = 0;
        List<Integer> moves = m_MoveGenerator.GenerateLegalMoves(position);
        if (moves.isEmpty())
            return new Result(Move.NONE, TerminalScore(position, 0), 0, 1);

        // Keep a legal fallback even if stop arrives before the first iteration completes.
        Result best = new Result(moves.getFirst(), m_Evaluator.Evaluate(position), 0, 0);
        for (int depth = 1; depth <= maxDepth && !m_Stop; depth++)
        {
            Result iteration = SearchRoot(position, moves, depth);
            if (m_Stop)
                break;
            best = iteration;
            Collections.swap(moves, 0, moves.indexOf(best.bestMove()));
        }
        return new Result(best.bestMove(), best.score(), best.depth(), m_Nodes);
    }

    public void Stop()
    {
        m_Stop = true;
    }

    private Result SearchRoot(Position position, List<Integer> moves, int depth)
    {
        m_Nodes++;
        int bestMove = moves.getFirst();
        int alpha = -INFINITY;
        Position.UndoState undo = new Position.UndoState();
        for (int move : moves)
        {
            if (m_Stop)
                break;

            position.MakeMove(move, undo);
            int score = -Negamax(position, depth - 1, -INFINITY, -alpha, 1);;
            position.UnmakeMove(undo);

            if (score > alpha)
            {
                alpha = score;
                bestMove = move;
            }
        }
        return new Result(bestMove, alpha, depth, m_Nodes);
    }

    private int Negamax(Position position, int depth, int alpha, int beta, int ply)
    {
        if (m_Stop)
            return 0;
        m_Nodes++;

        List<Integer> moves = m_MoveGenerator.GenerateLegalMoves(position);
        if (moves.isEmpty())
            return TerminalScore(position, ply);
        if (depth == 0)
            return m_Evaluator.Evaluate(position);

        int best = -INFINITY;
        Position.UndoState undo = new Position.UndoState();
        for (int move : moves)
        {
            if (m_Stop)
                return 0;

            position.MakeMove(move, undo);
            int score = -Negamax(position, depth - 1, -beta, -alpha, ply + 1);
            position.UnmakeMove(undo);

            best = Math.max(best, score);
            alpha = Math.max(alpha, score);

            if (alpha >= beta)
                break;
        }
        return best;
    }

    private int TerminalScore(Position position, int ply)
    {
        return m_MoveGenerator.IsInCheck(position, position.GetSideToMove()) ? -MATE_SCORE + ply : 0;
    }
}
