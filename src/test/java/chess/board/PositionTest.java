package chess.board;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PositionTest
{
    @ParameterizedTest
    @CsvSource({
            "-, 0", "K, 1", "Q, 2", "k, 4", "q, 8",
            "KQ, 3", "Kk, 5", "Kq, 9", "Qk, 6", "Qq, 10", "kq, 12",
            "KQk, 7", "KQq, 11", "Kkq, 13", "Qkq, 14", "KQkq, 15"
    })
    void castlingRightsRemainIndependentWhenLoadingFen(String rights, int expected)
    {
        Position position = new Position();
        String fen = "r3k2r/8/8/8/8/8/8/R3K2R w " + rights + " - 0 1";

        position.SetPosition(fen);

        assertEquals(expected, position.GetCastlingRights());
        assertEquals(fen, position.ToFen());
    }
}
