package chess.board;

public class Position
{
    private Bitboards[] position;

    public Position()
    {
        SetPosition("");
    }

    public void SetStartingPosition()
    {
        SetPosition("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }


    public void SetPosition(String fen)
    {

    }






}
