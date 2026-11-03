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
    // Generated offline for a1 = 0 with Random seed 0x4C454E545553L.
    // Table construction checks every relevant occupancy for conflicting collisions.
    private static final long[] BISHOP_MAGICS = {
            0x6860040420840010L, 0x0008010400821000L, 0x004401020A082820L, 0x08040400880C0100L,
            0x0001104104000420L, 0x0121012010C00400L, 0x1814112838144005L, 0x4008434050086000L,
            0x0400202022108110L, 0x0260823021020890L, 0x200A185294128200L, 0x0860082057400080L,
            0xC008C11040580018L, 0x2800110108400100L, 0x4800885A02202180L, 0x0000120080A41021L,
            0x004010602202D200L, 0x621201100C014C11L, 0x4108001118010010L, 0x020400CA02120088L,
            0x1842000400940400L, 0x0205009A00826100L, 0x1002090402410420L, 0x0110808600840110L,
            0x941048804102AC09L, 0x8010048068010C08L, 0x6002281230038160L, 0x040040414C010200L,
            0x60C1004004004040L, 0x0C040500809000A4L, 0x0002040000690804L, 0x000C810008884842L,
            0x40C4052000400202L, 0x0204020201212420L, 0x2000210106900C00L, 0x0602004040540101L,
            0xC001440C0004C100L, 0x0A04580200C02080L, 0x0048408091841200L, 0x1088004050008200L,
            0x6044442008000418L, 0x08440401282C8540L, 0x0001008044004040L, 0x008128201102480CL,
            0xC001400109002200L, 0x0820021000400208L, 0x0014084811058449L, 0x0101020081100201L,
            0x062C809008210009L, 0x8440804410050801L, 0x8048010048120009L, 0x002A1A0020880102L,
            0x20220210B2020C0FL, 0x0200403082948000L, 0x0044281014008200L, 0x2005500202002080L,
            0x0011002202200400L, 0x00004A008C540200L, 0x0B00002201008848L, 0x002C009000420200L,
            0x48221A0050020A04L, 0xA1908004AD083600L, 0x400040100200A910L, 0x800404102C010010L
    };
    // Generated independently with the same seed and square numbering as the bishops.
    private static final long[] ROOK_MAGICS = {
            0x0A80004000801020L, 0x2140001000200141L, 0x0200081020408200L, 0x618010000C813800L,
            0x4100040801008210L, 0x0F00040048028100L, 0x0400083004024081L, 0x020000840904402AL,
            0x0054800222804000L, 0x0080401000402000L, 0x0000808010002000L, 0x0802805000800802L,
            0x0002800800040080L, 0x0000800200800400L, 0x0812000200240821L, 0x04C9001040810022L,
            0x0034208008804004L, 0x2000808020004010L, 0x0800868010012000L, 0x0520808010000801L,
            0x2000850011000800L, 0x0804008004020080L, 0x1900440008019042L, 0x8452020011244084L,
            0x8220800080204004L, 0x8020002040100040L, 0x8000100280200080L, 0x040A0112000A2040L,
            0x1020040280080080L, 0x8200040080800200L, 0x0802000600081384L, 0x0400004200040081L,
            0x2080002000404000L, 0x0802400288802000L, 0x0000200288801000L, 0x11D4801000800800L,
            0x1404020480800800L, 0x8062012422001048L, 0x0080288204000110L, 0x4140208042000104L,
            0x8000802040108005L, 0x0201004000810020L, 0x5003200011030040L, 0x00050010000B0020L,
            0x0108020004004040L, 0x0001004400490002L, 0x080A020004010100L, 0x4000404081020004L,
            0x0040002080004080L, 0x1000408110220200L, 0x000814832200C200L, 0x0028008510000880L,
            0x0204008008018480L, 0x100C000200800480L, 0x044C500108020400L, 0x2000440081004200L,
            0x0040108000410029L, 0x0000208342041102L, 0x0080100820030041L, 0x080200082040D016L,
            0x0082002150080482L, 0x4141000204000801L, 0x00081A0910080084L, 0x0020110400402082L
    };
    private static final MagicTable[] BISHOP_ATTACKS =
            BuildMagicTables(BISHOP_OCCUPANCY_MASKS, BISHOP_MAGICS, BISHOP_STEPS);
    private static final MagicTable[] ROOK_ATTACKS =
            BuildMagicTables(ROOK_OCCUPANCY_MASKS, ROOK_MAGICS, ROOK_STEPS);

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
        MagicTable table = BISHOP_ATTACKS[square];
        return table.attacks()[table.Index(occupancy)];
    }

    static long RookAttacks(int square, long occupancy)
    {
        MagicTable table = ROOK_ATTACKS[square];
        return table.attacks()[table.Index(occupancy)];
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

    private static MagicTable[] BuildMagicTables(long[] masks, long[] multipliers, int[][] steps)
    {
        MagicTable[] tables = new MagicTable[64];
        for (int square = 0; square < 64; square++)
        {
            long mask = masks[square];
            int bits = Long.bitCount(mask);
            long[] attacks = new long[1 << bits];
            MagicTable table = new MagicTable(mask, multipliers[square], 64 - bits, attacks);
            long occupancy = 0;
            do
            {
                int index = table.Index(occupancy);
                long attack = SlidingAttacks(square, occupancy, steps);
                // Sliding attacks are never empty, so zero marks an unused entry.
                if (attacks[index] != 0 && attacks[index] != attack)
                    throw new IllegalStateException("Magic collision at square " + square);
                attacks[index] = attack;
                occupancy = (occupancy - mask) & mask;
            }
            while (occupancy != 0);
            tables[square] = table;
        }
        return tables;
    }

    private record MagicTable(long mask, long multiplier, int shift, long[] attacks)
    {
        private int Index(long occupancy)
        {
            return (int) (((occupancy & mask) * multiplier) >>> shift);
        }
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
