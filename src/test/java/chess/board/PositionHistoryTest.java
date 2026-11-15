package chess.board;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class PositionHistoryTest
{
    @Test
    void fenLoadingResetsKnownHistoryWithoutInventingEarlierPositions()
    {
        Position position = new Position();
        long startingKey = position.GetPositionKey();
        assertArrayEquals(new long[]{startingKey}, position.GetPositionHistory());
        position.MakeMove(Move.Encode(6, 21, Move.NORMAL), new Position.UndoState());
        long[] history = position.GetPositionHistory();
        assertThrows(IllegalArgumentException.class, () -> position.SetPosition("invalid"));
        assertArrayEquals(history, position.GetPositionHistory());

        position.SetPosition("4k3/8/8/8/8/8/8/4K3 b - - 99 50");
        assertEquals(99, position.GetHalfmoveClock());
        assertArrayEquals(new long[]{position.GetPositionKey()}, position.GetPositionHistory());
        position.SetStartingPosition();
        assertArrayEquals(new long[]{startingKey}, position.GetPositionHistory());
    }

    @Test
    void longHistorySurvivesGrowthNestedUndoAndUndoRecordReuse()
    {
        Position position = new Position();
        int[] from = {6, 62, 21, 45};
        int[] to = {21, 45, 6, 62};
        Position.UndoState[] undo = new Position.UndoState[300];
        long[] expected = new long[undo.length + 1];
        expected[0] = position.GetPositionKey();
        for (int i = 0; i < undo.length; i++)
        {
            undo[i] = new Position.UndoState();
            position.MakeMove(Move.Encode(from[i % 4], to[i % 4], Move.NORMAL), undo[i]);
            expected[i + 1] = position.GetPositionKey();
        }
        assertEquals(expected[0], expected[300]);
        assertArrayEquals(expected, position.GetPositionHistory());
        for (int i = undo.length - 1; i >= 0; i--)
        {
            position.UnmakeMove(undo[i]);
            assertArrayEquals(Arrays.copyOf(expected, i + 1), position.GetPositionHistory());
        }
        assertEquals(Position.STARTING_FEN, position.ToFen());

        // A different continuation must replace the discarded branch.
        position.MakeMove(Move.Encode(12, 28, Move.NORMAL), undo[0]);
        assertArrayEquals(new long[]{expected[0], position.GetPositionKey()}, position.GetPositionHistory());
        position.UnmakeMove(undo[0]);
        assertArrayEquals(new long[]{expected[0]}, position.GetPositionHistory());
    }

    @Test
    void copiesPreserveAllStateAndCanFollowIndependentContinuations()
    {
        Position original = new Position();
        original.MakeMove(Move.Encode(12, 28, Move.NORMAL), new Position.UndoState());
        Position copy = new Position(original);
        String fen = original.ToFen();
        long[] history = original.GetPositionHistory();
        assertEquals(fen, copy.ToFen());
        assertEquals(original.GetPositionKey(), copy.GetPositionKey());
        assertEquals(original.GetOccupancy(Piece.WHITE), copy.GetOccupancy(Piece.WHITE));
        assertEquals(original.GetOccupancy(Piece.BLACK), copy.GetOccupancy(Piece.BLACK));
        assertArrayEquals(history, copy.GetPositionHistory());

        long[] exported = copy.GetPositionHistory();
        exported[0] ^= 1;
        assertArrayEquals(history, copy.GetPositionHistory());

        Position.UndoState undo = new Position.UndoState();
        copy.MakeMove(Move.Encode(52, 36, Move.NORMAL), undo);
        String copyFen = copy.ToFen();
        long[] copyHistory = copy.GetPositionHistory();
        assertEquals(fen, original.ToFen());
        assertArrayEquals(history, original.GetPositionHistory());
        original.MakeMove(Move.Encode(51, 35, Move.NORMAL), new Position.UndoState());
        assertEquals(copyFen, copy.ToFen());
        assertArrayEquals(copyHistory, copy.GetPositionHistory());
        copy.UnmakeMove(undo);
        assertEquals(fen, copy.ToFen());
        assertArrayEquals(history, copy.GetPositionHistory());
        assertEquals(3, original.GetPositionHistory().length);
    }

    @Test
    void legalMoveGenerationAndPerftRestoreExistingHistory()
    {
        Position position = new Position();
        position.MakeMove(Move.Encode(6, 21, Move.NORMAL), new Position.UndoState());
        String fen = position.ToFen();
        long[] history = position.GetPositionHistory();
        assertEquals(20, new MoveGenerator().GenerateLegalMoves(position).size());
        assertArrayEquals(history, position.GetPositionHistory());
        assertTrue(new Perft().Count(position, 3) > 0);
        assertEquals(fen, position.ToFen());
        assertArrayEquals(history, position.GetPositionHistory());
    }
}
