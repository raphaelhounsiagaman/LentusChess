package chess.board;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PositionKeyTest
{
    @Test
    void identityIncludesPiecesSideAndEveryCastlingRightButNotCounters()
    {
        String board = "r3k2r/8/8/8/8/8/8/R3K2R";
        Set<Long> keys = new HashSet<>();
        for (int rights = 0; rights < 16; rights++)
        {
            StringBuilder field = new StringBuilder();
            for (int bit = 0; bit < 4; bit++)
                if ((rights & (1 << bit)) != 0)
                    field.append("KQkq".charAt(bit));
            String castling = field.isEmpty() ? "-" : field.toString();
            for (String side : new String[]{"w", "b"})
            {
                String prefix = board + " " + side + " " + castling + " - ";
                long key = Key(prefix + "0 1");
                assertTrue(keys.add(key), prefix);
                assertEquals(key, Key(prefix + "99 50"));
            }
        }
        long original = Key(board + " w - - 0 1");
        assertNotEquals(original, Key("r3k2r/8/8/8/8/8/7R/R3K3 w - - 0 1"));
        assertNotEquals(original, Key("r3k2r/8/8/8/8/8/8/R3K2Q w - - 0 1"));
        assertNotEquals(original, Key("r3k2r/8/8/8/8/8/8/R3K2r w - - 0 1"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            // Legal captures for either side, including capturing the pawn giving check.
            "k7/8/8/3pP3/8/8/8/4K3 w - d6 0 1 | true",
            "4k3/8/8/8/3Pp3/8/8/K7 b - d3 0 1 | true",
            "k7/8/8/3pP3/4K3/8/8/8 w - d6 0 1 | true",
            // No capturer, no captured pawn, or an occupied target.
            "k7/8/8/3p4/8/8/8/4K3 w - d6 0 1 | false",
            "k7/8/8/4P3/8/8/8/4K3 w - d6 0 1 | false",
            "k7/8/3n4/3pP3/8/8/8/4K3 w - d6 0 1 | false",
            // File, rank, and diagonal pins, and an unrelated check left unanswered.
            "k3r3/8/8/3pP3/8/8/8/4K3 w - d6 0 1 | false",
            "k7/8/8/r4pPK/8/8/8/8 w - f6 0 1 | false",
            "8/8/8/8/R4Ppk/8/8/K7 b - f3 0 1 | false",
            "k7/8/b7/1Pp5/2K5/8/8/8 w - c6 0 1 | false",
            "k7/8/8/3pP3/8/8/2n5/4K3 w - d6 0 1 | false",
            // The first candidate is pinned, but the second can capture.
            "k1r5/8/8/2PpP3/8/8/8/2K5 w - d6 0 1 | true"
    })
    void enPassantOnlyChangesIdentityWhenALegalCaptureExists(String fen, boolean relevant)
    {
        String[] fields = fen.split(" ");
        fields[3] = "-";
        long withoutEnPassant = Key(String.join(" ", fields));
        assertEquals(relevant, Key(fen) != withoutEnPassant);
    }

    @Test
    void legalEnPassantFilesHaveDifferentKeys()
    {
        String board = "k7/8/8/3pPp2/8/8/8/4K3 w - ";
        assertNotEquals(Key(board + "d6 0 1"), Key(board + "f6 0 1"));
    }

    @Test
    void reversibleCycleRestoresIdentityAndNestedUndoRestoresEachKey()
    {
        Position position = new Position();
        long startingKey = position.GetPositionKey();
        int[] from = {6, 62, 21, 45};
        int[] to = {21, 45, 6, 62};
        Position.UndoState[] undo = new Position.UndoState[from.length];
        long[] keys = new long[from.length];
        for (int i = 0; i < from.length; i++)
        {
            keys[i] = position.GetPositionKey();
            undo[i] = new Position.UndoState();
            position.MakeMove(Move.Encode(from[i], to[i], Move.NORMAL), undo[i]);
            assertEquals(Key(position.ToFen()), position.GetPositionKey());
        }
        assertEquals(4, position.GetHalfmoveClock());
        assertEquals(startingKey, position.GetPositionKey());
        for (int i = from.length - 1; i >= 0; i--)
        {
            position.UnmakeMove(undo[i]);
            assertEquals(keys[i], position.GetPositionKey());
        }
        assertEquals(Position.STARTING_FEN, position.ToFen());
        position.SetPosition("k7/8/8/3pP3/8/8/8/4K3 w - d6 0 1");
        position.SetStartingPosition();
        assertEquals(startingKey, position.GetPositionKey());
    }

    private static long Key(String fen)
    {
        Position position = new Position();
        position.SetPosition(fen);
        return position.GetPositionKey();
    }
}
