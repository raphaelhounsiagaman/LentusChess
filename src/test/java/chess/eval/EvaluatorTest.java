package chess.eval;

import chess.board.Position;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EvaluatorTest
{
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 | 0",
            "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1 | 100",
            "4k3/8/8/8/8/8/4P3/4K3 b - - 0 1 | -100",
            "4k3/8/8/8/8/8/3n4/4K3 b - - 0 1 | 320",
            "4k3/8/8/8/8/8/3B4/4K3 w - - 0 1 | 330",
            "4k3/8/8/8/8/8/R2q4/4K3 w - - 0 1 | -400",
            "4k3/8/8/8/8/8/QQ6/4K3 w - - 0 1 | 1800",
            "4k3/8/8/8/8/8/8/4K3 w - - 0 1 | 0"
    })
    void countsMaterialFromTheSideToMove(String fen, int expected)
    {
        Position position = new Position();
        position.SetPosition(fen);
        assertEquals(expected, new Evaluator().Evaluate(position));
        assertEquals(fen, position.ToFen());
    }
}
