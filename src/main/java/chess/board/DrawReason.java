package chess.board;

public enum DrawReason
{
    NONE,
    THREEFOLD_REPETITION,
    FIFTY_MOVE_RULE,
    INSUFFICIENT_MATERIAL;

    /** These results cannot be cached using only the Zobrist position key. */
    public boolean IsHistoryDependent()
    {
        return this == THREEFOLD_REPETITION || this == FIFTY_MOVE_RULE;
    }
}
