package chess.benchmark;

import chess.board.Piece;
import chess.board.Position;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class PerftSuite
{
    public static final String VERSION = "perft-v1";
    public static final int MAX_DEPTH = 5;

    private PerftSuite()
    {
    }

    public static List<Entry> Load() throws IOException
    {
        var resource = PerftSuite.class.getResourceAsStream("/chess/benchmark/" + VERSION + ".tsv");
        if (resource == null)
            throw new IOException("Missing benchmark suite: " + VERSION);
        try (var reader = new InputStreamReader(resource, StandardCharsets.UTF_8))
        {
            List<Entry> entries = Parse(reader);
            if (entries.size() != 50)
                throw new IOException(VERSION + " must contain exactly 50 positions");
            return entries;
        }
    }

    static List<Entry> Parse(Reader input) throws IOException
    {
        var reader = new BufferedReader(input);
        List<Entry> entries = new ArrayList<>();
        var ids = new HashSet<String>();
        var fens = new HashSet<String>();
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null)
        {
            lineNumber++;
            if (line.isBlank() || line.startsWith("#"))
                continue;
            try
            {
                String[] fields = line.split("\t", -1);
                if (fields.length != 2 + MAX_DEPTH)
                    throw new IllegalArgumentException("Expected ID, FEN and five reference counts");
                if (fields[0].isBlank() || !ids.add(fields[0]))
                    throw new IllegalArgumentException("Empty or duplicate position ID");
                if (!fens.add(fields[1]))
                    throw new IllegalArgumentException("Duplicate FEN");
                Position position = new Position();
                position.SetPosition(fields[1]);
                if (Long.bitCount(position.GetBitBoard(Piece.WHITE_KING)) != 1
                        || Long.bitCount(position.GetBitBoard(Piece.BLACK_KING)) != 1)
                    throw new IllegalArgumentException("Position must contain one king per side");

                List<Long> nodes = new ArrayList<>();
                nodes.add(1L); // perft(0), including terminal positions.
                for (int i = 2; i < fields.length; i++)
                {
                    long count = Long.parseLong(fields[i]);
                    if (count < 0)
                        throw new IllegalArgumentException("Reference counts must be non-negative");
                    nodes.add(count);
                }
                entries.add(new Entry(fields[0], fields[1], nodes));
            }
            catch (IllegalArgumentException exception)
            {
                throw new IOException("Invalid " + VERSION + " line " + lineNumber + ": "
                        + exception.getMessage(), exception);
            }
        }
        if (entries.isEmpty())
            throw new IOException("Benchmark suite is empty");
        return List.copyOf(entries);
    }

    public record Entry(String id, String fen, List<Long> nodes)
    {
        public Entry
        {
            nodes = List.copyOf(nodes);
        }

        public long ExpectedNodes(int depth)
        {
            return nodes.get(depth);
        }
    }
}
