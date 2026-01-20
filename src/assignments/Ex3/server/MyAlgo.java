package assignments.Ex3.server;

import exe.ex3.game.Game;
import exe.ex3.game.GhostCL;
import exe.ex3.game.PacManAlgo;
import exe.ex3.game.PacmanGame;

import java.util.LinkedList;
import java.util.Queue;

/**
 * MyAlgo - Stable Version (fixed).
 *
 * Fixes / Improvements:
 * 1) No crashes: bounds checks for ghost positions before marking visited.
 * 2) Robust parsePosArray: supports "x,y", "x,y,z", "(x,y,z)", with spaces.
 * 3) Safer BFS: can block a radius around ghosts in safeMode (not only exact cell).
 * 4) Anti-jitter: avoids quick flip-flops (simple cooldown + avoid opposite when possible).
 */
public class MyAlgo implements PacManAlgo {

    private int lastDir = Game.UP;
    private int jitterCooldown = 0; // small cooldown after changing direction

    @Override
    public String getInfo() {
        return "Stable Algo (fixed)";
    }

    @Override
    public int move(PacmanGame game) {
        int[][] board = game.getGame(0);
        int[] p = parsePosArray(game.getPos(0));
        int myX = clamp(p[0], 0, board.length - 1);
        int myY = clamp(p[1], 0, board[0].length - 1);

        GhostCL[] ghosts = game.getGhosts(0);

        int nextDir;

        // If close to ghosts => run away
        if (isInDanger(myX, myY, ghosts, 15)) {
            nextDir = runAway(board, myX, myY, ghosts);
        } else {
            // Try safe BFS first (blocks around ghosts), then fallback BFS
            nextDir = findNearestFoodBFS(board, myX, myY, ghosts, true);
            if (nextDir == -1) nextDir = findNearestFoodBFS(board, myX, myY, ghosts, false);
        }

        // Fallback if nothing found
        if (nextDir == -1) nextDir = randomDir();

        // --- Anti-jitter logic ---
        nextDir = applyAntiJitter(board, myX, myY, nextDir);

        lastDir = nextDir;
        return nextDir;
    }

    // ----------------------------
    // BFS for nearest food
    // ----------------------------
    private int findNearestFoodBFS(int[][] board, int startX, int startY, GhostCL[] ghosts, boolean safeMode) {
        int w = board.length;
        int h = board[0].length;

        boolean[][] visited = new boolean[w][h];

        // If safeMode: block radius around each ghost (safer than blocking only the exact cell)
        if (safeMode) {
            int blockRadius = 2; // you can tune: 1..4
            for (GhostCL g : ghosts) {
                int[] gp = parsePosArray(g.getPos(0));
                int gx = gp[0], gy = gp[1];

                // Mark a small square radius around the ghost as visited (blocked)
                for (int dx = -blockRadius; dx <= blockRadius; dx++) {
                    for (int dy = -blockRadius; dy <= blockRadius; dy++) {
                        int bx = gx + dx;
                        int by = gy + dy;
                        if (inBounds(board, bx, by)) {
                            visited[bx][by] = true;
                        }
                    }
                }
            }
        }

        // never block our own current cell
        if (inBounds(board, startX, startY)) visited[startX][startY] = false;

        Queue<int[]> q = new LinkedList<>();
        int[] moves = {Game.UP, Game.DOWN, Game.LEFT, Game.RIGHT}; // priority order

        // seed BFS with 1-step neighbors and remember the first direction
        for (int dir : moves) {
            int nx = startX + dx(dir);
            int ny = startY + dy(dir);

            if (isValid(board, nx, ny) && !visited[nx][ny]) {
                if (isFood(board, nx, ny)) return dir;
                visited[nx][ny] = true;
                q.add(new int[]{nx, ny, dir});
            }
        }

        while (!q.isEmpty()) {
            int[] curr = q.poll();
            int cx = curr[0], cy = curr[1], firstDir = curr[2];

            if (isFood(board, cx, cy)) return firstDir;

            for (int dir : moves) {
                int nx = cx + dx(dir);
                int ny = cy + dy(dir);

                if (isValid(board, nx, ny) && !visited[nx][ny]) {
                    visited[nx][ny] = true;
                    q.add(new int[]{nx, ny, firstDir});
                }
            }
        }

        return -1;
    }

    private boolean isFood(int[][] board, int x, int y) {
        // keep your original assumption: 2 or 3 are food
        return board[x][y] == 2 || board[x][y] == 3;
    }

    // ----------------------------
    // Danger / Run away
    // ----------------------------
    private boolean isInDanger(int x, int y, GhostCL[] ghosts, double radius) {
        for (GhostCL g : ghosts) {
            int[] gp = parsePosArray(g.getPos(0));
            double dist = Math.sqrt(Math.pow(x - gp[0], 2) + Math.pow(y - gp[1], 2));
            if (dist < radius) return true;
        }
        return false;
    }

    private int runAway(int[][] board, int x, int y, GhostCL[] ghosts) {
        int[] dirs = {Game.UP, Game.DOWN, Game.LEFT, Game.RIGHT};
        int bestMove = -1;
        double bestScore = -1;

        for (int dir : dirs) {
            int nx = x + dx(dir);
            int ny = y + dy(dir);

            if (!isValid(board, nx, ny)) continue;

            // score by distance to closest ghost (bigger is better)
            double closest = Double.MAX_VALUE;
            for (GhostCL g : ghosts) {
                int[] gp = parsePosArray(g.getPos(0));
                double d = Math.sqrt(Math.pow(nx - gp[0], 2) + Math.pow(ny - gp[1], 2));
                if (d < closest) closest = d;
            }

            // small preference to keep direction (less jitter while escaping)
            double keepBonus = (dir == lastDir) ? 0.25 : 0.0;

            double score = closest + keepBonus;
            if (score > bestScore) {
                bestScore = score;
                bestMove = dir;
            }
        }

        return (bestMove != -1) ? bestMove : randomDir();
    }

    // ----------------------------
    // Anti-jitter helper
    // ----------------------------
    private int applyAntiJitter(int[][] board, int x, int y, int nextDir) {
        // cooldown goes down every move
        if (jitterCooldown > 0) jitterCooldown--;

        // if nextDir is opposite of lastDir, try to keep going straight if possible
        if (nextDir == opposite(lastDir)) {
            int fx = x + dx(lastDir);
            int fy = y + dy(lastDir);
            if (isValid(board, fx, fy)) {
                return lastDir;
            }
        }

        // if we want to change direction and cooldown still active, keep direction if possible
        if (nextDir != lastDir && jitterCooldown > 0) {
            int fx = x + dx(lastDir);
            int fy = y + dy(lastDir);
            if (isValid(board, fx, fy)) {
                return lastDir;
            }
        }

        // if we actually change direction, reset cooldown a bit
        if (nextDir != lastDir) {
            jitterCooldown = 2; // tune: 1..3
        }

        return nextDir;
    }

    // ----------------------------
    // Validity / Bounds / Parse
    // ----------------------------
    private boolean isValid(int[][] board, int x, int y) {
        return inBounds(board, x, y) && board[x][y] != 1;
    }

    private boolean inBounds(int[][] board, int x, int y) {
        return x >= 0 && x < board.length && y >= 0 && y < board[0].length;
    }

    /**
     * Robust parsing:
     * Supports: "x,y", "x,y,z", "(x,y,z)", "x, y", etc.
     * Returns int cell coords using floor cast via (int)Double.parseDouble(...)
     */
    private int[] parsePosArray(String pos) {
        if (pos == null) return new int[]{0, 0};

        try {
            // remove parentheses and spaces
            String s = pos.trim();
            s = s.replace("(", "").replace(")", "").replace(" ", "");

            String[] parts = s.split(",");
            if (parts.length < 2) return new int[]{0, 0};

            int x = (int) Double.parseDouble(parts[0]);
            int y = (int) Double.parseDouble(parts[1]);
            return new int[]{x, y};
        } catch (Exception e) {
            return new int[]{0, 0};
        }
    }

    private int clamp(int v, int lo, int hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    // ----------------------------
    // Directions helpers
    // ----------------------------
    private int randomDir() {
        int[] dirs = {Game.UP, Game.DOWN, Game.LEFT, Game.RIGHT};
        return dirs[(int) (Math.random() * 4)];
    }

    private int opposite(int dir) {
        if (dir == Game.UP) return Game.DOWN;
        if (dir == Game.DOWN) return Game.UP;
        if (dir == Game.LEFT) return Game.RIGHT;
        if (dir == Game.RIGHT) return Game.LEFT;
        return -1;
    }

    private int dx(int dir) {
        if (dir == Game.RIGHT) return 1;
        if (dir == Game.LEFT) return -1;
        return 0;
    }

    private int dy(int dir) {
        // Screen coords: Down is +Y
        if (dir == Game.DOWN) return 1;
        if (dir == Game.UP) return -1;
        return 0;
    }
}