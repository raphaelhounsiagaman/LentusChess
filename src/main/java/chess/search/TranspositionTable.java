package chess.search;

import chess.board.Move;

import java.util.Arrays;
import java.util.Objects;

/** Fixed-capacity, direct-mapped table for a single search thread. */
final class TranspositionTable
{
    enum Bound
    {
        EXACT,
        LOWER,
        UPPER
    }

    /**
     * Depth is the remaining search depth in plies. The score is from the side to move's
     * perspective, with mate distance relative to this position. Best move uses the engine's
     * move encoding, with {@link Move#NONE} when no move is available.
     */
    record Entry(long key, int depth, int score, Bound bound, int bestMove)
    {
        Entry
        {
            if (depth < 0)
                throw new IllegalArgumentException("Search depth must be nonnegative");
            Objects.requireNonNull(bound, "bound");
        }
    }

    private final Entry[] m_Entries;

    TranspositionTable(int capacity)
    {
        if (capacity < 1)
            throw new IllegalArgumentException("Table capacity must be positive");
        m_Entries = new Entry[capacity];
    }

    int GetCapacity()
    {
        return m_Entries.length;
    }

    /** Returns the entry for the full key, or null on a miss. */
    Entry Probe(long key)
    {
        Entry entry = m_Entries[Index(key)];
        return entry != null && entry.key() == key ? entry : null;
    }

    /** Fills empty slots, replaces collisions, and keeps deeper entries for the same key. */
    void Store(Entry entry)
    {
        int index = Index(entry.key());
        Entry previous = m_Entries[index];
        if (previous == null || previous.key() != entry.key() || previous.depth() <= entry.depth())
            m_Entries[index] = entry;
    }

    void Clear()
    {
        Arrays.fill(m_Entries, null);
    }

    private int Index(long key)
    {
        return (int) Long.remainderUnsigned(key, m_Entries.length);
    }
}
