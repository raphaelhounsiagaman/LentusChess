package chess.board;

/**
 * Source in bits 0-5, destination in 6-11, type in 12-14.
 * Squares use a1 = 0, b1 = 2, ... through h8 = 63.
 */
public final class Move
{
    public static final int NONE = 0;

    // NORMAL includes captures and double pawn pushes; the position distinguishes them.
    public static final int NORMAL = 0;
    public static final int CASTLING = 1;
    public static final int EN_PASSANT = 2;
    public static final int PROMOTE_KNIGHT = 3;
    public static final int PROMOTE_BISHOP = 4;
    public static final int PROMOTE_ROOK = 5;
    public static final int PROMOTE_QUEEN = 6;

    private Move()
    {
    }

    public static int Encode(int from, int to, int type)
    {
        if (from < 0 || 63 < from  || to < 0 || 63 < to || from == to)
        {
            throw new IllegalArgumentException("Invalid move squares");
        }
        if (type < NORMAL || PROMOTE_QUEEN < type)
        {
            throw new IllegalArgumentException("Invalid move type: " + type);
        }
        return from | (to << 6) | (type << 12);
    }

    public static int GetFrom(int move)
    {
        return move & 63;
    }

    public static int GetTo(int move)
    {
        return (move >>> 6) & 63;
    }

    public static int GetType(int move)
    {
        return (move >>> 12) & 7;
    }
}
