package chess.search;

import chess.board.Move;
import chess.board.MoveGenerator;
import chess.board.Position;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoveOrderingTest
{
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "4k3/8/8/8/8/q6p/8/R3K2R w - - 0 1 | a1a3",
            "4k3/8/8/8/3q4/2P5/8/3RK3 w - - 0 1 | c3d4",
            "3rk3/8/2p5/3Q4/8/8/8/4K3 b - - 0 1 | c6d5",
            "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1 | e5d6",
            "4k3/8/8/8/3Pp3/8/8/4K3 b - d3 0 1 | e4d3",
            "4k3/1P6/8/8/8/8/8/4K3 w - - 0 1 | b7b8q",
            "r3k3/1P6/8/8/8/8/8/4K3 w - - 0 1 | b7a8q"
    })
    void prioritizesTacticalMovesWithoutChangingThePositionOrMoveSet(String fen, String first)
    {
        Position position = new Position();
        position.SetPosition(fen);
        List<Integer> moves = new MoveGenerator().GenerateLegalMoves(position);
        List<Integer> original = new ArrayList<>(moves);

        MoveOrdering.Order(position, moves);

        assertEquals(first, Move.ToUci(moves.getFirst()));
        assertEquals(original.size(), moves.size());
        assertTrue(moves.containsAll(original));
        assertEquals(fen, position.ToFen());
    }

    @Test
    void prioritizesALegalHintAndPreservesTheOrderOfOtherMoves()
    {
        Position position = new Position();
        position.SetPosition("4k3/8/8/8/8/q6p/8/R3K2R w - - 0 1");
        List<Integer> moves = new MoveGenerator().GenerateLegalMoves(position);
        List<Integer> expected = new ArrayList<>(moves);
        MoveOrdering.Order(position, expected);

        int quietMove = Move.Encode(4, 12, Move.NORMAL);
        assertTrue(expected.remove(Integer.valueOf(quietMove)));
        expected.addFirst(quietMove);

        MoveOrdering.Order(position, moves, quietMove);
        assertEquals(expected, moves);
    }

    @Test
    void ignoresAnIllegalHint()
    {
        Position position = new Position();
        List<Integer> moves = new MoveGenerator().GenerateLegalMoves(position);
        List<Integer> expected = new ArrayList<>(moves);
        MoveOrdering.Order(position, expected);

        MoveOrdering.Order(position, moves, Move.Encode(4, 12, Move.NORMAL));
        assertEquals(expected, moves);
    }

    @Test
    void preservesOrderAmongQuietMoves()
    {
        Position position = new Position();
        List<Integer> moves = new MoveGenerator().GenerateLegalMoves(position);
        List<Integer> original = new ArrayList<>(moves);
        MoveOrdering.Order(position, moves);
        assertEquals(original, moves);
    }
}
