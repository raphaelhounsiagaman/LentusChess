package chess.search;

import chess.board.DrawReason;
import chess.board.Move;
import chess.board.MoveGenerator;
import chess.board.Piece;
import chess.board.Position;
import chess.eval.Evaluator;

import java.util.Collections;
import java.util.List;

public class Search
{
    public static final int MATE_SCORE = 30_000;
    public static final int MAX_DEPTH = 64;
    private static final int INFINITY = 32_000;
    private static final int MAX_QUIESCENCE_PLY = 64;
    // Quiescence checks for checkmate at its final ply before enforcing the cap.
    private static final int MIN_MATE_SCORE = MATE_SCORE - (MAX_DEPTH + MAX_QUIESCENCE_PLY);
    private static final int TRANSPOSITION_CAPACITY = 16_384;

    private final MoveGenerator m_MoveGenerator = new MoveGenerator();
    private final Evaluator m_Evaluator = new Evaluator();
    private final TranspositionTable m_TranspositionTable;
    private volatile boolean m_Stop;
    private long m_Nodes;
    private long m_Started;
    private long m_BudgetNanos;

    /** historyDependent marks scores that used a repetition or fifty-move draw during search. */
    public record Result(int bestMove, int score, int depth, long nodes, boolean historyDependent)
    {
    }

    private record Score(int value, boolean historyDependent)
    {
    }

    public Search()
    {
        this(new TranspositionTable(TRANSPOSITION_CAPACITY));
    }

    Search(TranspositionTable table)
    {
        m_TranspositionTable = table;
    }

    public Result FindBestMove(Position position, int maxDepth)
    {
        return FindBestMove(position, maxDepth, 0);
    }

    public Result FindBestMove(Position position, int maxDepth, long timeMillis)
    {
        if (maxDepth < 1 || maxDepth > MAX_DEPTH)
            throw new IllegalArgumentException("Search depth must be between 1 and " + MAX_DEPTH);

        if (timeMillis < 0 || 86_400_000 < timeMillis)
            throw new IllegalArgumentException("Search time must be between 0 and 86400000 milliseconds");

        m_Started = System.nanoTime();
        m_BudgetNanos = timeMillis * 1_000_000;
        m_Nodes = 0;

        List<Integer> moves = m_MoveGenerator.GenerateLegalMoves(position);
        if (moves.isEmpty())
            return new Result(Move.NONE, TerminalScore(position, 0), 0, 1, false);

        DrawReason draw = position.GetDrawReason();
        if (draw != DrawReason.NONE)
            return new Result(moves.getFirst(), 0, 0, 1, draw.IsHistoryDependent());

        // Keep a legal fallback even if stop arrives before the first iteration completes.
        Result best = new Result(moves.getFirst(), m_Evaluator.Evaluate(position), 0, 0, false);
        MoveOrdering.Order(position, moves);
        for (int depth = 1; depth <= maxDepth && !ShouldStop(); depth++)
        {
            Result iteration = SearchRoot(position, moves, depth);
            if (ShouldStop())
                break;
            best = iteration;
            Collections.swap(moves, 0, moves.indexOf(best.bestMove()));
        }
        return new Result(best.bestMove(), best.score(), best.depth(), m_Nodes, best.historyDependent());
    }

    public void Stop()
    {
        m_Stop = true;
    }

    private boolean ShouldStop()
    {
        if (m_BudgetNanos > 0 && System.nanoTime() - m_Started >= m_BudgetNanos)
            m_Stop = true;
        return m_Stop;
    }

    private Result SearchRoot(Position position, List<Integer> moves, int depth)
    {
        m_Nodes++;
        int bestMove = moves.getFirst();
        int alpha = -INFINITY;
        boolean historyDependent = false;
        Position.UndoState undo = new Position.UndoState();
        for (int move : moves)
        {
            if (ShouldStop())
                break;

            position.MakeMove(move, undo);
            Score child = Negamax(position, depth - 1, -INFINITY, -alpha, 1);
            position.UnmakeMove(undo);
            int score = -child.value();
            historyDependent |= child.historyDependent();

            if (score > alpha)
            {
                alpha = score;
                bestMove = move;
            }
        }
        return new Result(bestMove, alpha, depth, m_Nodes, historyDependent);
    }

    private Score Negamax(Position position, int depth, int alpha, int beta, int ply)
    {
        if (depth == 0)
            return Quiescence(position, alpha, beta, ply, 0);
        if (ShouldStop())
            return new Score(0, false);
        m_Nodes++;

        List<Integer> moves = m_MoveGenerator.GenerateLegalMoves(position);
        if (moves.isEmpty())
            return new Score(TerminalScore(position, ply), false);
        // Draw adjudication is separate from static evaluation and any future evaluation cache.
        DrawReason draw = position.GetDrawReason();
        if (draw != DrawReason.NONE)
            return new Score(0, draw.IsHistoryDependent());

        long key = position.GetPositionKey();
        MoveOrdering.Order(position, moves, m_TranspositionTable.ProbeMove(key));
        int best = -INFINITY;
        int bestMove = Move.NONE;
        boolean historyDependent = false;
        Position.UndoState undo = new Position.UndoState();
        for (int move : moves)
        {
            if (ShouldStop())
                return new Score(0, false);

            position.MakeMove(move, undo);
            Score child = Negamax(position, depth - 1, -beta, -alpha, ply + 1);
            position.UnmakeMove(undo);
            if (ShouldStop())
                return new Score(0, false);
            int score = -child.value();
            // Rejected moves and cutoff bounds can also depend on a history-based draw.
            historyDependent |= child.historyDependent();

            if (score > best)
            {
                best = score;
                bestMove = move;
            }
            alpha = Math.max(alpha, score);

            if (alpha >= beta)
                break;
        }
        m_TranspositionTable.StoreMove(key, depth, bestMove);
        return new Score(best, historyDependent);
    }

    private Score Quiescence(Position position, int alpha, int beta, int ply, int qply)
    {
        if (ShouldStop())
            return new Score(0, false);
        m_Nodes++;

        List<Integer> moves = m_MoveGenerator.GenerateLegalMoves(position);
        if (moves.isEmpty())
            return new Score(TerminalScore(position, ply), false);
        DrawReason draw = position.GetDrawReason();
        if (draw != DrawReason.NONE)
            return new Score(0, draw.IsHistoryDependent());

        // Abort the iteration instead of assigning a score to an unresolved line.
        // In particular, a checked position must never become a stand-pat leaf.
        if (qply >= MAX_QUIESCENCE_PLY)
        {
            m_Stop = true;
            return new Score(0, false);
        }

        boolean inCheck = m_MoveGenerator.IsInCheck(position, position.GetSideToMove());
        int best = -INFINITY;
        if (!inCheck)
        {
            best = m_Evaluator.Evaluate(position);
            if (best >= beta)
                return new Score(best, false);
            alpha = Math.max(alpha, best);
        }

        MoveOrdering.Order(position, moves);
        boolean historyDependent = false;
        Position.UndoState undo = new Position.UndoState();
        for (int move : moves)
        {
            if (ShouldStop())
                return new Score(0, false);
            int type = Move.GetType(move);
            if (!inCheck && type != Move.EN_PASSANT && type < Move.PROMOTE_KNIGHT
                    && position.GetPiece(Move.GetTo(move)) == Piece.NONE)
                continue;

            position.MakeMove(move, undo);
            Score child = Quiescence(position, -beta, -alpha, ply + 1, qply + 1);
            position.UnmakeMove(undo);
            if (ShouldStop())
                return new Score(0, false);
            int score = -child.value();
            historyDependent |= child.historyDependent();

            best = Math.max(best, score);
            alpha = Math.max(alpha, score);
            if (alpha >= beta)
                break;
        }
        return new Score(best, historyDependent);
    }

    private int TerminalScore(Position position, int ply)
    {
        return m_MoveGenerator.IsInCheck(position, position.GetSideToMove()) ? -MATE_SCORE + ply : 0;
    }

    /** Converts a root-relative search score to a position-relative table score. */
    static int ToTableScore(int score, int ply)
    {
        return AdjustMateScore(score, ply);
    }

    /** Converts a position-relative table score to a score relative to the probing root. */
    static int FromTableScore(int score, int ply)
    {
        return AdjustMateScore(score, -ply);
    }

    private static int AdjustMateScore(int score, int adjustment)
    {
        if (MIN_MATE_SCORE <= score && score <= MATE_SCORE)
            return score + adjustment;
        if (-MATE_SCORE <= score && score <= -MIN_MATE_SCORE)
            return score - adjustment;
        return score;
    }
}
