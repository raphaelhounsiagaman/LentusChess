package chess.uci;

import chess.Engine;

import java.util.Objects;
import java.util.Scanner;

public class UCI
{

    private Engine m_Engine;

    public UCI(Engine engine)
    {
        m_Engine = engine;
    }

    public void run()
    {
        Scanner sc = new Scanner(System.in);
        boolean stop = false;

        while (!stop)
        {
            String msg = sc.nextLine();
            stop = HandleCommand(msg);
        }

        m_Engine.StopSearch();

    }

    private boolean HandleCommand(String msg)
    {

        if (msg.equals("uci"))
        {
            System.out.println("id name LentusChess");
            System.out.println("id author Raphael Hounsiagaman");

            System.out.println("uciok");
        }
        else if (msg.equals("isready"))
        {
            System.out.println("readyok");
        }
        else if (msg.equals("ucinewgame"))
        {
            m_Engine.StopSearch();
            m_Engine.NewGame();
        }


        return false;
    }


}

