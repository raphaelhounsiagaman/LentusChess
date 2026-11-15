package chess.eval;

import chess.board.Piece;
import chess.board.Position;

public class Evaluator
{
    private static final int[] PIECE_VALUES = {100, 300, 320, 500, 900, 0};
    // Remaining knights/bishops count 1, rooks 2, queens 4; pawns/kings count 0.
    // Both starting armies total 24. Promotions may exceed that, so clamp the phase.
    private static final int[] PHASE_WEIGHTS = {0, 1, 1, 2, 4, 0};
    private static final int MAX_PHASE = 24;

    /** Static centipawn score; draw adjudication belongs to search. Does not mutate position. */
    public int Evaluate(Position position)
    {
        int middlegame = 0;
        int endgame = 0;
        int phase = 0;
        for (int piece = 0; piece < Piece.COUNT; piece++)
        {
            int type = piece % 6;
            int sign = piece < Piece.BLACK_PAWN ? 1 : -1;
            long squares = position.GetBitBoard(piece);
            phase += Long.bitCount(squares) * PHASE_WEIGHTS[type];
            while (squares != 0)
            {
                int square = Long.numberOfTrailingZeros(squares);
                middlegame += sign * (PIECE_VALUES[type] + PieceSquareTables.GetMiddlegame(piece, square));
                endgame += sign * (PIECE_VALUES[type] + PieceSquareTables.GetEndgame(piece, square));
                squares &= squares - 1;
            }
        }
        phase = Math.min(MAX_PHASE, phase);
        // Divide once after summing both colors; Java truncates toward zero symmetrically.
        int score = (middlegame * phase + endgame * (MAX_PHASE - phase)) / MAX_PHASE;
        return position.GetSideToMove() == Piece.WHITE ? score : -score;
    }
}
