package chess.search;

import chess.board.Move;
import chess.board.Position;

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
     * move encoding, with {@link Move#NONE} when no move is available. The draw context
     * belongs to the stored node, and historyDependent describes draws used by its search.
     */
    record Entry(long key, int depth, int score, Bound bound, int bestMove,
                 Position.DrawContext drawContext, boolean historyDependent) implements Slot
    {
        Entry
        {
            if (depth < 0)
                throw new IllegalArgumentException("Search depth must be nonnegative");
            Objects.requireNonNull(bound, "bound");
            Objects.requireNonNull(drawContext, "drawContext");
        }
    }

    private sealed interface Slot permits Entry, MoveHint
    {
        long key();
        int depth();
        int bestMove();
    }

    private record MoveHint(long key, int depth, int bestMove) implements Slot
    {
    }

    private final Slot[] m_Entries;

    TranspositionTable(int capacity)
    {
        if (capacity < 1)
            throw new IllegalArgumentException("Table capacity must be positive");
        m_Entries = new Slot[capacity];
    }

    int GetCapacity()
    {
        return m_Entries.length;
    }

    /** Looks up a scored entry by key. Use ProbeExact for score reuse. */
    Entry Probe(long key)
    {
        return ProbeSlot(key) instanceof Entry entry ? entry : null;
    }

    /** Returns an exact score entry when the key, depth, and draw context match. */
    Entry ProbeExact(Position position, int depth)
    {
        Entry entry = Probe(position.GetPositionKey());
        if (entry != null && entry.bound() == Bound.EXACT && entry.depth() == depth
                && position.MatchesDrawContext(entry.drawContext()))
            return entry;
        return null;
    }

    /** Returns the stored move for the full key, or {@link Move#NONE} on a miss. */
    int ProbeMove(long key)
    {
        Slot slot = ProbeSlot(key);
        return slot != null ? slot.bestMove() : Move.NONE;
    }

    void Store(Entry entry)
    {
        StoreSlot(entry);
    }

    void StoreMove(long key, int depth, int bestMove)
    {
        StoreSlot(new MoveHint(key, depth, bestMove));
    }

    private Slot ProbeSlot(long key)
    {
        Slot slot = m_Entries[Index(key)];
        return slot != null && slot.key() == key ? slot : null;
    }

    /** Fills empty slots, replaces collisions, and keeps deeper entries for the same key. */
    private void StoreSlot(Slot slot)
    {
        int index = Index(slot.key());
        Slot previous = m_Entries[index];
        if (previous == null || previous.key() != slot.key() || previous.depth() <= slot.depth())
            m_Entries[index] = slot;
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
