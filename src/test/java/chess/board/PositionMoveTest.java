package chess.board;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class PositionMoveTest
{
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "4k3/8/8/8/8/8/4P3/4K3 w - - 9 1 | 12 | 28 | 4k3/8/8/8/4P3/8/8/4K3 b - e3 0 1",
            "4k3/4p3/8/8/8/8/8/4K3 b - - 9 1 | 52 | 36 | 4k3/8/8/4p3/8/8/8/4K3 w - e6 0 2",
            "4k3/8/8/3p4/4P3/8/8/4K3 w - - 7 8 | 28 | 35 | 4k3/8/8/3P4/8/8/8/4K3 b - - 0 8",
            "4k3/8/8/3p4/8/8/8/4K1N1 w - d6 0 4 | 6 | 21 | 4k3/8/8/3p4/8/5N2/8/4K3 b - - 1 4",
            "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 3 9 | 4 | 5 | r3k2r/8/8/8/8/8/8/R4K1R b kq - 4 9",
            "r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 3 9 | 60 | 61 | r4k1r/8/8/8/8/8/8/R3K2R w KQ - 4 10",
            "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 3 9 | 7 | 15 | r3k2r/8/8/8/8/8/7R/R3K3 b Qkq - 4 9",
            "r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 3 9 | 56 | 0 | 4k2r/8/8/8/8/8/8/r3K2R w Kk - 0 10",
            "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 3 9 | 7 | 63 | r3k2R/8/8/8/8/8/8/R3K3 b Qq - 0 9"
    })
    void ordinaryMovesUpdateAndRestoreAllState(String before, int from, int to, String after)
    {
        Position position = new Position();
        position.SetPosition(before);
        Position.UndoState undo = new Position.UndoState();
        position.MakeMove(Move.Encode(from, to, Move.NORMAL), undo);
        AssertPosition(after, position);
        position.UnmakeMove(undo);
        AssertPosition(before, position);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "r3k2r/8/8/3pP3/8/8/8/R3K2R w KQkq d6 0 19 | 36 | 43 | r3k2r/8/3P4/8/8/8/8/R3K2R b KQkq - 0 19",
            "r3k2r/8/8/4Pp2/8/8/8/R3K2R w KQkq f6 0 19 | 36 | 45 | r3k2r/8/5P2/8/8/8/8/R3K2R b KQkq - 0 19",
            "r3k2r/8/8/8/2Pp4/8/8/R3K2R b KQkq c3 0 19 | 27 | 18 | r3k2r/8/8/8/8/2p5/8/R3K2R w KQkq - 0 20",
            "r3k2r/8/8/8/3pP3/8/8/R3K2R b KQkq e3 0 19 | 27 | 20 | r3k2r/8/8/8/8/4p3/8/R3K2R w KQkq - 0 20"
    })
    void enPassantMovesUpdateAndRestoreAllState(String before, int from, int to, String after)
    {
        Position position = new Position();
        position.SetPosition(before);
        Position.UndoState undo = new Position.UndoState();
        position.MakeMove(Move.Encode(from, to, Move.EN_PASSANT), undo);
        AssertPosition(after, position);
        position.UnmakeMove(undo);
        AssertPosition(before, position);
    }

    @Test
    void enPassantAfterDoublePushSupportsNestedUndoAndReuse()
    {
        String before = "r3k2r/3p4/8/4P3/8/8/8/R3K2R b KQkq - 7 19";
        String afterPush = "r3k2r/8/8/3pP3/8/8/8/R3K2R w KQkq d6 0 20";
        Position position = new Position();
        position.SetPosition(before);
        Position.UndoState push = new Position.UndoState();
        Position.UndoState capture = new Position.UndoState();
        for (int repetition = 0; repetition < 2; repetition++)
        {
            position.MakeMove(Move.Encode(51, 35, Move.NORMAL), push);
            AssertPosition(afterPush, position);
            position.MakeMove(Move.Encode(36, 43, Move.EN_PASSANT), capture);
            AssertPosition("r3k2r/8/3P4/8/8/8/8/R3K2R b KQkq - 0 20", position);
            position.UnmakeMove(capture);
            AssertPosition(afterPush, position);
            position.UnmakeMove(push);
            AssertPosition(before, position);
        }
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 7 19 | 4 | 6 | r3k2r/8/8/8/8/8/8/R4RK1 b kq - 8 19",
            "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 7 19 | 4 | 2 | r3k2r/8/8/8/8/8/8/2KR3R b kq - 8 19",
            "r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 7 19 | 60 | 62 | r4rk1/8/8/8/8/8/8/R3K2R w KQ - 8 20",
            "r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 7 19 | 60 | 58 | 2kr3r/8/8/8/8/8/8/R3K2R w KQ - 8 20",
            "r3k2r/8/8/3p4/8/8/8/R3K2R w KQkq d6 0 19 | 4 | 6 | r3k2r/8/8/3p4/8/8/8/R4RK1 b kq - 1 19",
            "r3k2r/8/8/8/4P3/8/8/R3K2R b KQkq e3 0 19 | 60 | 58 | 2kr3r/8/8/8/4P3/8/8/R3K2R w KQ - 1 20"
    })
    void castlingMovesUpdateAndRestoreAllState(String before, int from, int to, String after)
    {
        Position position = new Position();
        position.SetPosition(before);
        Position.UndoState undo = new Position.UndoState();
        position.MakeMove(Move.Encode(from, to, Move.CASTLING), undo);
        AssertPosition(after, position);
        position.UnmakeMove(undo);
        AssertPosition(before, position);
    }

    @Test
    void castlingSupportsNestedUndoAndReuse()
    {
        String before = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 7 19";
        String afterWhite = "r3k2r/8/8/8/8/8/8/R4RK1 b kq - 8 19";
        Position position = new Position();
        position.SetPosition(before);
        Position.UndoState white = new Position.UndoState();
        Position.UndoState black = new Position.UndoState();
        for (int repetition = 0; repetition < 2; repetition++)
        {
            position.MakeMove(Move.Encode(4, 6, Move.CASTLING), white);
            AssertPosition(afterWhite, position);
            position.MakeMove(Move.Encode(60, 58, Move.CASTLING), black);
            AssertPosition("2kr3r/8/8/8/8/8/8/R4RK1 w - - 9 20", position);
            position.UnmakeMove(black);
            AssertPosition(afterWhite, position);
            position.UnmakeMove(white);
            AssertPosition(before, position);
        }
    }

    @Test
    void undoRecordsSupportNestedMovesAndReuse()
    {
        Position position = new Position();
        Position.UndoState first = new Position.UndoState();
        Position.UndoState second = new Position.UndoState();
        for (int repetition = 0; repetition < 2; repetition++)
        {
            position.MakeMove(Move.Encode(12, 28, Move.NORMAL), first);
            String afterFirst = position.ToFen();
            position.MakeMove(Move.Encode(51, 35, Move.NORMAL), second);
            position.UnmakeMove(second);
            AssertPosition(afterFirst, position);
            position.UnmakeMove(first);
            AssertPosition(Position.STARTING_FEN, position);
        }
    }

    @ParameterizedTest
    @CsvSource({"3, N, n", "4, B, b", "5, R, r", "6, Q, q"})
    void promotionsAndCapturesRestoreExactly(int type, String white, String black)
    {
        String[] before = {
                "4k3/1P6/8/8/8/8/8/4K3 w - - 7 9",
                "r3k3/1P6/8/8/8/8/8/4K3 w q - 7 9",
                "4k3/8/8/8/8/8/1p6/4K3 b - - 7 9",
                "4k3/8/8/8/8/8/1p6/R3K3 b Q - 7 9"
        };
        String[] after = {
                "1" + white + "2k3/8/8/8/8/8/8/4K3 b - - 0 9",
                white + "3k3/8/8/8/8/8/8/4K3 b - - 0 9",
                "4k3/8/8/8/8/8/8/1" + black + "2K3 w - - 0 10",
                "4k3/8/8/8/8/8/8/" + black + "3K3 w - - 0 10"
        };
        int[] from = {49, 49, 9, 9};
        int[] to = {57, 56, 1, 0};
        Position position = new Position();
        Position.UndoState undo = new Position.UndoState();
        for (int i = 0; i < before.length; i++)
        {
            position.SetPosition(before[i]);
            position.MakeMove(Move.Encode(from[i], to[i], type), undo);
            AssertPosition(after[i], position);
            position.UnmakeMove(undo);
            AssertPosition(before[i], position);
        }
    }

    private static void AssertPosition(String fen, Position actual)
    {
        assertEquals(fen, actual.ToFen());
        Position expected = new Position();
        expected.SetPosition(fen);
        for (int piece = 0; piece < Piece.COUNT; piece++)
            assertEquals(expected.GetBitBoard(piece), actual.GetBitBoard(piece));
        assertEquals(expected.GetOccupancy(Piece.WHITE), actual.GetOccupancy(Piece.WHITE));
        assertEquals(expected.GetOccupancy(Piece.BLACK), actual.GetOccupancy(Piece.BLACK));
        assertEquals(expected.GetOccupancy(), actual.GetOccupancy());
    }
}
