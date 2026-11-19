package chess.search;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MateScoreTest
{
    @ParameterizedTest
    @ValueSource(ints = {1, -1})
    void rebasesMateDistanceWhenProbedAtDifferentPlies(int sign)
    {
        int searchScore = sign * (Search.MATE_SCORE - 12);
        int tableScore = Search.ToTableScore(searchScore, 5);

        assertEquals(sign * (Search.MATE_SCORE - 7), tableScore);
        assertEquals(sign * (Search.MATE_SCORE - 9), Search.FromTableScore(tableScore, 2));
        assertEquals(sign * (Search.MATE_SCORE - 15), Search.FromTableScore(tableScore, 8));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, -1})
    void roundTripsMateScoresAtTheSamePly(int sign)
    {
        int ply = 9;
        int searchScore = sign * (Search.MATE_SCORE - ply);
        int tableScore = Search.ToTableScore(searchScore, ply);

        assertEquals(sign * Search.MATE_SCORE, tableScore);
        assertEquals(searchScore, Search.FromTableScore(tableScore, ply));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, -1})
    void includesMatesAtTheQuiescenceLimit(int sign)
    {
        int boundary = Search.MATE_SCORE - 128;
        int score = sign * boundary;
        int belowBand = sign * (boundary - 1);

        assertEquals(sign * (boundary + 1), Search.ToTableScore(score, 1));
        assertEquals(sign * (boundary - 1), Search.FromTableScore(score, 1));
        assertEquals(belowBand, Search.ToTableScore(belowBand, 1));
        assertEquals(belowBand, Search.FromTableScore(belowBand, 1));
    }

    @ParameterizedTest
    @ValueSource(ints = {-125, 0, 233, -32_000, 32_000})
    void leavesCentipawnScoresAndSearchSentinelsUnchanged(int score)
    {
        assertEquals(score, Search.ToTableScore(score, 5));
        assertEquals(score, Search.FromTableScore(score, 8));
    }
}
