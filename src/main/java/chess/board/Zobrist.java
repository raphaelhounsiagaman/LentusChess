package chess.board;

import java.util.Random;

final class Zobrist
{
    private static final long[][] PIECES = new long[Piece.COUNT][64];
    private static final long[] CASTLING = new long[16];
    private static final long[] EN_PASSANT = new long[8];
    private static final long BLACK_TO_MOVE;

    static
    {
        // Fixed seed keeps position keys reproducible across runs.
        Random random = new Random(0x4C454E545553L);
        for (long[] squares : PIECES)
            for (int square = 0; square < squares.length; square++)
                squares[square] = random.nextLong();
        for (int rights = 0; rights < CASTLING.length; rights++)
            CASTLING[rights] = random.nextLong();
        for (int file = 0; file < EN_PASSANT.length; file++)
            EN_PASSANT[file] = random.nextLong();
        BLACK_TO_MOVE = random.nextLong();
    }

    private Zobrist()
    {
    }

    static long PieceKey(int piece, long squares)
    {
        long key = 0;
        while (squares != 0)
        {
            key ^= PIECES[piece][Long.numberOfTrailingZeros(squares)];
            squares &= squares - 1;
        }
        return key;
    }

    static long StateKey(Position position)
    {
        long key = CASTLING[position.GetCastlingRights()];
        if (position.GetSideToMove() == Piece.BLACK)
            key ^= BLACK_TO_MOVE;
        if (MoveGenerator.HasLegalEnPassant(position))
            key ^= EN_PASSANT[position.GetEnPassantSquare() % 8];
        return key;
    }

    static long ComputeKey(Position position)
    {
        long key = StateKey(position);
        for (int piece = 0; piece < Piece.COUNT; piece++)
            key ^= PieceKey(piece, position.GetBitBoard(piece));
        return key;
    }
}
