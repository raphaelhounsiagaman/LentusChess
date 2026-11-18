package chess.search;

import chess.board.Move;
import chess.search.TranspositionTable.Bound;
import chess.search.TranspositionTable.Entry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class TranspositionTableTest
{
    @ParameterizedTest
    @EnumSource(Bound.class)
    void storesAllEntryFieldsForEachBound(Bound bound)
    {
        TranspositionTable table = new TranspositionTable(3);
        long key = Long.MIN_VALUE;
        int move = Move.Encode(52, 60, Move.PROMOTE_QUEEN);

        assertNull(table.Probe(key));
        table.Store(new Entry(key, 4, -125, bound, move));

        assertEquals(new Entry(key, 4, -125, bound, move), table.Probe(key));
    }

    @Test
    void checksTheFullKeyAndReplacesCollisionsEvenAtLowerDepth()
    {
        TranspositionTable table = new TranspositionTable(4);
        long collision = 1L << 32;
        Entry first = new Entry(0, 7, 100, Bound.EXACT, Move.NONE);
        Entry second = new Entry(collision, 1, 70, Bound.LOWER, Move.NONE);

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
        Entry original = new Entry(key, 4, 20, Bound.LOWER, Move.NONE);
        Entry equalDepth = new Entry(key, 4, 30, Bound.EXACT, Move.NONE);
        Entry deeper = new Entry(key, 5, 40, Bound.UPPER, Move.NONE);

        table.Store(original);
        table.Store(new Entry(key, 3, 10, Bound.EXACT, Move.NONE));
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
            table.Store(new Entry(key, 6, 20, Bound.LOWER, Move.NONE));

        table.Clear();
        for (long key = 0; key < 3; key++)
            assertNull(table.Probe(key));
        assertEquals(3, table.GetCapacity());

        Entry entry = new Entry(0, 1, 0, Bound.EXACT, Move.NONE);
        table.Store(entry);
        assertEquals(entry, table.Probe(0));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 4})
    void retainsOnlyTheRequestedNumberOfSlots(int capacity)
    {
        TranspositionTable table = new TranspositionTable(capacity);
        for (long key = 0; key < 10L * capacity; key++)
            table.Store(new Entry(key, 0, 0, Bound.EXACT, Move.NONE));

        assertEquals(capacity, table.GetCapacity());
        for (long key = 0; key < 9L * capacity; key++)
            assertNull(table.Probe(key));
        for (long key = 9L * capacity; key < 10L * capacity; key++)
            assertEquals(new Entry(key, 0, 0, Bound.EXACT, Move.NONE), table.Probe(key));
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
        assertThrows(IllegalArgumentException.class, () -> new Entry(0, -1, 0, Bound.EXACT, Move.NONE));
        assertThrows(NullPointerException.class, () -> new Entry(0, 1, 0, null, Move.NONE));
    }
}
