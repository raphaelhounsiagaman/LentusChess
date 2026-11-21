package chess.search;

import chess.Engine;
import chess.board.DrawReason;
import chess.board.Move;
import chess.board.MoveGenerator;
import chess.board.Position;
import chess.eval.Evaluator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchDrawTest
{
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "4k3/8/8/8/8/8/8/R3K3 w - - 100 51 | true",
            "4k3/8/8/8/8/8/8/R3K3 b - - 150 80 | true",
            "4k3/8/8/8/8/8/8/2B1K3 w - - 0 1 | false",
            "1n2k3/8/8/8/8/8/8/4K3 w - - 0 1 | false"
    })
    void rootDrawsKeepALegalMoveEvenWhenStoppedBeforeStartup(String fen, boolean historyDependent)
    {
        Position position = FromFen(fen);
        Search search = new Search();
        search.Stop();
        Search.Result result = search.FindBestMove(position, 3);
        assertEquals(0, result.score());
        assertEquals(historyDependent, result.historyDependent());
        assertTrue(new MoveGenerator().GenerateLegalMoves(position).contains(result.bestMove()));
        assertEquals(fen, position.ToFen());
        assertArrayEquals(new long[]{position.GetPositionKey()}, position.GetPositionHistory());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "7k/6Q1/5K2/8/8/8/8/8 b - - 100 51 | -30000",
            "7k/6Q1/5K2/8/8/8/8/8 b - - 150 80 | -30000",
            "7k/5Q2/5K2/8/8/8/8/8 b - - 100 51 | 0"
    })
    void terminalPositionsTakePrecedenceOverTheClock(String fen, int score)
    {
        Search.Result result = new Search().FindBestMove(FromFen(fen), 2);
        assertEquals(score, result.score());
        assertEquals(Move.NONE, result.bestMove());
        assertFalse(result.historyDependent());
    }

    @ParameterizedTest
    // At clock 98, depth-one checking moves now extend into evasions at clock 100.
    // Even though those draw lines are rejected, the result remains history-dependent.
    @CsvSource({"99, 1, true, true", "98, 1, false, true", "98, 2, true, true", "0, 2, false, false"})
    void fiftyMoveDrawsAreDetectedAtTheHorizonAndPropagated(int clock, int depth, boolean draw,
                                                         boolean historyDependent)
    {
        String fen = "4k3/8/8/8/8/8/8/R3K3 w - - " + clock + " 50";
        Position position = FromFen(fen);
        Search.Result result = new Search().FindBestMove(position, depth);
        if (draw)
            assertEquals(0, result.score());
        else
            assertTrue(result.score() > 350 && result.score() < 650,
                    "Without a forced draw, White retains roughly a rook's advantage");
        assertEquals(historyDependent, result.historyDependent());
        assertEquals(fen, position.ToFen());
        assertArrayEquals(new long[]{position.GetPositionKey()}, position.GetPositionHistory());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3})
    void aMatingMoveOnTheHundredthHalfmoveStillWins(int depth)
    {
        Position position = FromFen("7k/5Q2/6K1/8/8/8/8/8 w - - 99 50");
        assertEquals(Search.MATE_SCORE - 1, new Search().FindBestMove(position, depth).score());
    }

    @Test
    void rejectedDrawMovesStillMarkTheResultAsHistoryDependent()
    {
        Position position = FromFen("4k3/8/8/8/8/8/q7/R3K3 w - - 99 50");
        Search.Result result = new Search().FindBestMove(position, 1);
        assertEquals("a1a2", Move.ToUci(result.bestMove()));
        Position leaf = new Position(position);
        leaf.MakeMove(result.bestMove(), new Position.UndoState());
        assertEquals(-new Evaluator().Evaluate(leaf), result.score());
        assertTrue(result.score() > 0);
        assertTrue(result.historyDependent(), "Other moves reached the fifty-move threshold");
    }

    @Test
    void cachedScoreInAnOngoingNodeDoesNotBypassTheClockInDescendants()
    {
        String board = "4k3/8/8/8/8/8/8/R3K3 w - - ";
        Position fresh = FromFen(board + "0 50");
        Position nearDraw = FromFen(board + "98 50");
        Position freshChild = new Position(fresh);
        Position nearDrawChild = new Position(nearDraw);
        int rookMove = Move.Encode(0, 8, Move.NORMAL);
        freshChild.MakeMove(rookMove, new Position.UndoState());
        nearDrawChild.MakeMove(rookMove, new Position.UndoState());
        long key = freshChild.GetPositionKey();
        assertEquals(key, nearDrawChild.GetPositionKey());
        assertEquals(99, nearDrawChild.GetHalfmoveClock());
        assertEquals(DrawReason.NONE, nearDrawChild.GetDrawReason());

        TranspositionTable table = new TranspositionTable(16_384);
        Search search = new Search(table);
        assertTrue(search.FindBestMove(fresh, 2).score() > 350);
        int hint = table.ProbeMove(key);
        assertTrue(new MoveGenerator().GenerateLegalMoves(nearDrawChild).contains(hint));
        table.Store(new TranspositionTable.Entry(key, 1, -Search.MATE_SCORE,
                TranspositionTable.Bound.EXACT, hint, freshChild.CaptureDrawContext(), false));
        assertNotNull(table.ProbeExact(freshChild, 1));
        assertNull(table.ProbeExact(nearDrawChild, 1));

        Search.Result result = search.FindBestMove(nearDraw, 2);
        assertEquals(2, result.depth());
        assertEquals(0, result.score());
        assertTrue(result.historyDependent());
    }

    @Test
    void rejectsAnExactScoreWhenOnlyTheProbingHistoryAllowsARepetitionInAChild()
    {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNB1KBNR b KQkq - 0 1";
        Engine engine = new Engine();
        engine.SetPosition(fen, List.of("g8f6", "g1f3", "f6g8", "f3g1", "g8f6", "g1f3"));
        Position repeated = engine.GetPositionSnapshot();
        engine.SetPosition(fen, List.of("g8f6", "g1f3", "b8c6", "b1c3", "c6b8", "c3b1"));
        int candidate = Move.Encode(45, 62, Move.NORMAL);
        int repeatingMove = Move.Encode(21, 6, Move.NORMAL);
        Position repeatedChild = new Position(repeated);
        Position alternateChild = engine.GetPositionSnapshot();
        repeatedChild.MakeMove(candidate, new Position.UndoState());
        alternateChild.MakeMove(candidate, new Position.UndoState());
        long key = repeatedChild.GetPositionKey();
        assertEquals(key, alternateChild.GetPositionKey());
        assertEquals(repeatedChild.GetHalfmoveClock(), alternateChild.GetHalfmoveClock());
        assertEquals(DrawReason.NONE, repeatedChild.GetDrawReason());
        assertEquals(DrawReason.NONE, alternateChild.GetDrawReason());

        TranspositionTable table = new TranspositionTable(16_384);
        table.Store(new TranspositionTable.Entry(key, 1, -Search.MATE_SCORE,
                TranspositionTable.Bound.EXACT, repeatingMove, alternateChild.CaptureDrawContext(), false));
        assertNotNull(table.ProbeExact(alternateChild, 1));
        assertNull(table.ProbeExact(repeatedChild, 1));

        repeatedChild.MakeMove(repeatingMove, new Position.UndoState());
        alternateChild.MakeMove(repeatingMove, new Position.UndoState());
        assertEquals(DrawReason.THREEFOLD_REPETITION, repeatedChild.GetDrawReason());
        assertEquals(DrawReason.NONE, alternateChild.GetDrawReason());

        Search.Result cold = new Search().FindBestMove(repeated, 2);
        Search.Result warm = new Search(table).FindBestMove(repeated, 2);
        assertEquals(cold.score(), warm.score());
        assertTrue(warm.historyDependent());
    }

    @Test
    void capturingIntoInsufficientMaterialIsADrawAtTheHorizon()
    {
        Position position = FromFen("7k/8/8/8/8/2b5/8/1N2K3 w - - 0 1");
        Search.Result result = new Search().FindBestMove(position, 1);
        assertEquals("b1c3", Move.ToUci(result.bestMove()));
        assertEquals(0, result.score());
        assertFalse(result.historyDependent());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3})
    void searchChoosesAThirdOccurrenceUsingGameHistoryAndKeepsStaticEvaluationSeparate(int depth)
    {
        Engine engine = new Engine();
        engine.SetPosition("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNB1KBNR b KQkq - 0 1",
                List.of("g8f6", "g1f3", "f6g8", "f3g1", "g8f6", "g1f3", "f6g8"));
        Position position = engine.GetPositionSnapshot();
        String fen = position.ToFen();
        long[] history = position.GetPositionHistory();
        Search search = new Search();
        Search.Result repeated = search.FindBestMove(position, depth);
        assertEquals("f3g1", Move.ToUci(repeated.bestMove()));
        assertEquals(0, repeated.score());
        assertTrue(repeated.historyDependent());
        assertEquals(fen, position.ToFen());
        assertArrayEquals(history, position.GetPositionHistory());
        assertTrue(new Evaluator().Evaluate(position) < -700,
                "Static evaluation must retain the queen deficit despite repetition history");

        Position fresh = FromFen(fen);
        assertEquals(position.GetPositionKey(), fresh.GetPositionKey());
        Search.Result ongoing = search.FindBestMove(fresh, 1);
        assertTrue(ongoing.score() < -700, "Without repetition, White remains a queen down");
        assertEquals(new Evaluator().Evaluate(position), new Evaluator().Evaluate(fresh));
        assertFalse(ongoing.historyDependent());
    }

    @Test
    void warmTableStillAdjudicatesAThirdOccurrenceAtAnInteriorNode()
    {
        Engine engine = new Engine();
        engine.SetPosition("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNB1KBNR b KQkq - 0 1",
                List.of("g8f6", "g1f3", "f6g8", "f3g1", "g8f6", "g1f3", "f6g8"));
        Position repeated = engine.GetPositionSnapshot();
        Position fresh = FromFen(repeated.ToFen());
        Position repeatedChild = new Position(repeated);
        Position freshChild = new Position(fresh);
        int repeatingMove = Move.Encode(21, 6, Move.NORMAL);
        repeatedChild.MakeMove(repeatingMove, new Position.UndoState());
        freshChild.MakeMove(repeatingMove, new Position.UndoState());
        long key = freshChild.GetPositionKey();
        assertEquals(key, repeatedChild.GetPositionKey());
        assertEquals(DrawReason.THREEFOLD_REPETITION, repeatedChild.GetDrawReason());
        assertEquals(DrawReason.NONE, freshChild.GetDrawReason());

        TranspositionTable table = new TranspositionTable(16_384);
        Search search = new Search(table);
        assertTrue(search.FindBestMove(fresh, 2).score() < -700);
        assertTrue(new MoveGenerator().GenerateLegalMoves(repeatedChild).contains(table.ProbeMove(key)));

        Search.Result result = search.FindBestMove(repeated, 2);
        assertEquals(2, result.depth());
        assertEquals(repeatingMove, result.bestMove());
        assertEquals(0, result.score());
        assertTrue(result.historyDependent());
    }

    private static Position FromFen(String fen)
    {
        Position position = new Position();
        position.SetPosition(fen);
        return position;
    }
}
