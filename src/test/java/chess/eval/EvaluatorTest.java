package chess.eval;

import chess.board.Position;
import chess.board.Piece;
import chess.board.Move;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class EvaluatorTest
{
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 | 0",
            "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1 | 105",
            "4k3/8/8/8/8/8/4P3/4K3 b - - 0 1 | -105",
            "4k3/8/8/8/8/8/3n4/4K3 b - - 0 1 | 304",
            "4k3/8/8/8/8/8/3B4/4K3 w - - 0 1 | 324",
            "4k3/8/8/8/8/8/R2q4/4K3 w - - 0 1 | -405",
            "4k3/8/8/8/8/8/QQ6/4K3 w - - 0 1 | 1791",
            "4k3/8/8/8/8/8/8/4K3 w - - 0 1 | 0"
    })
    void scoresMaterialAndPlacementFromTheSideToMove(String fen, int expected)
    {
        Position position = new Position();
        position.SetPosition(fen);
        assertEquals(expected, new Evaluator().Evaluate(position));
        assertEquals(fen, position.ToFen());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            // Symmetric armies cancel. The extra a3 pawn is +105 MG and +110 EG.
            "4k3/8/8/8/8/P7/8/4K3 w - - 0 1 | 110",
            "r2qk3/8/8/8/8/P7/8/R2QK3 w - - 0 1 | 107",
            "rnbqkbnr/8/8/8/8/P7/8/RNBQKBNR w - - 0 1 | 105",
            // Extra promoted queens must not extrapolate past the middlegame endpoint.
            "rnbqkbnr/qq6/8/8/8/P7/QQ6/RNBQKBNR w - - 0 1 | 105",
            // Pawns do not raise the phase; all the mirrored pawn bonuses cancel.
            "4k3/pppppppp/8/8/8/P7/PPPPPPPP/4K3 w - - 0 1 | 110"
    })
    void blendsAtEndgameMidpointAndBoundedMiddlegame(String fen, int expected)
    {
        Position position = new Position();
        position.SetPosition(fen);
        assertEquals(expected, new Evaluator().Evaluate(position));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "4k3/8/8/8/8/8/3N4/4K3 w - - 0 1 | 304",
            "4k3/8/8/8/8/8/3B4/4K3 w - - 0 1 | 324",
            "4k3/8/8/8/8/8/R7/4K3 w - - 0 1 | 499",
            "4k3/8/8/8/8/8/Q7/4K3 w - - 0 1 | 890"
    })
    void usesEachNonPawnPhaseWeight(String fen, int expected)
    {
        Position position = new Position();
        position.SetPosition(fen);
        Evaluator evaluator = new Evaluator();
        assertEquals(expected, evaluator.Evaluate(position));
        // Flip ranks and colors independently of table lookup implementation.
        String[] ranks = fen.split(" ")[0].split("/");
        StringBuilder mirrored = new StringBuilder();
        for (int rank = 7; rank >= 0; rank--)
        {
            for (char symbol : ranks[rank].toCharArray())
                mirrored.append(Character.isUpperCase(symbol)
                        ? Character.toLowerCase(symbol) : Character.toUpperCase(symbol));
            if (rank > 0) mirrored.append('/');
        }
        position.SetPosition(mirrored + " w - - 0 1");
        assertEquals(-expected, evaluator.Evaluate(position));
        position.SetPosition(mirrored + " b - - 0 1");
        assertEquals(expected, evaluator.Evaluate(position));
    }

    @Test
    void includesKingPlacementWithoutAdjudicatingMaterialDraws()
    {
        Position position = new Position();
        position.SetPosition("7k/8/8/8/3K4/8/8/8 w - - 0 1");
        // Endgame king: d4 = +30, h8 = -30. Search, not evaluation, scores the draw.
        assertEquals(60, new Evaluator().Evaluate(position));
    }

    @Test
    void leavesBoardCountersKeyAndExistingHistoryUnchanged()
    {
        Position position = new Position();
        position.MakeMove(Move.Encode(6, 21, Move.NORMAL), new Position.UndoState());
        position.MakeMove(Move.Encode(51, 35, Move.NORMAL), new Position.UndoState());
        Position before = new Position(position);
        Evaluator evaluator = new Evaluator();
        int score = evaluator.Evaluate(position);
        assertEquals(score, evaluator.Evaluate(position));
        assertEquals(before.ToFen(), position.ToFen());
        assertEquals(before.GetPositionKey(), position.GetPositionKey());
        assertArrayEquals(before.GetPositionHistory(), position.GetPositionHistory());
        for (int piece = 0; piece < Piece.COUNT; piece++)
            assertEquals(before.GetBitBoard(piece), position.GetBitBoard(piece));
        assertEquals(before.GetOccupancy(Piece.WHITE), position.GetOccupancy(Piece.WHITE));
        assertEquals(before.GetOccupancy(Piece.BLACK), position.GetOccupancy(Piece.BLACK));
        assertEquals(before.GetOccupancy(), position.GetOccupancy());
    }
}
