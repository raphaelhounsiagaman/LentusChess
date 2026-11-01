package chess.board;

import java.util.ArrayList;
import java.util.List;

public class MoveGenerator
{

    public List<Integer> GenerateLegalMoves(Position position)
    {
        int color = position.GetSideToMove();
        List<Integer> candidates = new ArrayList<>();
        GeneratePawnMoves(position, candidates);
        GeneratePieceMoves(position, candidates);
        GenerateCastlingMoves(position, candidates);

        List<Integer> legalMoves = new ArrayList<>(candidates.size());
        Position.UndoState undo = new Position.UndoState();
        for (int move : candidates)
        {
            position.MakeMove(move, undo);
            if (!IsInCheck(position, color))
                legalMoves.add(move);
            position.UnmakeMove(undo);

        }
        return legalMoves;
    }

    public boolean IsInCheck(Position position, int color)
    {
        int kingSquare = Long.numberOfTrailingZeros(position.GetBitBoard(color * 6 + Piece.WHITE_KING));
        return IsSquareAttacked(position, kingSquare, color ^ 1);
    }

    public boolean IsSquareAttacked(Position position, int square, int attackingColor)
    {
        int offset = attackingColor * 6;
        long occupancy = position.GetOccupancy();
        long pawns = position.GetBitBoard(offset + Piece.WHITE_PAWN);
        long queens = position.GetBitBoard(offset + Piece.WHITE_QUEEN);

        return (BitboardAttacks.PawnAttacks(pawns, attackingColor) & (1L << square)) != 0
                || (BitboardAttacks.KnightAttacks(square) & position.GetBitBoard(offset + Piece.WHITE_KNIGHT)) != 0
                || (BitboardAttacks.KingAttacks(square) & position.GetBitBoard(offset + Piece.WHITE_KING)) != 0
                || (BitboardAttacks.BishopAttacks(square, occupancy)
                    & (position.GetBitBoard(offset + Piece.WHITE_BISHOP) | queens)) != 0
                || (BitboardAttacks.RookAttacks(square, occupancy)
                    & (position.GetBitBoard(offset + Piece.WHITE_ROOK) | queens)) != 0;
    }

    private void GeneratePawnMoves(Position position, List<Integer> moves)
    {
        int color = position.GetSideToMove();
        int enemyOffset = (color ^ 1) * 6;
        int step = color == Piece.WHITE ? 8 : -8;
        int startingRank = color == Piece.WHITE ? 1 : 6;
        long occupancy = position.GetOccupancy();
        long enemies = position.GetOccupancy(color ^ 1)
                & ~position.GetBitBoard(enemyOffset + Piece.WHITE_KING);
        long pawns = position.GetBitBoard(color * 6 + Piece.WHITE_PAWN);
        int enPassant = position.GetEnPassantSquare();

        while (pawns != 0)
        {
            int from = Long.numberOfTrailingZeros(pawns);
            pawns &= pawns - 1;
            int to = from + step;
            if (to >= 0 && to < 64 && (occupancy & (1L << to)) == 0)
            {
                AddPawnMove(moves, from, to);
                int doublePush = to + step;
                if (from / 8 == startingRank && (occupancy & (1L << doublePush)) == 0)
                    moves.add(Move.Encode(from, doublePush, Move.NORMAL));
            }

            long attacks = BitboardAttacks.PawnAttacks(1L << from, color);
            long captures = attacks & enemies;
            while (captures != 0)
            {
                int target = Long.numberOfTrailingZeros(captures);
                captures &= captures - 1;
                AddPawnMove(moves, from, target);
            }

            if (enPassant != Position.NO_SQUARE && (attacks & ~occupancy & (1L << enPassant)) != 0
                    && position.GetPiece(enPassant - step) == enemyOffset + Piece.WHITE_PAWN)
                moves.add(Move.Encode(from, enPassant, Move.EN_PASSANT));
        }
    }

    private void AddPawnMove(List<Integer> moves, int from, int to)
    {
        if (to / 8 == 0 || to / 8 == 7)
        {
            for (int type = Move.PROMOTE_KNIGHT; type <= Move.PROMOTE_QUEEN; type++)
                moves.add(Move.Encode(from, to, type));
        }
        else
            moves.add(Move.Encode(from, to, Move.NORMAL));
    }

    private void GeneratePieceMoves(Position position, List<Integer> moves)
    {
        int color = position.GetSideToMove();
        long occupancy = position.GetOccupancy();
        long destinations = ~(position.GetOccupancy(color)
                | position.GetBitBoard((color ^ 1) * 6 + Piece.WHITE_KING));
        for (int type = Piece.WHITE_KNIGHT; type <= Piece.WHITE_KING; type++)
        {
            long pieces = position.GetBitBoard(color * 6 + type);
            while (pieces != 0)
            {
                int from = Long.numberOfTrailingZeros(pieces);
                pieces &= pieces - 1;
                long attacks = switch (type)
                {
                    case Piece.WHITE_KNIGHT -> BitboardAttacks.KnightAttacks(from);
                    case Piece.WHITE_BISHOP -> BitboardAttacks.BishopAttacks(from, occupancy);
                    case Piece.WHITE_ROOK -> BitboardAttacks.RookAttacks(from, occupancy);
                    case Piece.WHITE_QUEEN -> BitboardAttacks.BishopAttacks(from, occupancy)
                            | BitboardAttacks.RookAttacks(from, occupancy);
                    case Piece.WHITE_KING -> BitboardAttacks.KingAttacks(from);
                    default -> throw new IllegalStateException("Unexpected piece type: " + type);
                };
                long targets = attacks & destinations;
                while (targets != 0)
                {
                    int to = Long.numberOfTrailingZeros(targets);
                    targets &= targets - 1;
                    moves.add(Move.Encode(from, to, Move.NORMAL));
                }
            }
        }
    }

    private void GenerateCastlingMoves(Position position, List<Integer> moves)
    {
        int color = position.GetSideToMove();
        int king = color == Piece.WHITE ? 4 : 60;
        if (position.GetPiece(king) != color * 6 + Piece.WHITE_KING || IsInCheck(position, color))
            return;

        int kingside = color == Piece.WHITE ? Position.WHITE_KINGSIDE : Position.BLACK_KINGSIDE;
        int queenside = color == Piece.WHITE ? Position.WHITE_QUEENSIDE : Position.BLACK_QUEENSIDE;
        AddCastlingMove(position, moves, king, king + 2, king + 3, kingside);
        AddCastlingMove(position, moves, king, king - 2, king - 4, queenside);
    }

    private void AddCastlingMove(Position position, List<Integer> moves, int king, int to, int rook, int right)
    {
        int color = position.GetSideToMove();
        if ((position.GetCastlingRights() & right) == 0
                || position.GetPiece(rook) != color * 6 + Piece.WHITE_ROOK)
            return;

        int step = to > king ? 1 : -1;
        long between = 0;
        for (int square = king + step; square != rook; square += step)
            between |= 1L << square;
        if ((position.GetOccupancy() & between) != 0
                || IsSquareAttacked(position, king + step, color ^ 1))
            return;

        moves.add(Move.Encode(king, to, Move.CASTLING));
    }
}
