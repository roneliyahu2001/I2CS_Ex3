package assignments.Ex3;

import exe.ex3.game.GhostCL;
import exe.ex3.game.PacManAlgo;
import exe.ex3.game.PacmanGame;
import java.awt.Color;
import java.util.LinkedList;
import java.util.Queue;

/**
 * This class implements the logic for the Pacman client (Ex3Algo).
 * The algorithm uses a hybrid approach:
 * 1. Survival Mode: If a ghost is detected within a close radius, the Pacman calculates the best escape route.
 * 2. Chase Mode: If no immediate danger exists, the Pacman uses BFS (Breadth-First Search) to find the shortest path to the nearest dot.
 */
public class Ex3Algo implements PacManAlgo {

    /**
     * Returns the identifier of the algorithm.
     * @return String representing the algorithm name.
     */
    @Override
    public String getInfo() {
        return "Smart BFS Algo";
    }

    /**
     * The main method called by the game engine at every step.
     * It determines the next move for the Pacman based on the current board state.
     *
     * @param game The game interface providing access to the board, ghosts, and player status.
     * @return The direction code (UP, DOWN, LEFT, RIGHT) for the next move.
     */
    @Override
    public int move(PacmanGame game) {
        int[][] board = game.getGame(0);
        String pos = game.getPos(0);
        int[] myPos = parseXY(pos);
        GhostCL[] ghosts = game.getGhosts(0);

        // Step 1: Safety check - Is there a ghost nearby?
        // If a ghost is dangerously close, we prioritize survival over points.
        int dangerousGhostDir = checkGhostDanger(board, myPos, ghosts);
        if (dangerousGhostDir != -1) {
            return dangerousGhostDir; // Execute escape maneuver
        }

        // Step 2: If safe, use BFS to find the nearest food.
        return bfs(board, myPos[0], myPos[1]);
    }

    /**
     * Checks if any ghost is within a critical radius (danger zone).
     * If a threat is detected, calculates the move that maximizes the distance from the ghost.
     *
     * @param board The game board.
     * @param myPos The current position of the Pacman.
     * @param ghosts The array of ghost objects.
     * @return The best direction to escape, or -1 if no immediate danger is detected.
     */
    private int checkGhostDanger(int[][] board, int[] myPos, GhostCL[] ghosts) {
        int bestEscapeDir = -1;
        double maxDist = -1;

        for (GhostCL g : ghosts) {
            int[] gPos = parseXY(g.getPos(0));
            double dist = Math.sqrt(Math.pow(myPos[0] - gPos[0], 2) + Math.pow(myPos[1] - gPos[1], 2));

            // Check if the ghost is close (distance < 3) and dangerous (type != 1 means not eatable)
            if (dist < 3 && g.getType() != 1) {

                int[] dirs = {PacmanGame.UP, PacmanGame.DOWN, PacmanGame.LEFT, PacmanGame.RIGHT};

                // Evaluate all possible moves to find the one that maximizes distance from the ghost
                for (int dir : dirs) {
                    int nextX = myPos[0];
                    int nextY = myPos[1];
                    if (dir == PacmanGame.UP) nextY--;
                    if (dir == PacmanGame.DOWN) nextY++;
                    if (dir == PacmanGame.LEFT) nextX--;
                    if (dir == PacmanGame.RIGHT) nextX++;

                    if (isValid(board, nextX, nextY)) {
                        double newDist = Math.sqrt(Math.pow(nextX - gPos[0], 2) + Math.pow(nextY - gPos[1], 2));
                        if (newDist > maxDist) {
                            maxDist = newDist;
                            bestEscapeDir = dir;
                        }
                    }
                }
                return bestEscapeDir; // Return the safest direction
            }
        }
        return -1; // No danger detected
    }

    /**
     * Performs a Breadth-First Search (BFS) to find the shortest path to the nearest target.
     * Targets include regular dots (2) and power-ups (3).
     *
     * @param board The game board.
     * @param startX The starting X coordinate.
     * @param startY The starting Y coordinate.
     * @return The direction of the first step towards the target, or a random move if no target is reachable.
     */
    private static int bfs(int[][] board, int startX, int startY) {
        int w = board.length;
        int h = board[0].length;
        boolean[][] visited = new boolean[w][h];
        Queue<int[]> queue = new LinkedList<>();

        int[] dirs = {PacmanGame.UP, PacmanGame.DOWN, PacmanGame.LEFT, PacmanGame.RIGHT};

        // Initialize queue with immediate neighbors
        for (int dir : dirs) {
            int nextX = startX;
            int nextY = startY;

            if (dir == PacmanGame.UP) nextY--;
            if (dir == PacmanGame.DOWN) nextY++;
            if (dir == PacmanGame.LEFT) nextX--;
            if (dir == PacmanGame.RIGHT) nextX++;

            if (isValid(board, nextX, nextY)) {
                // If immediate neighbor is a target, move there
                if (board[nextX][nextY] == 2 || board[nextX][nextY] == 3) {
                    return dir;
                }
                // Store state: {x, y, initialDirection}
                queue.add(new int[]{nextX, nextY, dir});
                visited[nextX][nextY] = true;
            }
        }

        visited[startX][startY] = true;

        // BFS Loop
        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int currX = current[0];
            int currY = current[1];
            int firstDir = current[2];

            int[][] moves = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

            for (int[] move : moves) {
                int nx = currX + move[0];
                int ny = currY + move[1];

                if (isValid(board, nx, ny) && !visited[nx][ny]) {
                    // Found a target (dot or power-up)
                    if (board[nx][ny] == 2 || board[nx][ny] == 3) {
                        return firstDir; // Return the direction that led to this path
                    }

                    visited[nx][ny] = true;
                    queue.add(new int[]{nx, ny, firstDir});
                }
            }
        }

        // Fallback: if no path found, make a random move to avoid getting stuck
        int randomDir = (int)(Math.random() * 4);
        return randomDir;
    }

    /**
     * Helper method to validate if a coordinate is within bounds and not a wall.
     */
    private static boolean isValid(int[][] board, int x, int y) {
        int w = board.length;
        int h = board[0].length;
        return x >= 0 && x < w && y >= 0 && y < h && board[x][y] != 1;
    }

    /**
     * Parses a position string "x,y" into an integer array.
     */
    private static int[] parseXY(String s) {
        try {
            String[] p = s.split(",");
            return new int[]{(int)Double.parseDouble(p[0]), (int)Double.parseDouble(p[1])};
        } catch (Exception e) { return new int[]{0,0}; }
    }
}