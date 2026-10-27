package chess.board;

import java.util.ArrayList;

public class Position
{
    private long[] m_BitBoards;

    public Position()
    {
        SetStartingPosition();
    }

    public void SetStartingPosition()
    {
        SetPosition("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }

    public void SetPosition(String fen)
    {
        if (fen == null || fen.isBlank())
        {
            throw new IllegalArgumentException("FEN cannot be empty");
        }
        String[] fenParts = fen.trim().split("\\s+");
        if (fenParts.length != 6)
        {
            throw new IllegalArgumentException("FEN must have 6 elements");
        }

        int file = 0;
        int rank = 7;

        for (char c : fenParts[0].toCharArray())
        {
            if (c == 'p')
            {

            }


        }








    }






}
