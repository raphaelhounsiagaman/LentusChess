package chess;

import chess.uci.UCI;

public class Main {

    public static void main(String[] args) {
        Engine engine = new Engine();

        UCI uci = new UCI(engine);
        uci.run();
    }

}
