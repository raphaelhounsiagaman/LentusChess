package chess.board;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MoveTest
{
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
