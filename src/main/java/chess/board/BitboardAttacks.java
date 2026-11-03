package chess.board;

final class BitboardAttacks
{
    private static final long FILE_A = 0x0101010101010101L;
    private static final long FILE_H = 0x8080808080808080L;

    private static final int[][] KNIGHT_STEPS = {
            {1, 2}, {2, 1}, {2, -1}, {1, -2},
            {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}
    };
    private static final int[][] ROOK_STEPS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] BISHOP_STEPS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final int[][] KING_STEPS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };
    private static final long[] KNIGHT_ATTACKS = BuildLeaperAttacks(KNIGHT_STEPS);
    private static final long[] KING_ATTACKS = BuildLeaperAttacks(KING_STEPS);
    private static final long[] BISHOP_OCCUPANCY_MASKS = BuildOccupancyMasks(BISHOP_STEPS);
    private static final long[] ROOK_OCCUPANCY_MASKS = BuildOccupancyMasks(ROOK_STEPS);

    private BitboardAttacks()
    {
    }

    static long PawnAttacks(long pawns, int color)
    {
        return color == Piece.WHITE ?
                ((pawns & ~FILE_A) << 7) | ((pawns & ~FILE_H) << 9) :
                ((pawns & ~FILE_A) >>> 9) | ((pawns & ~FILE_H) >>> 7);
    }

    static long KnightAttacks(int square)
    {
        return KNIGHT_ATTACKS[square];
    }

    static long KingAttacks(int square)
    {
        return KING_ATTACKS[square];
    }

    static long BishopAttacks(int square, long occupancy)
    {
        return SlidingAttacks(square, occupancy, BISHOP_STEPS);
    }

    static long RookAttacks(int square, long occupancy)
    {
        return SlidingAttacks(square, occupancy, ROOK_STEPS);
    }

    static long BishopOccupancyMask(int square)
    {
        return BISHOP_OCCUPANCY_MASKS[square];
    }

    static long RookOccupancyMask(int square)
    {
        return ROOK_OCCUPANCY_MASKS[square];
    }

    private static long[] BuildLeaperAttacks(int[][] steps)
    {
        long[] attacks = new long[64];
        for (int square = 0; square < 64; square++)
        {
            for (int[] step : steps)
            {
                int file = square % 8 + step[0];
                int rank = square / 8 + step[1];
                if (OnBoard(file, rank))
                    attacks[square] |= 1L << (rank * 8 + file);
            }
        }
        return attacks;
    }

    private static long[] BuildOccupancyMasks(int[][] steps)
    {
        long[] masks = new long[64];
        for (int square = 0; square < 64; square++)
        {
            for (int[] step : steps)
            {
                int file = square % 8 + step[0];
                int rank = square / 8 + step[1];

                while (OnBoard(file, rank) && OnBoard(file + step[0], rank + step[1]))
                {
                    masks[square] |= 1L << (rank * 8 + file);
                    file += step[0];
                    rank += step[1];
                }
            }
        }
        return masks;
    }

    private static long SlidingAttacks(int square, long occupancy, int[][] steps)
    {
        long attacks = 0;
        for (int[] step : steps)
        {
            int file = square % 8 + step[0];
            int rank = square / 8 + step[1];
            while (OnBoard(file, rank))
            {
                long target = 1L << (rank * 8 + file);
                attacks |= target;
                if ((occupancy & target) != 0)
                    break;
                file += step[0];
                rank += step[1];
            }
        }
        return attacks;
    }

    private static boolean OnBoard(int file, int rank)
    {
        return 0 <= file && file < 8 &&
                0 <= rank && rank < 8;
    }
}
