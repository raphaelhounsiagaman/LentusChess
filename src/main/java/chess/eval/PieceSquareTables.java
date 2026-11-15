package chess.eval;

import chess.board.Piece;

/**
 * Positional bonuses in centipawns, separate from material values.
 * These are hand-written starting values, not tuned values: pawns favor advance,
 * minor pieces and queens favor central squares, and rooks favor the seventh rank.
 * Middlegame kings favor shelter on the home rank; endgame kings favor the center.
 * Endgame pawn advancement is rewarded more strongly.
 *
 * Tables use White's perspective and the board's a1 = 0, h1 = 7, a8 = 56,
 * h8 = 63 indexing. Each row below runs a-file to h-file, from rank 1 to rank 8
 * (the opposite row order from a printed chess diagram). Black mirrors ranks
 * only: square ^ 56 maps a8 to a1 and preserves the file.
 *
 * Lookups return a bonus for the piece's own color, without applying a sign.
 * The eventual evaluator must subtract Black's bonuses from White's and then
 * convert to the side-to-move perspective. No position is read or modified here.
 */
final class PieceSquareTables
{
    // Piece order matches Piece's White indices: pawn, knight, bishop, rook, queen, king.
    private static final int[][] MIDDLEGAME = {
            { // Pawn: encourage central occupation and advancement.
                  0,   0,   0,   0,   0,   0,   0,   0,
                  0,   0,   0,  -5,  -5,   0,   0,   0,
                  5,   5,  10,  15,  15,  10,   5,   5,
                 10,  10,  15,  25,  25,  15,  10,  10,
                 15,  15,  20,  30,  30,  20,  15,  15,
                 20,  20,  25,  35,  35,  25,  20,  20,
                 30,  30,  35,  40,  40,  35,  30,  30,
                  0,   0,   0,   0,   0,   0,   0,   0
            },
            { // Knight: avoid the rim; prefer the center.
                -30, -20, -15, -15, -15, -15, -20, -30,
                -20, -10,   0,   0,   0,   0, -10, -20,
                -15,   0,  10,  15,  15,  10,   0, -15,
                -15,   5,  15,  25,  25,  15,   5, -15,
                -15,   5,  15,  25,  25,  15,   5, -15,
                -15,   0,  10,  15,  15,  10,   0, -15,
                -20, -10,   0,   0,   0,   0, -10, -20,
                -30, -20, -15, -15, -15, -15, -20, -30
            },
            { // Bishop: modest centralization and development bonuses.
                -10,  -5,  -5,  -5,  -5,  -5,  -5, -10,
                 -5,   5,   0,   0,   0,   0,   5,  -5,
                 -5,   5,  10,  10,  10,  10,   5,  -5,
                 -5,   5,  10,  15,  15,  10,   5,  -5,
                 -5,   5,  10,  15,  15,  10,   5,  -5,
                 -5,   5,  10,  10,  10,  10,   5,  -5,
                 -5,   5,   0,   0,   0,   0,   5,  -5,
                -10,  -5,  -5,  -5,  -5,  -5,  -5, -10
            },
            { // Rook: central home files and the seventh rank.
                  0,   0,   0,   5,   5,   0,   0,   0,
                 -5,   0,   0,   0,   0,   0,   0,  -5,
                 -5,   0,   0,   0,   0,   0,   0,  -5,
                 -5,   0,   0,   0,   0,   0,   0,  -5,
                 -5,   0,   0,   0,   0,   0,   0,  -5,
                 -5,   0,   0,   0,   0,   0,   0,  -5,
                 15,  20,  20,  20,  20,  20,  20,  15,
                  0,   0,   0,   5,   5,   0,   0,   0
            },
            { // Queen: small central bonuses to avoid overvaluing early activity.
                -10,  -5,  -5,   0,   0,  -5,  -5, -10,
                 -5,   0,   0,   0,   0,   0,   0,  -5,
                 -5,   0,   5,   5,   5,   5,   0,  -5,
                  0,   0,   5,  10,  10,   5,   0,   0,
                  0,   0,   5,  10,  10,   5,   0,   0,
                 -5,   0,   5,   5,   5,   5,   0,  -5,
                 -5,   0,   0,   0,   0,   0,   0,  -5,
                -10,  -5,  -5,   0,   0,  -5,  -5, -10
            },
            { // King: favor castled home squares, especially g1, over central exposure.
                 10,  10,  20, -10, -20,   0,  30,  10,
                  5,   5,   0, -15, -15,   0,   5,   5,
                -10, -10, -20, -30, -30, -20, -10, -10,
                -20, -20, -30, -40, -40, -30, -20, -20,
                -30, -30, -40, -50, -50, -40, -30, -30,
                -30, -30, -40, -50, -50, -40, -30, -30,
                -30, -30, -40, -50, -50, -40, -30, -30,
                -30, -30, -40, -50, -50, -40, -30, -30
            }
    };

    private static final int[][] ENDGAME = {
            { // Pawn: advance toward promotion; terminal ranks are unused, set to zero.
                  0,   0,   0,   0,   0,   0,   0,   0,
                  0,   0,   0,   5,   5,   0,   0,   0,
                 10,  10,  10,  15,  15,  10,  10,  10,
                 20,  20,  20,  25,  25,  20,  20,  20,
                 35,  35,  35,  40,  40,  35,  35,  35,
                 55,  55,  55,  60,  60,  55,  55,  55,
                 80,  80,  80,  85,  85,  80,  80,  80,
                  0,   0,   0,   0,   0,   0,   0,   0
            },
            { // Knight: centralization remains useful with fewer pieces.
                -25, -15, -10, -10, -10, -10, -15, -25,
                -15,  -5,   0,   5,   5,   0,  -5, -15,
                -10,   0,  10,  15,  15,  10,   0, -10,
                -10,   5,  15,  20,  20,  15,   5, -10,
                -10,   5,  15,  20,  20,  15,   5, -10,
                -10,   0,  10,  15,  15,  10,   0, -10,
                -15,  -5,   0,   5,   5,   0,  -5, -15,
                -25, -15, -10, -10, -10, -10, -15, -25
            },
            { // Bishop: favor central activity.
                -10,  -5,  -5,  -5,  -5,  -5,  -5, -10,
                 -5,   0,   5,   5,   5,   5,   0,  -5,
                 -5,   5,  10,  15,  15,  10,   5,  -5,
                 -5,   5,  15,  20,  20,  15,   5,  -5,
                 -5,   5,  15,  20,  20,  15,   5,  -5,
                 -5,   5,  10,  15,  15,  10,   5,  -5,
                 -5,   0,   5,   5,   5,   5,   0,  -5,
                -10,  -5,  -5,  -5,  -5,  -5,  -5, -10
            },
            { // Rook: mild central-file bonus, stronger on the seventh rank.
                  0,   0,   5,   5,   5,   5,   0,   0,
                  0,   0,   5,   5,   5,   5,   0,   0,
                  0,   0,   5,   5,   5,   5,   0,   0,
                  0,   0,   5,   5,   5,   5,   0,   0,
                  0,   0,   5,   5,   5,   5,   0,   0,
                  0,   0,   5,   5,   5,   5,   0,   0,
                 20,  20,  25,  25,  25,  25,  20,  20,
                  5,   5,  10,  10,  10,  10,   5,   5
            },
            { // Queen: more central activity in the endgame.
                -15, -10,  -5,  -5,  -5,  -5, -10, -15,
                -10,   0,   5,   5,   5,   5,   0, -10,
                 -5,   5,  10,  15,  15,  10,   5,  -5,
                 -5,   5,  15,  20,  20,  15,   5,  -5,
                 -5,   5,  15,  20,  20,  15,   5,  -5,
                 -5,   5,  10,  15,  15,  10,   5,  -5,
                -10,   0,   5,   5,   5,   5,   0, -10,
                -15, -10,  -5,  -5,  -5,  -5, -10, -15
            },
            { // King: leave the edge and participate in the center.
                -30, -20, -10, -10, -10, -10, -20, -30,
                -20, -10,   0,   0,   0,   0, -10, -20,
                -10,   0,  10,  20,  20,  10,   0, -10,
                -10,   0,  20,  30,  30,  20,   0, -10,
                -10,   0,  20,  30,  30,  20,   0, -10,
                -10,   0,  10,  20,  20,  10,   0, -10,
                -20, -10,   0,   0,   0,   0, -10, -20,
                -30, -20, -10, -10, -10, -10, -20, -30
            }
    };

    private PieceSquareTables()
    {
    }

    static int GetMiddlegame(int piece, int square)
    {
        return Lookup(MIDDLEGAME, piece, square);
    }

    static int GetEndgame(int piece, int square)
    {
        return Lookup(ENDGAME, piece, square);
    }

    private static int Lookup(int[][] tables, int piece, int square)
    {
        if (piece < 0 || piece >= Piece.COUNT)
            throw new IllegalArgumentException("Invalid piece index: " + piece);
        if (square < 0 || square >= 64)
            throw new IllegalArgumentException("Invalid square: " + square);
        int relativeSquare = piece < Piece.BLACK_PAWN ? square : square ^ 56;
        return tables[piece % 6][relativeSquare];
    }
}
