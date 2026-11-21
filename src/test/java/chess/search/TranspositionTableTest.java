package chess.search;

import chess.board.Move;
import chess.board.Position;
import chess.search.TranspositionTable.Bound;
import chess.search.TranspositionTable.Entry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class TranspositionTableTest
{
    private static final Position.DrawContext CONTEXT = new Position().CaptureDrawContext();

    @ParameterizedTest
    @EnumSource(Bound.class)
    void storesAllEntryFieldsForEachBound(Bound bound)
    {
        TranspositionTable table = new TranspositionTable(3);
        long key = Long.MIN_VALUE;
        int move = Move.Encode(52, 60, Move.PROMOTE_QUEEN);

        assertNull(table.Probe(key));
        table.Store(new Entry(key, 4, -125, bound, move, CONTEXT, false));

        assertEquals(new Entry(key, 4, -125, bound, move, CONTEXT, false), table.Probe(key));
    }

    @ParameterizedTest
    @EnumSource(Bound.class)
    void onlyExactEntriesAtTheRequestedDepthCanSupplyScores(Bound bound)
    {
        Position position = new Position();
        TranspositionTable table = new TranspositionTable(1);
        int move = Move.Encode(6, 21, Move.NORMAL);
        Entry entry = new Entry(position.GetPositionKey(), 3, 125, bound, move, CONTEXT, true);
        table.Store(entry);

        assertEquals(bound == Bound.EXACT ? entry : null, table.ProbeExact(position, 3));
        assertNull(table.ProbeExact(position, 2));
        assertNull(table.ProbeExact(position, 4));
        assertEquals(move, table.ProbeMove(position.GetPositionKey()));
    }

    @Test
    void checksTheFullKeyAndReplacesCollisionsEvenAtLowerDepth()
    {
        TranspositionTable table = new TranspositionTable(4);
        long collision = 1L << 32;
        Entry first = new Entry(0, 7, 100, Bound.EXACT, Move.NONE, CONTEXT, false);
        Entry second = new Entry(collision, 1, 70, Bound.LOWER, Move.NONE, CONTEXT, false);

        table.Store(first);
        assertEquals(first, table.Probe(0));
        assertNull(table.Probe(collision));

        table.Store(second);
        assertNull(table.Probe(0));
        assertEquals(second, table.Probe(collision));
    }

    @Test
    void probesMoveHintsByFullKeyWithoutReturningAScore()
    {
        TranspositionTable table = new TranspositionTable(1);
        long key = 1L << 32;
        int move = Move.Encode(1, 18, Move.NORMAL);

        table.StoreMove(key, 3, move);

        assertEquals(move, table.ProbeMove(key));
        assertEquals(Move.NONE, table.ProbeMove(0));
        assertNull(table.Probe(key));
    }

    @Test
    void keepsDeeperEntriesForTheSameKeyAndReplacesAtEqualOrGreaterDepth()
    {
        TranspositionTable table = new TranspositionTable(1);
        long key = 5;
        Entry original = new Entry(key, 4, 20, Bound.LOWER, Move.NONE, CONTEXT, false);
        Entry equalDepth = new Entry(key, 4, 30, Bound.EXACT, Move.NONE, CONTEXT, false);
        Entry deeper = new Entry(key, 5, 40, Bound.UPPER, Move.NONE, CONTEXT, false);

        table.Store(original);
        table.Store(new Entry(key, 3, 10, Bound.EXACT, Move.NONE, CONTEXT, false));
        assertEquals(original, table.Probe(key));

        table.Store(equalDepth);
        assertEquals(equalDepth, table.Probe(key));

        table.Store(deeper);
        assertEquals(deeper, table.Probe(key));
    }

    @Test
    void clearsEverySlotAndCanBeReused()
    {
        TranspositionTable table = new TranspositionTable(3);
        for (long key = 0; key < 3; key++)
            table.Store(new Entry(key, 6, 20, Bound.LOWER, Move.NONE, CONTEXT, false));

        table.Clear();
        for (long key = 0; key < 3; key++)
            assertNull(table.Probe(key));
        assertEquals(3, table.GetCapacity());

        Entry entry = new Entry(0, 1, 0, Bound.EXACT, Move.NONE, CONTEXT, false);
        table.Store(entry);
        assertEquals(entry, table.Probe(0));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 4})
    void retainsOnlyTheRequestedNumberOfSlots(int capacity)
    {
        TranspositionTable table = new TranspositionTable(capacity);
        for (long key = 0; key < 10L * capacity; key++)
            table.Store(new Entry(key, 0, 0, Bound.EXACT, Move.NONE, CONTEXT, false));

        assertEquals(capacity, table.GetCapacity());
        for (long key = 0; key < 9L * capacity; key++)
            assertNull(table.Probe(key));
        for (long key = 9L * capacity; key < 10L * capacity; key++)
            assertEquals(new Entry(key, 0, 0, Bound.EXACT, Move.NONE, CONTEXT, false), table.Probe(key));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void rejectsNonpositiveCapacities(int capacity)
    {
        assertThrows(IllegalArgumentException.class, () -> new TranspositionTable(capacity));
    }

    @Test
    void rejectsInvalidEntryMetadata()
    {
        assertThrows(IllegalArgumentException.class, () -> new Entry(0, -1, 0, Bound.EXACT, Move.NONE, CONTEXT, false));
        assertThrows(NullPointerException.class, () -> new Entry(0, 1, 0, null, Move.NONE, CONTEXT, false));
        assertThrows(NullPointerException.class, () -> new Entry(0, 1, 0, Bound.EXACT, Move.NONE, null, false));
    }
}
