package chess.board;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoveGeneratorTest
{
    private final MoveGenerator generator = new MoveGenerator();

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            // Pins, check evasions, and king moves exposing a sliding attack.
            "4r1k1/8/8/8/8/8/4R3/4K3 w - - 7 12 | 12 | 11 | 0 | false",
            "4r1k1/8/8/8/8/8/4R3/4K3 w - - 7 12 | 12 | 60 | 0 | true",
            "4r1k1/8/8/8/8/8/R7/4K3 w - - 0 1 | 8 | 12 | 0 | true",
            "4r1k1/8/8/8/8/8/R7/4K3 w - - 0 1 | 8 | 16 | 0 | false",
            "k7/8/8/8/8/8/8/r3K3 w - - 0 1 | 4 | 5 | 0 | false",
            "8/8/8/8/8/4k3/8/4K3 w - - 0 1 | 4 | 12 | 0 | false",
            // A pinned enemy knight still attacks the king's destination.
            "4k3/4n3/8/8/5K2/8/8/4R3 w - - 0 1 | 29 | 37 | 0 | false",
            // Pawn double pushes cannot jump blockers or start off the home rank.
            "4k3/8/8/8/8/4N3/4P3/4K3 w - - 0 1 | 12 | 28 | 0 | false",
            "4k3/8/8/8/4n3/8/4P3/4K3 w - - 0 1 | 12 | 28 | 0 | false",
            "4k3/8/8/8/8/4P3/8/4K3 w - - 0 1 | 20 | 36 | 0 | false",
            // En passant for both colors, including an exposed rook and a check evasion.
            "k7/8/8/3pP3/8/8/8/4K3 w - d6 0 1 | 36 | 43 | 2 | true",
            "4k3/8/8/8/3pP3/8/8/K7 b - e3 0 1 | 27 | 20 | 2 | true",
            "k7/8/8/r4pPK/8/8/8/8 w - f6 0 1 | 38 | 45 | 2 | false",
            "k7/8/8/3pP3/4K3/8/8/8 w - d6 0 1 | 36 | 43 | 2 | true",
            // Both castling directions and colors.
            "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 7 19 | 4 | 6 | 1 | true",
            "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 7 19 | 4 | 2 | 1 | true",
            "r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 7 19 | 60 | 62 | 1 | true",
            "r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 7 19 | 60 | 58 | 1 | true",
            // No rights, a missing rook, or a blocked path.
            "4k3/8/8/8/8/8/8/R3K2R w - - 0 1 | 4 | 6 | 1 | false",
            "4k3/8/8/8/8/8/8/4K3 w K - 0 1 | 4 | 6 | 1 | false",
            "4k3/8/8/8/8/8/8/RN2K2R w KQ - 0 1 | 4 | 2 | 1 | false",
            // Cannot castle out of, through, or into check (including pawn attacks).
            "k3r3/8/8/8/8/8/8/R3K2R w KQ - 0 1 | 4 | 6 | 1 | false",
            "4kr2/8/8/8/8/8/8/R3K2R w KQ - 0 1 | 4 | 6 | 1 | false",
            "4k1r1/8/8/8/8/8/8/R3K2R w KQ - 0 1 | 4 | 6 | 1 | false",
            "4k3/8/8/8/8/8/6p1/R3K2R w KQ - 0 1 | 4 | 6 | 1 | false",
            // An attacked b1 does not prevent queenside castling.
            "1r2k3/8/8/8/8/8/8/R3K2R w KQ - 0 1 | 4 | 2 | 1 | true"
    })
    void generatesOnlyAllowedMovesAndRestoresPosition(String fen, int from, int to, int type, boolean allowed)
    {
        Position position = new Position();
        position.SetPosition(fen);
        List<Integer> moves = generator.GenerateLegalMoves(position);

        assertEquals(allowed, moves.contains(Move.Encode(from, to, type)));
        assertEquals(moves.size(), new HashSet<>(moves).size(), "Duplicate moves");
        AssertPosition(fen, position);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "r3k3/1P6/8/8/8/8/8/4K3 w - - 0 1 | 49 | 57 | 56",
            "4k3/8/8/8/8/8/1p6/R3K3 b - - 0 1 | 9 | 1 | 0"
    })
    void generatesAllQuietAndCapturePromotions(String fen, int from, int quiet, int capture)
    {
        Position position = new Position();
        position.SetPosition(fen);
        List<Integer> moves = generator.GenerateLegalMoves(position);

        assertEquals(8, moves.stream().filter(move -> Move.GetFrom(move) == from).count());
        for (int type = Move.PROMOTE_KNIGHT; type <= Move.PROMOTE_QUEEN; type++)
        {
            assertTrue(moves.contains(Move.Encode(from, quiet, type)));
            assertTrue(moves.contains(Move.Encode(from, capture, type)));
        }
        AssertPosition(fen, position);
    }

    @Test
    void doubleCheckAllowsOnlyKingMoves()
    {
        Position position = new Position();
        String fen = "4r1k1/8/8/8/1b6/8/R7/4K3 w - - 0 1";
        position.SetPosition(fen);
        List<Integer> moves = generator.GenerateLegalMoves(position);

        assertTrue(generator.IsInCheck(position, Piece.WHITE));
        assertFalse(moves.isEmpty());
        assertTrue(moves.stream().allMatch(move -> Move.GetFrom(move) == 4));
        AssertPosition(fen, position);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "7k/6Q1/5K2/8/8/8/8/8 b - - 0 1 | true",
            "7k/5Q2/5K2/8/8/8/8/8 b - - 0 1 | false"
    })
    void checkmateAndStalemateHaveNoLegalMoves(String fen, boolean inCheck)
    {
        Position position = new Position();
        position.SetPosition(fen);
        assertEquals(inCheck, generator.IsInCheck(position, Piece.BLACK));
        assertTrue(generator.GenerateLegalMoves(position).isEmpty());
        AssertPosition(fen, position);
    }

    @Test
    void attackMasksRespectBoardEdgesAndIncludeOnlyTheFirstBlocker()
    {
        assertEquals((1L << 10) | (1L << 17), BitboardAttacks.KnightAttacks(0));
        assertEquals((1L << 54) | (1L << 55) | (1L << 62), BitboardAttacks.KingAttacks(63));
        assertEquals((1L << 17) | (1L << 22), BitboardAttacks.PawnAttacks((1L << 8) | (1L << 15), Piece.WHITE));
        assertEquals((1L << 41) | (1L << 46), BitboardAttacks.PawnAttacks((1L << 48) | (1L << 55), Piece.BLACK));

        long blockers = (1L << 2) | (1L << 16);
        assertEquals((1L << 1) | (1L << 2) | (1L << 8) | (1L << 16), BitboardAttacks.RookAttacks(0, blockers));
        assertEquals((1L << 9) | (1L << 18), BitboardAttacks.BishopAttacks(0, 1L << 18));
    }

    private static void AssertPosition(String fen, Position actual)
    {
        assertEquals(fen, actual.ToFen());
        Position expected = new Position();
        expected.SetPosition(fen);
        for (int piece = 0; piece < Piece.COUNT; piece++)
            assertEquals(expected.GetBitBoard(piece), actual.GetBitBoard(piece));
        assertEquals(expected.GetOccupancy(Piece.WHITE), actual.GetOccupancy(Piece.WHITE));
        assertEquals(expected.GetOccupancy(Piece.BLACK), actual.GetOccupancy(Piece.BLACK));
        assertEquals(expected.GetOccupancy(), actual.GetOccupancy());
    }
}
