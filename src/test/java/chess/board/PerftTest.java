package chess.board;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Reference FENs and counts: https://chessprogramming.org/Perft_Results (2026-11-01). */
class PerftTest
{
    private final MoveGenerator generator = new MoveGenerator();
    private final Perft perft = new Perft();

    // Indexed by depth, including the perft(0) convention of one leaf.
    // Keep every intervening depth: a matching deeper total can hide shallower errors.
    private static final List<ReferencePosition> POSITIONS = List.of(
            new ReferencePosition("Initial position", Position.STARTING_FEN,
                    new long[]{1, 20, 400, 8_902, 197_281, 4_865_609}),
            // The source omits Kiwipete's counters; append 0 1 for the six-field FEN parser.
            new ReferencePosition("Position 2 (Kiwipete)",
                    "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
                    new long[]{1, 48, 2_039, 97_862, 4_085_603}),
            new ReferencePosition("Position 3 (rook and pawn endgame)",
                    "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
                    new long[]{1, 14, 191, 2_812, 43_238, 674_624, 11_030_083}),
            new ReferencePosition("Position 4",
                    "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
                    new long[]{1, 6, 264, 9_467, 422_333}),
            new ReferencePosition("Position 4 (mirrored, black to move)",
                    "r2q1rk1/pP1p2pp/Q4n2/bbp1p3/Np6/1B3NBn/pPPP1PPP/R3K2R b KQ - 0 1",
                    new long[]{1, 6, 264, 9_467, 422_333}),
            new ReferencePosition("Position 5",
                    "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
                    new long[]{1, 44, 1_486, 62_379, 2_103_487}),
            new ReferencePosition("Position 6",
                    "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10",
                    new long[]{1, 46, 2_079, 89_890, 3_894_594})
    );

    @ParameterizedTest(name = "{0}, depth {2}: {3} nodes")
    @MethodSource("ReferenceCases")
    void nodeCountsMatchReferences(String name, String fen, int depth, long expected)
    {
        Position position = new Position();
        position.SetPosition(fen);
        long actual = perft.Count(position, depth);

        AssertPosition(fen, position);
        // Only compute the root-move breakdown on failure, keeping passing runs inexpensive.
        assertEquals(expected, actual, () -> name + " at depth " + depth + "\nFEN: " + fen
                + "\n" + Divide(position, depth));
    }

    private static Stream<Arguments> ReferenceCases()
    {
        return POSITIONS.stream().flatMap(reference -> IntStream.range(0, reference.nodes().length)
                .mapToObj(depth -> Arguments.of(reference.name(), reference.fen(), depth, reference.nodes()[depth])));
    }

    @ParameterizedTest(name = "{0}, depth {2}: leaf statistics")
    @MethodSource("StatisticsCases")
    void leafStatisticsMatchReferences(String name, String fen, int depth, LeafCounts expected)
    {
        Position position = new Position();
        position.SetPosition(fen);
        LeafCounts actual = new LeafCounts();
        CountLeaves(position, depth, actual);

        AssertPosition(fen, position);
        assertAll(name + " at depth " + depth + "\nFEN: " + fen,
                () -> assertEquals(expected.nodes, actual.nodes, "Nodes"),
                () -> assertEquals(expected.captures, actual.captures, "Captures (including en passant)"),
                () -> assertEquals(expected.enPassant, actual.enPassant, "En passant"),
                () -> assertEquals(expected.castles, actual.castles, "Castles"),
                () -> assertEquals(expected.promotions, actual.promotions, "Promotions"),
                () -> assertEquals(expected.checks, actual.checks, "Checks (including mates)"),
                () -> assertEquals(expected.checkmates, actual.checkmates, "Checkmates"));
    }

    private static Stream<Arguments> StatisticsCases()
    {
        // Source columns: nodes, captures, e.p., castles, promotions, checks, checkmates.
        // These shallower runs complement the deeper node counts without checking mate at every deep leaf.
        return Stream.of(
                StatisticsCase(0, 4, new LeafCounts(197_281, 1_576, 0, 0, 0, 469, 8)),
                StatisticsCase(1, 3, new LeafCounts(97_862, 17_102, 45, 3_162, 0, 993, 1)),
                StatisticsCase(2, 4, new LeafCounts(43_238, 3_348, 123, 0, 0, 1_680, 17)),
                StatisticsCase(3, 3, new LeafCounts(9_467, 1_021, 4, 0, 120, 38, 22)),
                StatisticsCase(4, 3, new LeafCounts(9_467, 1_021, 4, 0, 120, 38, 22))
        );
    }

    private static Arguments StatisticsCase(int index, int depth, LeafCounts counts)
    {
        ReferencePosition reference = POSITIONS.get(index);
        return Arguments.of(reference.name(), reference.fen(), depth, counts);
    }

    @ParameterizedTest
    @CsvSource({
            "7k/6Q1/5K2/8/8/8/8/8 b - - 0 1",
            "7k/5Q2/5K2/8/8/8/8/8 b - - 0 1"
    })
    void terminalPositionsCountOnlyAtDepthZero(String fen)
    {
        Position position = new Position();
        position.SetPosition(fen);
        assertEquals(1, perft.Count(position, 0));
        assertEquals(0, perft.Count(position, 1));
        assertEquals(0, perft.Count(position, 3));
        AssertPosition(fen, position);
    }

    @Test
    void negativeDepthIsRejectedWithoutChangingPosition()
    {
        Position position = new Position();
        assertThrows(IllegalArgumentException.class, () -> perft.Count(position, -1));
        AssertPosition(Position.STARTING_FEN, position);
    }

    // Counts events only on the last ply, as in the source tables.
    private void CountLeaves(Position position, int depth, LeafCounts counts)
    {
        if (depth == 0)
        {
            counts.nodes++;
            return;
        }
        Position.UndoState undo = new Position.UndoState();
        for (int move : generator.GenerateLegalMoves(position))
        {
            int type = Move.GetType(move);
            boolean capture = type == Move.EN_PASSANT || position.GetPiece(Move.GetTo(move)) != Piece.NONE;
            position.MakeMove(move, undo);
            try
            {
                if (depth > 1)
                {
                    CountLeaves(position, depth - 1, counts);
                    continue;
                }

                counts.nodes++;
                if (capture)
                    counts.captures++;
                if (type == Move.EN_PASSANT)
                    counts.enPassant++;
                if (type == Move.CASTLING)
                    counts.castles++;
                if (type >= Move.PROMOTE_KNIGHT && type <= Move.PROMOTE_QUEEN)
                    counts.promotions++;
                if (generator.IsInCheck(position, position.GetSideToMove()))
                {
                    counts.checks++;
                    if (generator.GenerateLegalMoves(position).isEmpty())
                        counts.checkmates++;
                }
            }
            finally
            {
                position.UnmakeMove(undo);
            }
        }
    }

    private String Divide(Position position, int depth)
    {
        if (depth == 0)
            return "Depth zero has one leaf and no root moves.";
        StringBuilder result = new StringBuilder("Root move counts:\n");
        Position.UndoState undo = new Position.UndoState();
        for (int move : generator.GenerateLegalMoves(position))
        {
            long nodes;
            position.MakeMove(move, undo);
            try
            {
                nodes = perft.Count(position, depth - 1);
            }
            finally
            {
                position.UnmakeMove(undo);
            }

            int from = Move.GetFrom(move);
            int to = Move.GetTo(move);
            result.append((char) ('a' + from % 8)).append((char) ('1' + from / 8))
                    .append((char) ('a' + to % 8)).append((char) ('1' + to / 8));
            int type = Move.GetType(move);
            if (type >= Move.PROMOTE_KNIGHT && type <= Move.PROMOTE_QUEEN)
                result.append("nbrq".charAt(type - Move.PROMOTE_KNIGHT));
            result.append(": ").append(nodes).append('\n');
        }
        return result.toString();
    }

    private static void AssertPosition(String fen, Position actual)
    {
        assertEquals(fen, actual.ToFen(), "Perft must restore all FEN state");
        Position expected = new Position();
        expected.SetPosition(fen);
        for (int piece = 0; piece < Piece.COUNT; piece++)
            assertEquals(expected.GetBitBoard(piece), actual.GetBitBoard(piece), "Bitboard " + piece);
        assertEquals(expected.GetOccupancy(Piece.WHITE), actual.GetOccupancy(Piece.WHITE), "White occupancy");
        assertEquals(expected.GetOccupancy(Piece.BLACK), actual.GetOccupancy(Piece.BLACK), "Black occupancy");
        assertEquals(expected.GetOccupancy(), actual.GetOccupancy(), "Combined occupancy");
    }

    private record ReferencePosition(String name, String fen, long[] nodes)
    {
    }

    private static class LeafCounts
    {
        long nodes, captures, enPassant, castles, promotions, checks, checkmates;

        LeafCounts()
        {
        }

        LeafCounts(long nodes, long captures, long enPassant, long castles, long promotions, long checks, long checkmates)
        {
            this.nodes = nodes;
            this.captures = captures;
            this.enPassant = enPassant;
            this.castles = castles;
            this.promotions = promotions;
            this.checks = checks;
            this.checkmates = checkmates;
        }
    }
}
