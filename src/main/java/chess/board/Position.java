package chess.board;

public class Position
{
    public static final String STARTING_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private long[] m_BitBoards;

    public Position()
    {
        SetStartingPosition();
    }

    public void SetStartingPosition()
    {
        SetPosition(STARTING_FEN);
    }

    public void SetPosition(String fen)
    {
        if (fen == null || fen.isBlank())
        {
            throw new IllegalArgumentException("FEN cannot be empty");
        }
        String[] fenParts = fen.trim().split("\\s+");
        if (fenParts.length != 6)
        {
            throw new IllegalArgumentException("FEN must have 6 elements");
        }

        long[] bitBoards = ParsePlacement(fenParts[0]);

        int sideToMove;

        switch (fenParts[1])
        {
            case "w":
            {
                sideToMove = Piece.WHITE;
            }
            case "b":
            {
                sideToMove = Piece.BLACK;
            }
            default:
            {
                throw new IllegalArgumentException("Invalid FEN side to move");
            }
        }

        int castlingRights = ParseCastlingRights(fenParts[2]);
        int enPassantSquare = ParseEnPassantSquare(fenParts[3], sideToMove);
        int halfmoveClock = ParseCounter(fenParts[4]);
        int fullmoveNumber = ParseCounter(fenParts[5]);




    }

    private long[] ParsePlacement(String placement)
    {
        long[] bitBoards = new long[Piece.COUNT];
        return bitBoards;
    }

    private int ParseCastlingRights(String castlingRights)
    {
        return 0;
    }

    private int ParseEnPassantSquare(String square, int sideToMove)
    {
        return -1;
    }

    private int ParseCounter(String counter)
    {
        return 1;
    }


}
