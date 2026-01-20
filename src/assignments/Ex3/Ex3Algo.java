package assignments.Ex3;

import exe.ex3.game.Game;
import exe.ex3.game.GhostCL;
import exe.ex3.game.PacManAlgo;
import exe.ex3.game.PacmanGame;
import java.awt.*;

public class Ex3Algo implements PacManAlgo {
    private int _count;

    public Ex3Algo() {
        _count = 0;
    }

    @Override
    public String getInfo() {
        return null;
    }

    /**
     * This method decides the next move for Pacman.
     * It gets the game board and marks ghosts as walls to avoid them.
     * It finds the current position of Pacman.
     * It looks for the closest food (pink dots) using BFS.
     * It returns the direction of the shortest safe path to the food.
     */
    @Override
    public int move(PacmanGame game) {
        _count++;

        int[][] board = game.getGame(0);
        Map myMap = new Map(board);
        myMap.setCyclic(false);
        int blue = Game.getIntColor(Color.BLUE, 0);
        GhostCL[] ghosts = game.getGhosts(0);
        for (GhostCL g : ghosts) {
            if (g != null) {
                String[] gPos = g.getPos(0).split(",");
                int gx = Integer.parseInt(gPos[0]);
                int gy = Integer.parseInt(gPos[1]);
                myMap.setPixel(gx, gy, blue);
                markGhostNeighbors(myMap, gx, gy, blue);
            }
        }
        String posStr = game.getPos(0).toString();
        String[] parts = posStr.split(",");
        int px = Integer.parseInt(parts[0]);
        int py = Integer.parseInt(parts[1]);
        Pixel2D pacmanPos = new Index2D(px, py);

        Map2D distMap = myMap.allDistance(pacmanPos, blue);
        Pixel2D target = findClosestPink(game, distMap);

        if (target != null) {
            Pixel2D[] path = myMap.shortestPath(pacmanPos, target, blue);
            if (path != null && path.length > 1) {
                return getDirection(path[0], path[1]);
            }
        }

        return randomDir();
    }

    /**
     * This method marks the 4 neighbors of a ghost as walls.
     * It prevents Pacman from moving to a spot right next to a ghost.
     */
    private void markGhostNeighbors(Map myMap, int gx, int gy, int wall) {
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        for (int i = 0; i < 4; i++) {
            int nx = gx + dx[i];
            int ny = gy + dy[i];
            if (nx >= 0 && nx < myMap.getWidth() && ny >= 0 && ny < myMap.getHeight()) {
                myMap.setPixel(nx, ny, wall);
            }
        }
    }

    /**
     * This method finds the nearest food (pink pixel) on the board.
     * It uses the distance map to check which food is the closest to Pacman.
     */
    private Pixel2D findClosestPink(PacmanGame game, Map2D distMap) {
        int[][] board = game.getGame(0);
        int pink = Game.getIntColor(Color.PINK, 0);
        Pixel2D closest = null;
        int minDist = Integer.MAX_VALUE;

        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[0].length; y++) {
                if (board[x][y] == pink) {
                    int d = distMap.getPixel(x, y);
                    if (d > 0 && d < minDist) {
                        minDist = d;
                        closest = new Index2D(x, y);
                    }
                }
            }
        }
        return closest;
    }

    /**
     * This method determines the move direction based on two pixels.
     * It compares the X and Y coordinates of the current and next pixel
     */
    public int getDirection(Pixel2D curr, Pixel2D next) {
        if (next.getX() > curr.getX()) return Game.RIGHT;
        if (next.getX() < curr.getX()) return Game.LEFT;
        if (next.getY() > curr.getY()) return Game.UP;
        if (next.getY() < curr.getY()) return Game.DOWN;
        return Game.ERR;
    }

    /**
     * This method returns a random move direction.
     * It is used when no safe path to food is found.
     */
    private static int randomDir() {
        int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};
        return dirs[(int) (Math.random() * dirs.length)];
    }
}