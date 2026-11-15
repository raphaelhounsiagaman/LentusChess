package chess.uci;

import chess.Engine;
import chess.board.Move;
import chess.board.MoveGenerator;
import chess.board.Position;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(10)
class UCITest
{
    @Test
    void handshakeAndOpeningMovesAcceptExtraWhitespace()
    {
        Engine engine = new Engine();
        String output = RunCommands(engine,
                "\n  uci \n  isready\t\nposition   startpos moves e2e4 e7e5 g1f3\nquit\n");
        assertTrue(output.contains("id name LentusChess"));
        assertTrue(output.contains("uciok"));
        assertTrue(output.contains("readyok"));
        assertEquals("rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2",
                engine.GetPositionFen());
    }

    @Test
    void fenCommandsResolvePromotionsAndNewGameResetsTheBoard()
    {
        Engine engine = new Engine();
        RunCommands(engine, "position fen 4k3/1P6/8/8/8/8/8/4K3 w - - 0 1 moves b7b8q\n");
        assertEquals("1Q2k3/8/8/8/8/8/8/4K3 b - - 0 1", engine.GetPositionFen());
        RunCommands(engine, "ucinewgame\n");
        assertEquals(Position.STARTING_FEN, engine.GetPositionFen());
    }

    @ParameterizedTest
    @ValueSource(strings = {"stop\nquit\n", "quit\n", ""})
    void readinessAndShutdownWorkDuringSearch(String ending)
    {
        Engine engine = new Engine();
        String output = RunCommands(engine, "go depth 64\nisready\n" + ending);
        assertTrue(output.contains("readyok"));
        assertTrue(output.contains("info depth "));
        String[] bestMoves = output.lines().filter(line -> line.startsWith("bestmove ")).toArray(String[]::new);
        assertEquals(1, bestMoves.length);
        assertTrue(output.indexOf("readyok") < output.indexOf("bestmove "));
        assertTrue(new MoveGenerator().GenerateLegalMoves(new Position()).stream()
                .map(Move::ToUci).anyMatch(move -> bestMoves[0].equals("bestmove " + move)));
        assertEquals(Position.STARTING_FEN, engine.GetPositionFen());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "7k/6Q1/5K2/8/8/8/8/8 b - - 0 1",
            "7k/5Q2/5K2/8/8/8/8/8 b - - 0 1"
    })
    void terminalPositionsReportNoMove(String fen)
    {
        String output = RunCommands(new Engine(), "position fen " + fen + "\ngo depth 2\nstop\nquit\n");
        assertTrue(output.contains("bestmove 0000"));
        assertTrue(output.contains(fen.contains("6Q1") ? "score mate 0" : "score cp 0"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "4k3/8/8/8/8/8/8/R3K3 w - - 100 51",
            "4k3/8/8/8/8/8/8/2B1K3 w - - 0 1"
    })
    void recognizedDrawsReportZeroAndALegalMove(String fen)
    {
        Engine engine = new Engine();
        String output = RunCommands(engine, "position fen " + fen + "\ngo depth 2\nstop\nquit\n");
        assertTrue(output.contains("score cp 0"));
        assertTrue(new MoveGenerator().GenerateLegalMoves(engine.GetPositionSnapshot()).stream()
                .map(Move::ToUci).anyMatch(move -> output.contains("bestmove " + move)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "position", "position fen 8/8/8/8/8/8/8/8 w - - 0 1", "position startpos bogus",
            "position startpos moves e2e4 e7e4", "go depth", "go depth zero", "go depth 0",
            "go depth 65", "go nodes 100", "go wtime -1", "go movestogo 0", "go wtime"
    })
    void reportsCommandErrorsAndContinuesReading(String command)
    {
        Engine engine = new Engine();
        String output = RunCommands(engine, command + "\nisready\nquit\n");
        assertTrue(output.contains("info string "));
        assertTrue(output.contains("readyok"));
        assertFalse(output.contains("bestmove "));
        assertEquals(Position.STARTING_FEN, engine.GetPositionFen());
    }

    @Test
    void unknownCommandsAndIdleStopAreIgnoredAndQuitEndsInput()
    {
        Engine engine = new Engine();
        String output = RunCommands(engine, "unknown\nstop\nisready\nquit\nposition startpos moves e2e4\n");
        assertEquals("readyok", output.trim());
        assertEquals(Position.STARTING_FEN, engine.GetPositionFen());
    }

    private static String RunCommands(Engine engine, String commands)
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new UCI(engine, new ByteArrayInputStream(commands.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8)).run();
        return output.toString(StandardCharsets.UTF_8);
    }
}
