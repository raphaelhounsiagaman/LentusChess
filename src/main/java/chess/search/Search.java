package chess.search;

public class Search
{
    public record Result(int bestMove, int score, int depth, long nodes)
    {
    }
}
