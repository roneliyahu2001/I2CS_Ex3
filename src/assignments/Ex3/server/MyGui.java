package assignments.Ex3.server;

import assignments.Ex3.Ex3Algo;
import exe.ex3.game.GhostCL;
import exe.ex3.game.PacManAlgo;
import exe.ex3.game.PacmanGame;
import exe.ex3.game.StdDraw;

import java.awt.*;

/**
 * The main GUI class for the Pacman game.
 * Uses StdDraw to render the game state, including the board, Pacman, and ghosts./
 */
public class MyGui {
    private static final int CODE = 0;
    private static final int DUMMY = 0; // Dummy parameter required by some StdDraw methods

    public static void main(String[] args) throws Exception {
        // Initialize the game
        PacmanGame g = new MyPacmanGame();
        g.init(CODE, "323951038", false, 0, 1.0, 50, 4);

        // Initialize the playing algorithm (Client)
        PacManAlgo algo = new Ex3Algo();

        int[][] board = g.getGame(CODE);
        int w = board.length, h = board[0].length;

        // Setup the drawing canvas
        StdDraw.setCanvasSize(700, 700, DUMMY);
        StdDraw.setXscale(0, w, DUMMY);
        StdDraw.setYscale(0, h, DUMMY);
        StdDraw.enableDoubleBuffering(DUMMY);

        // Main Game Loop
        while (g.getStatus() != PacmanGame.DONE) {
            // Get move from algorithm
            int dir = algo.move(g);
            if (dir != PacmanGame.STAY) {
                g.move(dir);
            }

            board = g.getGame(CODE);

            // Clear screen to black
            StdDraw.clear(Color.BLACK, DUMMY);

            // Draw game elements
            drawBoard(board, w, h);
            drawPac(g.getPos(CODE), h);
            drawGhosts(g.getGhosts(CODE), h);

            // Draw score/status text
            StdDraw.setPenColor(Color.WHITE, DUMMY);
            StdDraw.text(w/2.0, h + 0.5, g.getData(CODE), DUMMY);

            // Display frame and wait
            StdDraw.show(DUMMY);
            StdDraw.pause(100, DUMMY);
        }
        System.out.println("Game Over!");
    }

    /**
     * Draws the static board elements (walls and dots).
     */
    private static void drawBoard(int[][] board, int w, int h) {
        StdDraw.setPenColor(Color.BLUE, DUMMY);

        double th = 0.05; // Wall thickness

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                double px = x + 0.5, py = (h - 1 - y) + 0.5;

                if (board[x][y] == 1) {
                    // Draw connected walls for a clean look
                    if (y > 0 && board[x][y-1] == 1)
                        StdDraw.filledRectangle(px, py + 0.25, th, 0.25);
                    if (y < h-1 && board[x][y+1] == 1)
                        StdDraw.filledRectangle(px, py - 0.25, th, 0.25);
                    if (x > 0 && board[x-1][y] == 1)
                        StdDraw.filledRectangle(px - 0.25, py, 0.25, th);
                    if (x < w-1 && board[x+1][y] == 1)
                        StdDraw.filledRectangle(px + 0.25, py, 0.25, th);

                    StdDraw.filledRectangle(px, py, th, th);
                }
                else if (board[x][y] == 2) {
                    // Draw dots
                    StdDraw.setPenColor(Color.PINK, DUMMY);
                    StdDraw.filledCircle(px, py, 0.1, DUMMY);
                    StdDraw.setPenColor(Color.BLUE, DUMMY);
                } else if (board[x][y] == 3) {
                    // Draw power-ups
                    StdDraw.setPenColor(Color.GREEN, DUMMY);
                    StdDraw.filledCircle(px, py, 0.25, DUMMY);
                    StdDraw.setPenColor(Color.BLUE, DUMMY);
                }
            }
        }
    }

    /**
     * Draws the Pacman character.
     */
    private static void drawPac(String pos, int h) {
        if (pos == null) return;
        int[] xy = parseXY(pos);
        double px = xy[0] + 0.5, py = (h - 1 - xy[1]) + 0.5;
        try {
            // Draw image scaled to fit cell
            StdDraw.picture(px, py, "pacman.png", 0.8, 0.8);
        } catch (Exception e) {
            // Fallback to simple circle
            StdDraw.setPenColor(Color.YELLOW, DUMMY);
            StdDraw.filledCircle(px, py, 0.4, DUMMY);
        }
    }

    /**
     * Draws the ghosts.
     */
    private static void drawGhosts(GhostCL[] ghosts, int h) {
        if (ghosts == null) return;
        for (int i = 0; i < ghosts.length; i++) {
            int[] xy = parseXY(ghosts[i].getPos(CODE));
            double px = xy[0] + 0.5, py = (h - 1 - xy[1]) + 0.5;
            try {
                // Choose image based on ghost state (scared or normal)
                String img = (ghosts[i].getType() == 1) ? "g_blue.png" : "g" + (i % 4) + ".png";
                StdDraw.picture(px, py, img, 0.8, 0.8);
            } catch (Exception e) {
                // Fallback
                StdDraw.setPenColor(Color.RED, DUMMY);
                StdDraw.filledCircle(px, py, 0.35, DUMMY);
            }
        }
    }

    /**
     * Helper to parse coordinate string "x,y".
     */
    private static int[] parseXY(String s) {
        try {
            String[] p = s.split(",");
            return new int[]{(int)Double.parseDouble(p[0]), (int)Double.parseDouble(p[1])};
        } catch (Exception e) { return new int[]{0,0}; }
    }
}