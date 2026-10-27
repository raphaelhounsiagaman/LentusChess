package chess;

import chess.board.Position;
import chess.board.MoveGenerator;

public class Engine
{
    Position position = new Position();
    MoveGenerator moveGenerator = new MoveGenerator();

    public Engine()
    {

    }

    public void NewGame()
    {
        position.SetStartingPosition();
    }



    public void StopSearch()
    {

    }

}
