package chess.search;

import chess.board.Move;
import chess.board.Piece;
import chess.board.Position;

import java.util.List;

final class MoveOrdering
{
    private MoveOrdering()
    {
    }

    static void Order(Position position, List<Integer> moves)
    {
        Order(position, moves, Move.NONE);
    }

    static void Order(Position position, List<Integer> moves, int preferredMove)
    {
        int[] scores = new int[moves.size()];
        for (int i = 0; i < moves.size(); i++)
        {
            int move = moves.get(i);
            scores[i] = move == preferredMove ? Integer.MAX_VALUE : Score(position, move);
        }

        // Stable insertion sort preserves generator order for equally ranked moves.
        for (int i = 1; i < moves.size(); i++)
        {
            int move = moves.get(i);
            int score = scores[i];
            int j = i;
            while (j > 0 && scores[j - 1] < score)
            {
                moves.set(j, moves.get(j - 1));
                scores[j] = scores[j - 1];
                j--;
            }
            moves.set(j, move);
            scores[j] = score;
        }
    }

    private static int Score(Position position, int move)
    {
        int type = Move.GetType(move);
        int victim = type == Move.EN_PASSANT ? Piece.WHITE_PAWN : position.GetPiece(Move.GetTo(move));
        int score = 0;
        if (victim != Piece.NONE)
        {
            int attacker = position.GetPiece(Move.GetFrom(move)) % 6;
            score = 100 + 6 * (victim % 6) - attacker;
        }
        if (type >= Move.PROMOTE_KNIGHT && type <= Move.PROMOTE_QUEEN)
            score += 200 + type - Move.PROMOTE_KNIGHT;
        return score;
    }
}
