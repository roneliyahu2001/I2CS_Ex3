package assignments.Ex3.server;

import exe.ex3.game.PacmanGame;

public class ServerMain {
    public static void main(String[] args) {
        PacmanGame g = new MyPacmanGame();

        System.out.println("init: " + g.init(0, "323951038", false, 31, 1.0, 50, 4));
        System.out.println("pos: " + g.getPos(0));

        int[] moves = {PacmanGame.RIGHT, PacmanGame.RIGHT, PacmanGame.DOWN, PacmanGame.DOWN,
                PacmanGame.LEFT, PacmanGame.UP};

        for (int m : moves) {
            String r = g.move(m);
            System.out.println("move=" + m + "  result=" + r + "  pos=" + g.getPos(0));
        }
    }
}