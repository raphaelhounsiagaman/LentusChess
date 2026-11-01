package chess.board;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class MoveTest
{
    @ParameterizedTest
    @CsvSource({
            "12, 28, 0, e2e4", "60, 58, 1, e8c8", "36, 43, 2, e5d6",
            "49, 57, 3, b7b8n", "49, 57, 4, b7b8b", "49, 57, 5, b7b8r", "49, 57, 6, b7b8q"
    })
    void formatsUciMoves(int from, int to, int type, String expected)
    {
        assertEquals(expected, Move.ToUci(Move.Encode(from, to, type)));
    }

    @Test
    void formatsNoMoveAsUciNullMove()
    {
        assertEquals("0000", Move.ToUci(Move.NONE));
    }

    @Test
    void everySquarePairAndMoveTypeHasADistinctCompactEncoding()
    {
        int[] types = {Move.NORMAL, Move.CASTLING, Move.EN_PASSANT,
                Move.PROMOTE_KNIGHT, Move.PROMOTE_BISHOP, Move.PROMOTE_ROOK, Move.PROMOTE_QUEEN};
        boolean[] seen = new boolean[1 << 16];
        seen[Move.NONE] = true;

        for (int type : types)
        {
            for (int from = 0; from < 64; from++)
            {
                for (int to = 0; to < 64; to++)
                {
                    if (from == to)
                        continue;

                    int move = Move.Encode(from, to, type);

                    assertTrue(move >= 0 && move < seen.length, "Move must fit in 16 bits");
                    assertFalse(seen[move], "Move encoding collided with another move or NONE");
                    seen[move] = true;
                    assertEquals(from, Move.GetFrom(move));
                    assertEquals(to, Move.GetTo(move));
                    assertEquals(type, Move.GetType(move));
                }
            }
        }
    }

}
