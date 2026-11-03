package chess;

import chess.board.Move;
import chess.board.Piece;
import chess.board.Position;
import chess.board.MoveGenerator;
import chess.search.Search;

import java.util.List;
import java.util.function.Consumer;

public class Engine
{
    private Position m_Position = new Position();
    private final MoveGenerator m_MoveGenerator = new MoveGenerator();
    private Search m_Search;
    private Thread m_SearchThread;

    public void NewGame()
    {
        StopSearch();
        m_Position.SetStartingPosition();
    }

    public String GetPositionFen()
    {
        return m_Position.ToFen();
    }

    public void SetPosition(String fen, List<String> moves)
    {
        Position next = new Position();
        next.SetPosition(fen);
        if (Long.bitCount(next.GetBitBoard(Piece.WHITE_KING)) != 1
                || Long.bitCount(next.GetBitBoard(Piece.BLACK_KING)) != 1)
            throw new IllegalArgumentException("Position must contain one king per side");

        Position.UndoState undo = new Position.UndoState();
        for (String text : moves) {
            int move = -1;

            for (int candidate : m_MoveGenerator.GenerateLegalMoves(next)) {
                if (Move.ToUci(candidate).equals(text)) {
                    move = candidate;
                    break;
                }
            }

            if (move == -1) {
                throw new IllegalArgumentException("Illegal move: " + text);
            }

            next.MakeMove(move, undo);
        }

        StopSearch();
        m_Position = next;
    }

    public void StartSearch(int depth, Consumer<Search.Result> onComplete)
    {
        StartSearch(depth, 0, onComplete);
    }

    public void StartSearch(int depth, long timeMillis, Consumer<Search.Result> onComplete)
    {
        if (timeMillis < 0 || 86_400_000 < timeMillis)
            throw new IllegalArgumentException("Invalid search time");
        if (depth < 1 || depth > Search.MAX_DEPTH)
            throw new IllegalArgumentException("Search depth must be between 1 and " + Search.MAX_DEPTH);
        StopSearch();
        Position searchPosition = new Position();
        searchPosition.SetPosition(m_Position.ToFen());
        Search search = new Search();
        m_Search = search;
        m_SearchThread = new Thread(() -> onComplete.accept(search.FindBestMove(searchPosition, depth, timeMillis)), "lentus-search");
        m_SearchThread.start();
    }

    public void StopSearch()
    {
        if (m_SearchThread == null)
            return;
        m_Search.Stop();
        try
        {
            m_SearchThread.join();
        }
        catch (InterruptedException exception)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while stopping search", exception);
        }
        m_SearchThread = null;
        m_Search = null;
    }
}
