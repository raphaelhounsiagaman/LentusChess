package chess.eval;

import chess.board.Piece;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PieceSquareTablesTest
{
    @Test
    void mirrorsRanksForEveryPieceAndSquareInBothPhases()
    {
        for (int piece = Piece.WHITE_PAWN; piece <= Piece.WHITE_KING; piece++)
        {
            for (int rank = 0; rank < 8; rank++)
            {
                for (int file = 0; file < 8; file++)
                {
                    int whiteSquare = rank * 8 + file;
                    int blackSquare = (7 - rank) * 8 + file;
                    String context = "piece=" + piece + ", square=" + whiteSquare;
                    assertEquals(PieceSquareTables.GetMiddlegame(piece, whiteSquare),
                            PieceSquareTables.GetMiddlegame(piece + 6, blackSquare), context);
                    assertEquals(PieceSquareTables.GetEndgame(piece, whiteSquare),
                            PieceSquareTables.GetEndgame(piece + 6, blackSquare), context);
                }
            }
        }
    }

    @ParameterizedTest
    @CsvSource({
            "P, a2, 0, 0",
            "P, d4, 25, 25",
            "P, a7, 30, 80",
            "p, a2, 30, 80",
            "p, a7, 0, 0",
            "N, a1, -30, -25",
            "N, d4, 25, 20",
            "B, d4, 15, 20",
            "R, d7, 20, 25",
            "Q, d4, 10, 20",
            "K, d4, -40, 30",
            "K, g1, 30, -20",
            "K, b1, 10, -20",
            // Asymmetric home-rank bonuses distinguish a rank flip from a rotation.
            "k, g8, 30, -20",
            "k, b8, 10, -20",
            "k, g1, -30, -20",
            "K, h8, -30, -30",
            "k, h1, -30, -30"
    })
    void returnsDocumentedBonusesWithoutMaterialOrColorSign(char symbol, String square,
                                                           int middlegame, int endgame)
    {
        int index = (square.charAt(1) - '1') * 8 + square.charAt(0) - 'a';
        int piece = Piece.FromFenChar(symbol);
        assertEquals(middlegame, PieceSquareTables.GetMiddlegame(piece, index));
        assertEquals(endgame, PieceSquareTables.GetEndgame(piece, index));
    }

    @ParameterizedTest
    @CsvSource({"-1, 0", "12, 0", "0, -1", "0, 64", "6, -1", "6, 64"})
    void rejectsInvalidPieceOrSquare(int piece, int square)
    {
        assertThrows(IllegalArgumentException.class,
                () -> PieceSquareTables.GetMiddlegame(piece, square));
        assertThrows(IllegalArgumentException.class,
                () -> PieceSquareTables.GetEndgame(piece, square));
    }
}
