package chess.uci;

import chess.Engine;
import chess.board.Move;
import chess.board.Position;
import chess.search.Search;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class UCI
{
    private static final int DEFAULT_DEPTH = 4;
    private final Engine m_Engine;
    private final Scanner m_Input;
    private final PrintStream m_Output;

    public UCI(Engine engine)
    {
        m_Engine = engine;
        m_Input = new Scanner(System.in);
        m_Output = System.out;
    }

    public void run()
    {
        try
        {
            while (m_Input.hasNextLine())
            {
                if (HandleCommand(m_Input.nextLine()))
                    break;
            }
        }
        finally
        {
            m_Engine.StopSearch();
        }
    }

    private boolean HandleCommand(String message)
    {
        String[] tokens = message.trim().split("\\s+");
        try
        {
            switch (tokens[0])
            {
                case "uci":
                    Send("id name LentusChess");
                    Send("id author Raphael Hounsiagaman");
                    Send("uciok");
                    break;
                case "isready":
                    Send("readyok");
                    break;
                case "ucinewgame":
                    m_Engine.NewGame();
                    break;
                case "position":
                    SetPosition(tokens);
                    break;
                case "go":
                    int depth = DEFAULT_DEPTH;
                    if (tokens.length != 1)
                    {
                        if (tokens.length != 3 || !tokens[1].equals("depth"))
                            throw new IllegalArgumentException("This version supports go depth <1..64> only");
                        depth = Integer.parseInt(tokens[2]);
                    }
                    m_Engine.StartSearch(depth, this::SendResult);
                    break;
                case "stop":
                    m_Engine.StopSearch();
                    break;
                case "quit":
                    return true;
                default:
                    break;
            }
        }
        catch (IllegalArgumentException exception)
        {
            Send("info string " + exception.getMessage());
        }
        return false;
    }

    private void SetPosition(String[] tokens)
    {
        String fen;
        int index;
        if (tokens.length >= 2 && tokens[1].equals("startpos"))
        {
            fen = Position.STARTING_FEN;
            index = 2;
        }
        else if (tokens.length >= 8 && tokens[1].equals("fen"))
        {
            fen = String.join(" ", Arrays.copyOfRange(tokens, 2, 8));
            index = 8;
        }
        else
            throw new IllegalArgumentException("Expected position startpos or position fen <six FEN fields>");

        List<String> moves = List.of();
        if (index < tokens.length)
        {
            if (!tokens[index].equals("moves"))
                throw new IllegalArgumentException("Expected moves after position");
            moves = Arrays.asList(tokens).subList(index + 1, tokens.length);
        }
        m_Engine.SetPosition(fen, moves);
    }

    private void SendResult(Search.Result result)
    {
        String score = "cp " + result.score();
        if (Math.abs(result.score()) >= Search.MATE_SCORE - Search.MAX_DEPTH)
        {
            int mate = (Search.MATE_SCORE - Math.abs(result.score()) + 1) / 2;
            score = "mate " + (result.score() < 0 ? -mate : mate);
        }
        Send("info depth " + result.depth() + " score " + score + " nodes " + result.nodes());
        Send("bestmove " + Move.ToUci(result.bestMove()));
    }

    private void Send(String message)
    {
        m_Output.println(message);
        m_Output.flush();
    }
}
