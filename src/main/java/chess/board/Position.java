package chess.board;

public class Position
{
    public static final String STARTING_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public static final int WHITE_KINGSIDE = 1;
    public static final int WHITE_QUEENSIDE = 2;
    public static final int BLACK_KINGSIDE = 4;
    public static final int BLACK_QUEENSIDE = 8;

    public static final int NO_SQUARE = -1;

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
                break;
            }
            case "b":
            {
                sideToMove = Piece.BLACK;
                break;
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

        m_BitBoards = bitBoards;

    }

    private long[] ParsePlacement(String placement)
    {
        long[] bitBoards = new long[Piece.COUNT];
        String[] ranks = placement.split("/", -1);
        if (ranks.length != 8)
        {
            throw new IllegalArgumentException("FEN must have 8 ranks");
        }

        for (int rank = 7; rank >= 0; rank--)
        {
            int file = 0;

            for (char symbol : ranks[7 - rank].toCharArray()) {
                if ('1' <= symbol && symbol <= '8') {
                    file += symbol - '0';
                } else {
                    int piece = Piece.FromFenChar(symbol);
                    bitBoards[piece] |= 1L << ((rank * 8) + file);

                    file++;
                }
            }
        }

        return bitBoards;
    }

    private int ParseCastlingRights(String castlingRights)
    {
        if (castlingRights.equals("-"))
        {
            return 0;
        }
        int rights = 0;

        for (char symbol : castlingRights.toCharArray())
        {
            switch (symbol)
            {
                case 'K':
                {
                    rights |= WHITE_KINGSIDE;
                }
                case 'Q':
                {
                    rights |= WHITE_QUEENSIDE;
                }
                case 'k':
                {
                    rights |= BLACK_KINGSIDE;
                }
                case 'q':
                {
                    rights |= BLACK_QUEENSIDE;
                }
            }

        }

        return rights;
    }

    private int ParseEnPassantSquare(String square, int sideToMove)
    {
        if (square.equals("-"))
        {
            return NO_SQUARE;
        }

        return (square.charAt(1) - '1') * 8 + square.charAt(0) - 'a';
    }

    private int ParseCounter(String counter)
    {
        for (char symbol : counter.toCharArray())
        {
            if (symbol < '0' || symbol > '9')
            {
                throw new IllegalArgumentException("Can only parse counter from numbers");
            }
        }
        int value;
        try
        {
            value = Integer.parseInt(counter);
        }
        catch (NumberFormatException exception)
        {
            throw new IllegalArgumentException("FEN surpasses integer range", exception);
        }
        return value;
    }


}
