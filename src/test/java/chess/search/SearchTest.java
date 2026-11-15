package chess.search;

import chess.board.DrawReason;
import chess.board.Move;
import chess.board.MoveGenerator;
import chess.board.Piece;
import chess.board.Position;
import chess.eval.Evaluator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchTest
{
    private final MoveGenerator generator = new MoveGenerator();

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "4k3/8/8/8/8/8/q7/R3K3 w - - 0 1 | a1a2",
            "r3k3/Q7/8/8/8/8/8/4K3 b - - 0 1 | a8a7"
    })
    void takesAnUndefendedQueenForEitherColor(String fen, String expectedMove)
    {
        Position position = PositionFrom(fen);
        Search.Result result = new Search().FindBestMove(position, 2);
        assertEquals(expectedMove, Move.ToUci(result.bestMove()));
        assertEquals(500, result.score());
        assertEquals(2, result.depth());
        assertTrue(result.nodes() > 0);
        AssertPosition(fen, position);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3})
    void findsMateIncludingAtTheEvaluationHorizon(int depth)
    {
        String fen = "7k/5Q2/6K1/8/8/8/8/8 w - - 0 1";
        Position position = PositionFrom(fen);
        Search.Result result = new Search().FindBestMove(position, depth);
        assertEquals(Search.MATE_SCORE - 1, result.score());
        AssertPosition(fen, position);

        position.MakeMove(result.bestMove(), new Position.UndoState());
        assertTrue(generator.IsInCheck(position, Piece.BLACK));
        assertTrue(generator.GenerateLegalMoves(position).isEmpty());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "7k/6Q1/5K2/8/8/8/8/8 b - - 0 1 | -30000",
            "7k/5Q2/5K2/8/8/8/8/8 b - - 0 1 | 0"
    })
    void distinguishesCheckmateFromStalemate(String fen, int score)
    {
        Position position = PositionFrom(fen);
        Search.Result result = new Search().FindBestMove(position, 3);
        assertEquals(Move.NONE, result.bestMove());
        assertEquals(score, result.score());
        AssertPosition(fen, position);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void avoidsAPoisonedPawnEvenAtDepthOne(int depth)
    {
        String fen = "3r2k1/8/8/3p4/8/8/8/3R2K1 w - - 0 1";
        Position position = PositionFrom(fen);
        Search.Result result = new Search().FindBestMove(position, depth);
        assertNotEquals("d1d5", Move.ToUci(result.bestMove()));
        assertEquals(-100, result.score());
        AssertPosition(fen, position);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            // A rook recaptures the queen that has just taken on d5.
            "6k1/8/8/3q4/8/8/8/3R2K1 w - - 0 1 | 500",
            // En passant is a capture despite its empty destination.
            "4k3/8/8/8/3pP3/8/8/K7 b - e3 0 1 | 100",
            // Quiet promotions must be searched, including underpromotions.
            "8/1P6/7k/8/8/8/8/K7 w - - 0 1 | 900",
            // Only quiet queen blocks evade check, followed by Rxa3#; stand pat is +400.
            "r7/8/8/8/8/7Q/2k5/K7 w - - 0 1 | -29997",
            "7k/6Q1/5K2/8/8/8/8/8 b - - 0 1 | -29999",
            "7k/5Q2/5K2/8/8/8/8/8 b - - 0 1 | 0"
    })
    void resolvesTacticsAndTerminalsAtTheHorizon(String fen, int expected) throws Exception
    {
        Position position = PositionFrom(fen);
        // Enter the actual depth-zero path with a full window, independent of root choices.
        var negamax = Search.class.getDeclaredMethod("Negamax", Position.class,
                int.class, int.class, int.class, int.class);
        negamax.setAccessible(true);
        Object score = negamax.invoke(new Search(), position, 0, -32000, 32000, 1);
        var value = score.getClass().getDeclaredMethod("value");
        value.setAccessible(true);
        assertEquals(expected, value.invoke(score));
        AssertPosition(fen, position);
    }

    @Test
    void countsQuiescenceNodesWithoutCountingQuietLeavesTwice()
    {
        Position quiet = new Position();
        assertEquals(21, new Search().FindBestMove(quiet, 1).nodes());
        Position tactical = PositionFrom("3r2k1/8/8/3p4/8/8/8/3R2K1 w - - 0 1");
        assertTrue(new Search().FindBestMove(tactical, 1).nodes()
                > 1 + generator.GenerateLegalMoves(tactical).size());
    }

    @Test
    void recursionGuardAbortsInsteadOfEvaluatingCheck() throws Exception
    {
        String fen = "r7/8/8/8/8/7Q/2k5/K7 w - - 0 1";
        Position position = PositionFrom(fen);
        Search search = new Search();
        var quiescence = Search.class.getDeclaredMethod("Quiescence", Position.class,
                int.class, int.class, int.class, int.class);
        quiescence.setAccessible(true);
        quiescence.invoke(search, position, -32000, 32000, 65, 64);
        assertEquals(0, search.FindBestMove(position, 1).depth());
        AssertPosition(fen, position);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            Position.STARTING_FEN,
            "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 7 19",
            "4k3/8/8/8/3pP3/8/8/K7 b - e3 0 1",
            "4k3/1P6/8/8/8/8/8/4K3 w - - 0 1"
    })
    void alphaBetaMatchesAnExhaustiveSearch(String fen)
    {
        Position position = PositionFrom(fen);
        int expected = ExhaustiveScore(position, 2, 0);
        Search.Result result = new Search().FindBestMove(position, 2);
        assertEquals(expected, result.score());
        assertTrue(generator.GenerateLegalMoves(position).contains(result.bestMove()));
        AssertPosition(fen, position);

        Position.UndoState undo = new Position.UndoState();
        position.MakeMove(result.bestMove(), undo);
        assertEquals(expected, -ExhaustiveScore(position, 1, 1));
        position.UnmakeMove(undo);
        AssertPosition(fen, position);
    }

    @Test
    void searchRestoresTheHistoryOfItsCopiedPosition()
    {
        Position game = new Position();
        game.MakeMove(Move.Encode(6, 21, Move.NORMAL), new Position.UndoState());
        game.MakeMove(Move.Encode(62, 45, Move.NORMAL), new Position.UndoState());
        Position searchPosition = new Position(game);
        long[] history = game.GetPositionHistory();
        String fen = game.ToFen();

        new Search().FindBestMove(searchPosition, 3);

        assertArrayEquals(history, searchPosition.GetPositionHistory());
        assertArrayEquals(history, game.GetPositionHistory());
        assertEquals(fen, searchPosition.ToFen());
        assertEquals(game.GetPositionKey(), searchPosition.GetPositionKey());
    }

    @Test
    void stopBeforeStartupStillReturnsALegalMove()
    {
        Position position = new Position();
        Search search = new Search();
        search.Stop();
        Search.Result result = search.FindBestMove(position, Search.MAX_DEPTH);
        assertEquals(0, result.depth());
        assertTrue(generator.GenerateLegalMoves(position).contains(result.bestMove()));
        AssertPosition(Position.STARTING_FEN, position);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 65})
    void rejectsUnsupportedDepths(int depth)
    {
        Position position = new Position();
        assertThrows(IllegalArgumentException.class, () -> new Search().FindBestMove(position, depth));
        AssertPosition(Position.STARTING_FEN, position);
    }

    // Independent reference: visit every branch without alpha-beta or iterative deepening.
    private int ExhaustiveScore(Position position, int depth, int ply)
    {
        List<Integer> moves = generator.GenerateLegalMoves(position);
        if (moves.isEmpty())
            return generator.IsInCheck(position, position.GetSideToMove()) ? -Search.MATE_SCORE + ply : 0;
        if (position.GetDrawReason() != DrawReason.NONE)
            return 0;
        boolean inCheck = generator.IsInCheck(position, position.GetSideToMove());
        int best = depth == 0 && !inCheck ? new Evaluator().Evaluate(position) : Integer.MIN_VALUE;
        for (int move : moves)
        {
            if (depth == 0 && !inCheck && Move.GetType(move) == Move.NORMAL
                    && position.GetPiece(Move.GetTo(move)) == Piece.NONE)
                continue;
            if (depth == 0 && !inCheck && Move.GetType(move) == Move.CASTLING)
                continue;
            Position.UndoState undo = new Position.UndoState();
            position.MakeMove(move, undo);
            best = Math.max(best, -ExhaustiveScore(position, Math.max(0, depth - 1), ply + 1));
            position.UnmakeMove(undo);
        }
        return best;
    }

    private static Position PositionFrom(String fen)
    {
        Position position = new Position();
        position.SetPosition(fen);
        return position;
    }

    private static void AssertPosition(String fen, Position actual)
    {
        assertEquals(fen, actual.ToFen());
        Position expected = PositionFrom(fen);
        assertEquals(expected.GetPositionKey(), actual.GetPositionKey());
        assertArrayEquals(expected.GetPositionHistory(), actual.GetPositionHistory());
        for (int piece = 0; piece < Piece.COUNT; piece++)
            assertEquals(expected.GetBitBoard(piece), actual.GetBitBoard(piece));
        assertEquals(expected.GetOccupancy(Piece.WHITE), actual.GetOccupancy(Piece.WHITE));
        assertEquals(expected.GetOccupancy(Piece.BLACK), actual.GetOccupancy(Piece.BLACK));
    }
}
