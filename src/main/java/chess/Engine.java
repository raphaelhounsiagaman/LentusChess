package chess;

import chess.board.Move;
import chess.board.Piece;
import chess.board.Position;
import chess.board.MoveGenerator;

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
    }

    public void StartSearch(int depth, Consumer<Search.Result> onComplete)
    {
    }

    public void StopSearch()
    {

    }

}
