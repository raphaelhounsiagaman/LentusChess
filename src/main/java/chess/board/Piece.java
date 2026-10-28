package chess.board;

public final class Piece
{
    public static final int NONE = -1;
    public static final int WHITE = 0;
    public static final int BLACK = 1;

    // Each piece index selects one of Position's twelve bitboards.
    public static final int WHITE_PAWN = 0;
    public static final int WHITE_KNIGHT = 1;
    public static final int WHITE_BISHOP = 2;
    public static final int WHITE_ROOK = 3;
    public static final int WHITE_QUEEN = 4;
    public static final int WHITE_KING = 5;
    public static final int BLACK_PAWN = 6;
    public static final int BLACK_KNIGHT = 7;
    public static final int BLACK_BISHOP = 8;
    public static final int BLACK_ROOK = 9;
    public static final int BLACK_QUEEN = 10;
    public static final int BLACK_KING = 11;
    public static final int COUNT = 12;

    private static final String FEN_PIECES = "PNBRQKpnbrqk";

    private Piece()
    {
    }

    public static int FromFenChar(char symbol)
    {
        int piece = FEN_PIECES.indexOf(symbol);
        if (piece == -1)
        {
            throw new IllegalArgumentException("Piece doesn't exist");
        }
        return piece;
    }

    public static char ToFenChar(int piece)
    {
        if (piece < 0 || piece >= COUNT)
        {
            throw new IllegalArgumentException("Invalid piece index: " + piece);
        }
        return FEN_PIECES.charAt(piece);
    }
}
