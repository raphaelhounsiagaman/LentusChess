package chess.board;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class DrawRecognitionTest
{
    @Test
    void threeOccurrencesAreRequiredAndCopyingPreservesThemWhileFenDoesNot()
    {
        Position position = new Position();
        assertEquals(DrawReason.NONE, position.GetDrawReason());
        Play(position, "g1f3", "g8f6", "f3g1", "f6g8");
        assertEquals(DrawReason.NONE, position.GetDrawReason());
        // Reach the same board through a different sequence of moves.
        Play(position, "b1c3", "b8c6", "c3b1");
        Position.UndoState last = Play(position, "c6b8");
        assertEquals(DrawReason.THREEFOLD_REPETITION, position.GetDrawReason());
        assertTrue(position.GetDrawReason().IsHistoryDependent());
        assertEquals(DrawReason.THREEFOLD_REPETITION, new Position(position).GetDrawReason());
        assertEquals(DrawReason.NONE, FromFen(position.ToFen()).GetDrawReason());

        position.UnmakeMove(last);
        assertEquals(DrawReason.NONE, position.GetDrawReason());
        Play(position, "c6b8");
        Position.UndoState pawn = Play(position, "e2e4");
        assertEquals(DrawReason.NONE, position.GetDrawReason());
        position.UnmakeMove(pawn);
        assertEquals(DrawReason.THREEFOLD_REPETITION, position.GetDrawReason());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            // A legal en-passant opportunity makes the initial position different.
            "k7/8/8/3pP3/8/8/8/4K3 w - d6 0 1 | false",
            // An unusable target does not: first a pinned pawn, then no capturer.
            "k3r3/8/8/3pP3/8/8/8/4K3 w - d6 0 1 | true",
            "k7/8/8/3p4/8/8/8/4K3 w - d6 0 1 | true",
            // Returning the king does not restore castling rights.
            "k7/8/8/8/8/8/8/4K2R w K - 0 1 | false"
    })
    void repetitionRespectsCastlingAndRelevantEnPassantRights(String fen, boolean initialCounts)
    {
        Position position = FromFen(fen);
        for (int cycle = 0; cycle < 2; cycle++)
            Play(position, "e1f1", "a8b8", "f1e1", "b8a8");
        assertEquals(initialCounts ? DrawReason.THREEFOLD_REPETITION : DrawReason.NONE,
                position.GetDrawReason());
        Play(position, "e1f1", "a8b8", "f1e1", "b8a8");
        assertEquals(DrawReason.THREEFOLD_REPETITION, position.GetDrawReason());
    }

    @ParameterizedTest
    @CsvSource({"0, false", "98, false", "99, false", "100, true", "101, true", "150, true"})
    void fiftyMoveThresholdUsesHalfmoves(int clock, boolean reached)
    {
        Position position = FromFen("4k3/8/8/8/8/8/8/R3K3 b - - " + clock + " 80");
        assertEquals(reached ? DrawReason.FIFTY_MOVE_RULE : DrawReason.NONE, position.GetDrawReason());
        assertEquals(reached, position.GetDrawReason().IsHistoryDependent());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "4k3/8/8/8/8/8/8/R3K3 w - - 99 1 | e1f1 | FIFTY_MOVE_RULE",
            "4k3/8/8/8/8/8/8/4K2R w K - 99 1 | e1g1 | FIFTY_MOVE_RULE",
            "4k3/8/8/8/8/8/P7/4K3 w - - 100 1 | a2a3 | NONE",
            "4k3/r7/8/8/8/8/8/R3K3 w - - 100 1 | a1a7 | NONE",
            "4k3/P7/8/8/8/8/8/4K3 w - - 100 1 | a7a8q | NONE"
    })
    void movesUpdateTheThresholdAndUndoRestoresIt(String fen, String move, DrawReason after)
    {
        Position position = FromFen(fen);
        DrawReason before = position.GetDrawReason();
        Position.UndoState undo = Play(position, move);
        assertEquals(after, position.GetDrawReason());
        position.UnmakeMove(undo);
        assertEquals(before, position.GetDrawReason());
        assertEquals(fen, position.ToFen());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "4k3/8/8/8/8/8/8/4K3 w - - 0 1 | true",
            "4k3/8/8/8/8/8/8/2B1K3 w - - 0 1 | true",
            "2b1k3/8/8/8/8/8/8/4K3 w - - 0 1 | true",
            "4k3/8/8/8/8/8/8/1N2K3 w - - 0 1 | true",
            "1n2k3/8/8/8/8/8/8/4K3 w - - 0 1 | true",
            // Bishop-only positions, including promoted bishops of the same square color.
            "4kb2/8/8/8/8/8/8/2B1K3 w - - 0 1 | true",
            "2b1k3/8/8/8/8/8/8/4KB2 w - - 0 1 | true",
            "4kb2/8/8/8/5B2/8/3B4/2B1K3 w - - 0 1 | true",
            // Mating material or possible cooperative mates must not be called dead.
            "4k3/8/8/8/8/8/P7/4K3 w - - 0 1 | false",
            "4k3/p7/8/8/8/8/8/4K3 w - - 0 1 | false",
            "4k3/8/8/8/8/8/8/R3K3 w - - 0 1 | false",
            "r3k3/8/8/8/8/8/8/4K3 w - - 0 1 | false",
            "4k3/8/8/8/8/8/8/3QK3 w - - 0 1 | false",
            "3qk3/8/8/8/8/8/8/4K3 w - - 0 1 | false",
            "4k3/8/8/8/8/8/8/1NN1K3 w - - 0 1 | false",
            "1n2k3/8/8/8/8/8/8/1N2K3 w - - 0 1 | false",
            "4k3/8/8/8/8/8/8/2B1K1N1 w - - 0 1 | false",
            "1n2k3/8/8/8/8/8/8/2B1K3 w - - 0 1 | false",
            "4k3/8/8/8/8/8/8/2B1KB2 w - - 0 1 | false",
            "2b1k3/8/8/8/8/8/8/2B1K3 w - - 0 1 | false"
    })
    void recognizesOnlySupportedInsufficientMaterial(String fen, boolean insufficient)
    {
        DrawReason reason = FromFen(fen).GetDrawReason();
        assertEquals(insufficient ? DrawReason.INSUFFICIENT_MATERIAL : DrawReason.NONE, reason);
        assertFalse(reason.IsHistoryDependent());
    }

    @Test
    void boardOnlyDrawTakesPriorityWhenTheClockAlsoAllowsADraw()
    {
        DrawReason reason = FromFen("4k3/8/8/8/8/8/8/4K3 w - - 100 51").GetDrawReason();
        assertEquals(DrawReason.INSUFFICIENT_MATERIAL, reason);
        assertFalse(reason.IsHistoryDependent());
    }

    private static Position FromFen(String fen)
    {
        Position position = new Position();
        position.SetPosition(fen);
        return position;
    }

    private static Position.UndoState Play(Position position, String... moves)
    {
        Position.UndoState undo = new Position.UndoState();
        MoveGenerator generator = new MoveGenerator();
        for (String text : moves)
        {
            int move = generator.GenerateLegalMoves(position).stream()
                    .filter(candidate -> Move.ToUci(candidate).equals(text)).findFirst().orElseThrow();
            position.MakeMove(move, undo);
        }
        return undo;
    }
}
