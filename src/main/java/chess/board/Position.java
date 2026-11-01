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

    private long m_WhiteOccupancy;
    private long m_BlackOccupancy;
    private int m_SideToMove;
    private int m_CastlingRights;
    private int m_EnPassantSquare;
    private int m_HalfmoveClock;
    private int m_FullmoveNumber;

    public Position()
    {
        SetStartingPosition();
    }

    public void SetStartingPosition()
    {
        SetPosition(STARTING_FEN);
    }

    public static final class UndoState
    {
        private int move, piece, placedPiece, captured;
        private int castlingRights, enPassantSquare, halfmoveClock, fullmoveNumber;
    }

    public void MakeMove(int move, UndoState undo)
    {
        int from = Move.GetFrom(move);
        int to = Move.GetTo(move);
        int type = Move.GetType(move);
        int piece = GetPiece(from);
        int captured = GetPiece(to);

        boolean pawn = (piece == Piece.WHITE_PAWN || piece == Piece.BLACK_PAWN);

        // Figures out what piece needs to be placed
        int placedPiece = type >= Move.PROMOTE_KNIGHT
                ? m_SideToMove * 6 + Piece.WHITE_KNIGHT + type - Move.PROMOTE_KNIGHT : piece;

        int halfmoveClock = pawn || captured != Piece.NONE ? 0 : m_HalfmoveClock + 1;
        int fullmoveNumber = m_SideToMove == Piece.BLACK
                ? m_FullmoveNumber + 1 : m_FullmoveNumber;

        undo.move = move;
        undo.piece = piece;
        undo.placedPiece = placedPiece;
        undo.captured = captured;
        undo.castlingRights = m_CastlingRights;
        undo.enPassantSquare = m_EnPassantSquare;
        undo.halfmoveClock = m_HalfmoveClock;
        undo.fullmoveNumber = m_FullmoveNumber;

        if (captured != Piece.NONE)
            TogglePiece(captured, 1L << to);

        TogglePiece(piece, 1L << from);
        TogglePiece(placedPiece, 1L << to);
        m_CastlingRights &= ~(CastlingRightsAt(from) | CastlingRightsAt(to));
        m_EnPassantSquare = pawn && Math.abs(to - from) == 16 ? (from + to) / 2 : NO_SQUARE;
        m_HalfmoveClock = halfmoveClock;
        m_FullmoveNumber = fullmoveNumber;
        m_SideToMove ^= 1;
    }

    public void UnmakeMove(UndoState undo)
    {
        int from = Move.GetFrom(undo.move);
        int to = Move.GetTo(undo.move);
        TogglePiece(undo.placedPiece, 1L << to);
        TogglePiece(undo.piece, 1L << from);
        if (undo.captured != Piece.NONE)
            TogglePiece(undo.captured, 1L << to);
        m_CastlingRights = undo.castlingRights;
        m_EnPassantSquare = undo.enPassantSquare;
        m_HalfmoveClock = undo.halfmoveClock;
        m_FullmoveNumber = undo.fullmoveNumber;
        m_SideToMove ^= 1;
    }

    private void TogglePiece(int piece, long squares)
    {
        m_BitBoards[piece] ^= squares;
        if (piece < Piece.BLACK_PAWN)
            m_WhiteOccupancy ^= squares;
        else
            m_BlackOccupancy ^= squares;
    }

    private static int CastlingRightsAt(int square)
    {
        return switch (square)
        {
            case 0 -> WHITE_QUEENSIDE;
            case 4 -> WHITE_KINGSIDE | WHITE_QUEENSIDE;
            case 7 -> WHITE_KINGSIDE;
            case 56 -> BLACK_QUEENSIDE;
            case 60 -> BLACK_KINGSIDE | BLACK_QUEENSIDE;
            case 63 -> BLACK_KINGSIDE;
            default -> 0;
        };
    }

    public String ToFen()
    {
        StringBuilder fen = new StringBuilder();

        // Add pieces to fen
        for (int rank = 7; rank >= 0; rank--)
        {
            int emptySquares = 0;
            for (int file = 0; file < 8; file++)
            {
                int piece = GetPiece(rank * 8 + file);
                if (piece == Piece.NONE)
                {
                    emptySquares++;
                    continue;
                }
                if (emptySquares > 0)
                {
                    fen.append(emptySquares);
                    emptySquares = 0;
                }
                fen.append(Piece.ToFenChar(piece));

            }
            if (emptySquares > 0)
                fen.append(emptySquares);
            if (rank > 0)
                fen.append('/');
        }

        fen.append(' ');

        fen.append(m_SideToMove == Piece.WHITE ? "w" : "b");

        fen.append(' ');

        if (m_CastlingRights == 0)
            fen.append('-');
        else
        {
            if ((m_CastlingRights & WHITE_KINGSIDE) != 0)
                fen.append('K');
            if ((m_CastlingRights & WHITE_QUEENSIDE) != 0)
                fen.append('Q');
            if ((m_CastlingRights & BLACK_KINGSIDE) != 0)
                fen.append('k');
            if ((m_CastlingRights & BLACK_QUEENSIDE) != 0)
                fen.append('q');
        }

        fen.append(' ');

        if (m_EnPassantSquare == NO_SQUARE)
            fen.append('-');
        else
        {
            fen.append((char)('a' + m_EnPassantSquare % 8));
            fen.append((char)('1' + m_EnPassantSquare / 8));
        }

        fen.append(' ');

        fen.append(m_HalfmoveClock);

        fen.append(' ');

        fen.append(m_FullmoveNumber);

        return fen.toString();
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

        long whiteOccupancy = 0;
        long blackOccupancy = 0;

        for (int piece = Piece.WHITE_PAWN; piece <= Piece.WHITE_KING; piece++)
        {
            whiteOccupancy |= bitBoards[piece];
        }
        for (int piece = Piece.BLACK_PAWN; piece <= Piece.BLACK_KING; piece++)
        {
            blackOccupancy |= bitBoards[piece];
        }

        m_BitBoards = bitBoards;
        m_WhiteOccupancy = whiteOccupancy;
        m_BlackOccupancy = blackOccupancy;
        m_SideToMove = sideToMove;
        m_CastlingRights = castlingRights;
        m_EnPassantSquare = enPassantSquare;
        m_HalfmoveClock = halfmoveClock;
        m_FullmoveNumber = fullmoveNumber;

    }

    public int GetPiece(int square)
    {
        if (square < 0 || 63 < square)
        {
            throw new IllegalArgumentException("Invalid square: " + square);
        }

        long mask = 1L << square;

        for (int piece = 0; piece < Piece.COUNT; piece++)
        {
            if ((m_BitBoards[piece] & mask) != 0)
            {
                return piece;
            }
        }
        return Piece.NONE;
    }

    public long GetBitBoard(int piece)
    {
        if (piece < 0 || Piece.COUNT <= piece)
        {
            throw new IllegalArgumentException("Invalid piece index: " + piece);
        }
        return m_BitBoards[piece];
    }

    public long GetOccupancy(int color)
    {
        return switch (color)
        {
            case Piece.WHITE -> m_WhiteOccupancy;
            case Piece.BLACK -> m_BlackOccupancy;
            default -> throw new IllegalArgumentException("Invalid color: " + color);
        };
    }

    public long GetOccupancy()
    {
        return m_WhiteOccupancy | m_BlackOccupancy;
    }

    public int GetSideToMove()
    {
        return m_SideToMove;
    }

    public int GetCastlingRights()
    {
        return m_CastlingRights;
    }

    public int GetEnPassantSquare()
    {
        return m_EnPassantSquare;
    }

    public int GetHalfmoveClock()
    {
        return m_HalfmoveClock;
    }

    public int GetFullmoveNumber()
    {
        return m_FullmoveNumber;
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
                    break;
                }
                case 'Q':
                {
                    rights |= WHITE_QUEENSIDE;
                    break;
                }
                case 'k':
                {
                    rights |= BLACK_KINGSIDE;
                    break;
                }
                case 'q':
                {
                    rights |= BLACK_QUEENSIDE;
                    break;
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
