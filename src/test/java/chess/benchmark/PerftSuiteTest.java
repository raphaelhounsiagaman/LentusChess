package chess.benchmark;

import chess.board.Perft;
import chess.board.Position;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PerftSuiteTest
{
    private static final String ROW = "startpos\t" + Position.STARTING_FEN + "\t20\t400\t8902\t197281\t4865609";

    @Test
    void bundledSuiteHasStableOrderAndReferenceTotals() throws IOException
    {
        List<PerftSuite.Entry> suite = PerftSuite.Load();
        assertEquals(50, suite.size());
        assertEquals("startpos", suite.getFirst().id());
        assertEquals("promotions-124", suite.getLast().id());
        assertEquals(15_072_328L, suite.stream().mapToLong(entry -> entry.ExpectedNodes(4)).sum());
        assertEquals(574_453_633L, suite.stream().mapToLong(entry -> entry.ExpectedNodes(5)).sum());
        assertThrows(UnsupportedOperationException.class, suite::clear);
        assertThrows(UnsupportedOperationException.class, () -> suite.getFirst().nodes().set(1, 0L));
    }

    @ParameterizedTest(name = "{0}, depth {2}")
    @MethodSource("ReferenceCases")
    void bundledPositionsMatchReferences(String id, String fen, int depth, long expected)
    {
        Position position = new Position();
        position.SetPosition(fen);
        assertEquals(expected, new Perft().Count(position, depth), id + "\nFEN: " + fen);
        assertEquals(fen, position.ToFen());
    }

    private static Stream<Arguments> ReferenceCases() throws IOException
    {
        // Keep routine coverage through depth four so the regular test suite stays inexpensive.
        return PerftSuite.Load().stream().flatMap(entry -> IntStream.rangeClosed(0, 4)
                .mapToObj(depth -> Arguments.of(entry.id(), entry.fen(), depth, entry.ExpectedNodes(depth))));
    }

    @ParameterizedTest
    @MethodSource("InvalidSuites")
    void malformedSuitesAreRejected(String text)
    {
        assertThrows(IOException.class, () -> PerftSuite.Parse(new StringReader(text)));
    }

    private static Stream<String> InvalidSuites()
    {
        return Stream.of("", "# Only a comment\n", "incomplete\t" + Position.STARTING_FEN,
                ROW.replace("startpos", ""), ROW.replace("\t20\t", "\t-1\t"),
                ROW.replace("\t20\t", "\tinvalid\t"), ROW.replace("\t20\t", "\t9223372036854775808\t"),
                ROW.replace(Position.STARTING_FEN, "8/8/8/8/8/8/8/8 w - - 0 1"),
                ROW + "\n" + ROW,
                ROW + "\n" + ROW.replace("startpos", "duplicate-fen"));
    }
}
