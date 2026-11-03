package chess;

import chess.board.Move;
import chess.board.Position;
import chess.search.Search;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(10)
class EngineTest
{
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1 | e1g1 | r3k2r/8/8/8/8/8/8/R4RK1 b kq - 1 1",
            "k7/8/8/3pP3/8/8/8/4K3 w - d6 0 1 | e5d6 | k7/8/3P4/8/8/8/8/4K3 b - - 0 1",
            "4k3/1P6/8/8/8/8/8/4K3 w - - 0 1 | b7b8n | 1N2k3/8/8/8/8/8/8/4K3 b - - 0 1"
    })
    void resolvesSpecialMovesFromUciNotation(String fen, String move, String expected)
    {
        Engine engine = new Engine();
        engine.SetPosition(fen, List.of(move));
        assertEquals(expected, engine.GetPositionFen());
    }

    @Test
    void anIllegalMoveDoesNotInstallAPartiallyAppliedPosition()
    {
        Engine engine = new Engine();
        engine.SetPosition(Position.STARTING_FEN, List.of("d2d4"));
        String before = engine.GetPositionFen();
        assertThrows(IllegalArgumentException.class,
                () -> engine.SetPosition(Position.STARTING_FEN, List.of("e2e4", "e7e4")));
        assertEquals(before, engine.GetPositionFen());
        engine.NewGame();
        assertEquals(Position.STARTING_FEN, engine.GetPositionFen());
    }

    @Test
    void uciMoveHistorySurvivesSnapshotsAndRejectedPositionUpdates()
    {
        Engine engine = new Engine();
        engine.SetPosition(Position.STARTING_FEN, List.of("g1f3", "g8f6", "f3g1", "f6g8"));
        Position snapshot = engine.GetPositionSnapshot();
        long[] history = snapshot.GetPositionHistory();
        assertEquals(5, history.length);
        assertEquals(new Position().GetPositionKey(), history[0]);
        assertEquals(history[0], history[4]);
        String fen = snapshot.ToFen();
        snapshot.MakeMove(Move.Encode(12, 28, Move.NORMAL), new Position.UndoState());
        assertEquals(fen, engine.GetPositionFen());
        assertArrayEquals(history, engine.GetPositionSnapshot().GetPositionHistory());

        assertThrows(IllegalArgumentException.class,
                () -> engine.SetPosition(Position.STARTING_FEN, List.of("e2e4", "e7e4")));
        assertArrayEquals(history, engine.GetPositionSnapshot().GetPositionHistory());
        engine.SetPosition(fen, List.of());
        assertArrayEquals(new long[]{history[4]}, engine.GetPositionSnapshot().GetPositionHistory());
        engine.SetPosition(Position.STARTING_FEN, List.of("e2e4"));
        engine.NewGame();
        assertArrayEquals(new long[]{history[0]}, engine.GetPositionSnapshot().GetPositionHistory());
    }

    @Test
    void searchCompletesWithoutPlayingItsMove() throws InterruptedException
    {
        Engine engine = new Engine();
        String fen = "4k3/8/8/8/8/8/q7/R3K3 w - - 0 1";
        engine.SetPosition(fen, List.of());
        LinkedBlockingQueue<Search.Result> results = new LinkedBlockingQueue<>();
        try
        {
            engine.StartSearch(2, results::add);
            Search.Result result = results.poll(5, TimeUnit.SECONDS);
            assertNotNull(result);
            assertEquals("a1a2", Move.ToUci(result.bestMove()));
            assertEquals(2, result.depth());
            assertEquals(fen, engine.GetPositionFen());
        }
        finally
        {
            engine.StopSearch();
        }
        assertTrue(results.isEmpty(), "Only one completion per search");
    }

    @Test
    void searchReceivesRepetitionsThatTheSameFenAloneCannotRepresent() throws InterruptedException
    {
        Engine engine = new Engine();
        engine.SetPosition("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNB1KBNR w KQkq - 0 1",
                List.of("g1f3", "g8f6", "f3g1", "f6g8", "g1f3", "g8f6", "f3g1", "f6g8"));
        String fen = engine.GetPositionFen();
        long[] history = engine.GetPositionSnapshot().GetPositionHistory();
        LinkedBlockingQueue<Search.Result> results = new LinkedBlockingQueue<>();
        try
        {
            engine.StartSearch(1, results::add);
            Search.Result repeated = results.poll(5, TimeUnit.SECONDS);
            assertNotNull(repeated);
            assertEquals(0, repeated.score());
            assertNotEquals(Move.NONE, repeated.bestMove());
            assertTrue(repeated.historyDependent());
            assertArrayEquals(history, engine.GetPositionSnapshot().GetPositionHistory());

            engine.SetPosition(fen, List.of());
            engine.StartSearch(1, results::add);
            Search.Result fresh = results.poll(5, TimeUnit.SECONDS);
            assertNotNull(fresh);
            assertEquals(-900, fresh.score());
            assertFalse(fresh.historyDependent());
        }
        finally
        {
            engine.StopSearch();
        }
    }

    @Test
    void stopAndNewGameJoinTheWorkerAndAllowAnotherSearch() throws InterruptedException
    {
        Engine engine = new Engine();
        LinkedBlockingQueue<Search.Result> results = new LinkedBlockingQueue<>();
        try
        {
            engine.StartSearch(Search.MAX_DEPTH, results::add);
            engine.StopSearch();
            assertNotNull(results.poll());
            engine.StopSearch();
            assertTrue(results.isEmpty());

            engine.StartSearch(Search.MAX_DEPTH, results::add);
            engine.NewGame();
            assertNotNull(results.poll());
            assertEquals(Position.STARTING_FEN, engine.GetPositionFen());

            engine.StartSearch(1, results::add);
            Search.Result result = results.poll(5, TimeUnit.SECONDS);
            assertNotNull(result);
            assertEquals(1, result.depth());
        }
        finally
        {
            engine.StopSearch();
        }
    }
}
