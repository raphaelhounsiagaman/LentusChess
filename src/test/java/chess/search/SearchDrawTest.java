package chess.search;

import chess.Engine;
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
    @CsvSource({"99, 1, 0, true", "98, 1, 500, false", "98, 2, 0, true", "0, 2, 500, false"})
    void fiftyMoveDrawsAreDetectedAtTheHorizonAndPropagated(int clock, int depth, int score,
                                                         boolean historyDependent)
    {
        String fen = "4k3/8/8/8/8/8/8/R3K3 w - - " + clock + " 50";
        Position position = FromFen(fen);
        Search.Result result = new Search().FindBestMove(position, depth);
        assertEquals(score, result.score());
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
        assertEquals(500, result.score());
        assertTrue(result.historyDependent(), "Other moves reached the fifty-move threshold");
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
        assertEquals(-900, new Evaluator().Evaluate(position));

        Position fresh = FromFen(fen);
        assertEquals(position.GetPositionKey(), fresh.GetPositionKey());
        Search.Result ongoing = search.FindBestMove(fresh, 1);
        assertEquals(-900, ongoing.score());
        assertFalse(ongoing.historyDependent());
    }

    private static Position FromFen(String fen)
    {
        Position position = new Position();
        position.SetPosition(fen);
        return position;
    }
}
