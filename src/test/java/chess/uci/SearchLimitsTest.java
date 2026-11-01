package chess.uci;

import chess.board.MoveGenerator;
import chess.board.Position;
import chess.search.Search;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class SearchLimitsTest
{
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "go | 0 | 4 | 0",
            "go depth 3 | 0 | 3 | 0",
            "go wtime 180000 btime 180000 winc 2000 binc 2000 | 0 | 64 | 5000",
            "go wtime 180000 btime 1000 winc 2000 binc 2000 | 1 | 64 | 800",
            "go wtime 100 btime 180000 | 0 | 64 | 1",
            "go wtime 0 btime 180000 | 0 | 64 | 1",
            "go movetime 30 | 0 | 64 | 30",
            "go movetime 0 | 0 | 64 | 1",
            "go depth 2 wtime 180000 btime 180000 | 0 | 2 | 5000",
            "go wtime 1000 movestogo 10 | 0 | 64 | 100",
            "go wtime 50 movetime 100 | 0 | 64 | 1"
    })
    void respectsSideClockDepthAndReserve(String command, int side, int depth, long millis)
    {
        assertEquals(new SearchLimits(depth, millis), SearchLimits.Parse(command.split(" "), side));
    }

    @Test
    void timedSearchReturnsLegalMoveAndRestoresBoard()
    {
        Position position = new Position();
        Search.Result result = assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> new Search().FindBestMove(position, Search.MAX_DEPTH, 25));
        assertTrue(new MoveGenerator().GenerateLegalMoves(position).contains(result.bestMove()));
        assertEquals(Position.STARTING_FEN, position.ToFen());
        assertTrue(result.depth() < Search.MAX_DEPTH);
    }
}
