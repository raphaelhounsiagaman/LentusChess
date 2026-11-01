package chess.board;

/**
 * Source in bits 0-5, destination in 6-11, type in 12-14.
 * Squares use a1 = 0, b1 = 1, ... through h8 = 63.
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

    public static String ToUci(int move)
    {
        if (move == NONE)
            return "0000";
        int from = GetFrom(move);
        int to = GetTo(move);
        String text = "" + (char) ('a' + from % 8) + (char) ('1' + from / 8)
                + (char) ('a' + to % 8) + (char) ('1' + to / 8);
        int type = GetType(move);
        if (type >= PROMOTE_KNIGHT && type <= PROMOTE_QUEEN)
            text += "nbrq".charAt(type - PROMOTE_KNIGHT);
        return text;
    }
}
