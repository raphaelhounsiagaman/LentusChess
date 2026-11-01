package chess.eval;

import chess.board.Piece;
import chess.board.Position;

public class Evaluator
{
    private static final int[] PIECE_VALUES = {100, 300, 320, 500, 900};

    public int Evaluate(Position position)
    {
        int score = 0;
        for (int type = Piece.WHITE_PAWN; type <= Piece.WHITE_QUEEN; type++)
        {
            int white = Long.bitCount(position.GetBitBoard(type));
            int black = Long.bitCount(position.GetBitBoard(type + 6));
            score += (white - black) * PIECE_VALUES[type];
        }
        return position.GetSideToMove() == Piece.WHITE ? score : -score;
    }
}
