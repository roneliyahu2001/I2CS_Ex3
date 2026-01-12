package assignments.Ex3.server;

import exe.ex3.game.PacmanGame;
import exe.ex3.game.StdDraw;

public class MyGui {

    private static final int BLACK  = 0;
    private static final int WHITE  = 1;
    private static final int YELLOW = 2;

    private static final int MODE = 1;

    public static void main(String[] args) throws Exception {
        PacmanGame g = new MyPacmanGame();
        g.init(0, "323951038", false, 31, 1.0, 50, 4);

        int[][] board = g.getGame(0);
        int w = board.length, h = board[0].length;

        StdDraw.setCanvasSize(700, 700, MODE);

        StdDraw.setXscale(0.0, (double) w, MODE);
        StdDraw.setYscale(0.0, (double) h, MODE);

        StdDraw.enableDoubleBuffering(MODE);

        while (g.getStatus() != PacmanGame.DONE) {

            board = g.getGame(0);

            int dir = PacmanGame.STAY;

            if (StdDraw.isKeyPressed(37)) dir = PacmanGame.LEFT;   // ←
            else if (StdDraw.isKeyPressed(39)) dir = PacmanGame.RIGHT;  // →
            else if (StdDraw.isKeyPressed(38)) dir = PacmanGame.UP;     // ↑
            else if (StdDraw.isKeyPressed(40)) dir = PacmanGame.DOWN;   // ↓

            if (dir != PacmanGame.STAY) {
                g.move(dir);
            }

            drawBoard(board);
            drawPac(g.getPos(0), h);

            StdDraw.show(MODE);

            StdDraw.pause(50, MODE);
        }
    }

    private static void drawBoard(int[][] board) {
        int w = board.length, h = board[0].length;

        StdDraw.clear(WHITE);

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int color = (board[x][y] == 1) ? BLACK : WHITE;

                StdDraw.setPenColor(color);

                StdDraw.filledSquare(
                        x + 0.5,
                        (h - 1 - y) + 0.5,
                        0.5,
                        color
                );
            }
        }
    }

    private static void drawPac(String pos, int h) {
        int[] xy = parseXY(pos);
        int x = xy[0], y = xy[1];

        StdDraw.setPenColor(YELLOW);

        StdDraw.filledCircle(
                x + 0.5,
                (h - 1 - y) + 0.5,
                0.35,
                YELLOW
        );
    }

    private static int[] parseXY(String s) {
        String[] parts = s.split(",");
        int x = Integer.parseInt(parts[0].trim());
        int y = Integer.parseInt(parts[1].trim());
        return new int[]{x, y};
    }
}