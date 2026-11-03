package chess.board;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class BitboardAttacksTest
{
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "0  | b2 c3 d4 e5 f6 g7          | b1 c1 d1 e1 f1 g1 a2 a3 a4 a5 a6 a7",
            "63 | b2 c3 d4 e5 f6 g7          | b8 c8 d8 e8 f8 g8 h2 h3 h4 h5 h6 h7",
            "27 | b2 c3 e5 f6 g7 c5 b6 e3 f2 | b4 c4 e4 f4 g4 d2 d3 d5 d6 d7",
            "24 | b3 c2 b5 c6 d7             | b4 c4 d4 e4 f4 g4 a2 a3 a5 a6 a7",
            "3  | c2 b3 e2 f3 g4             | b1 c1 e1 f1 g1 d2 d3 d4 d5 d6 d7"
    })
    void occupancyMasksOmitOnlyTheOriginAndRayEndpoints(int square, String bishop, String rook)
    {
        assertEquals(Squares(bishop), BitboardAttacks.BishopOccupancyMask(square));
        assertEquals(Squares(rook), BitboardAttacks.RookOccupancyMask(square));
    }

    @ParameterizedTest(name = "bishop = {0}")
    @ValueSource(booleans = {true, false})
    void masksContainExactlyTheSquaresWhoseBlockersCanChangeAttacks(boolean bishop)
    {
        for (int square = 0; square < 64; square++)
        {
            long mask = OccupancyMask(bishop, square);
            long emptyAttacks = Attacks(bishop, square, 0);
            for (int blocker = 0; blocker < 64; blocker++)
            {
                long occupancy = 1L << blocker;
                boolean changesAttacks = Attacks(bishop, square, occupancy) != emptyAttacks;
                assertEquals(changesAttacks, (mask & occupancy) != 0,
                        "Origin " + square + ", blocker " + blocker);
            }
        }
    }

    @ParameterizedTest(name = "bishop = {0}")
    @ValueSource(booleans = {true, false})
    void maskingOccupancyPreservesAttacksForEveryRelevantBlockerSubset(boolean bishop)
    {
        int totalSubsets = 0;
        for (int square = 0; square < 64; square++)
        {
            long mask = OccupancyMask(bishop, square);
            long subset = 0;
            do
            {
                long attacks = Attacks(bishop, square, subset);
                long occupiedOutsideMask = subset | ~mask;
                int origin = square;
                long blockers = subset;
                // Includes the origin, ray endpoints, and all squares off the rays.
                assertEquals(attacks, Attacks(bishop, square, occupiedOutsideMask),
                        () -> "Origin " + origin + ", blockers " + Long.toUnsignedString(blockers, 16));
                totalSubsets++;
                // Carry-rippler enumeration visits every subset once, including zero.
                subset = (subset - mask) & mask;
            }
            while (subset != 0);
        }
        assertEquals(bishop ? 5_248 : 102_400, totalSubsets);
    }

    private static long OccupancyMask(boolean bishop, int square)
    {
        return bishop ? BitboardAttacks.BishopOccupancyMask(square) : BitboardAttacks.RookOccupancyMask(square);
    }

    private static long Attacks(boolean bishop, int square, long occupancy)
    {
        return bishop ? BitboardAttacks.BishopAttacks(square, occupancy) : BitboardAttacks.RookAttacks(square, occupancy);
    }

    private static long Squares(String names)
    {
        long board = 0;
        for (String name : names.split(" "))
            board |= 1L << ((name.charAt(1) - '1') * 8 + name.charAt(0) - 'a');
        return board;
    }
}
