package assignments.Ex3.server;

import exe.ex3.game.GhostCL;
import exe.ex3.game.PacmanGame;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/**
 * Implements the server-side logic for the Pacman game.
 * This class manages the game state, board, player movement, ghosts' AI, and scoring.
 */
public class MyPacmanGame implements PacmanGame {
    private int[][] board;
    private String pos = "13,23"; // Starting position of Pacman
    private int status = INIT;
    private int score = 0;

    // Initial positions for the ghosts
    private String[] ghostsPos = {"13,13", "14,13", "12,13"};
    // Tracks which ghosts were eaten during the current power-up mode
    private boolean[] ghostsEaten = {false, false, false};

    private Random rand = new Random();

    private int powerUpTimer = 0;
    // Duration of the power-up effect in game ticks
    private static final int POWER_UP_DURATION = 40;

    // Counts moves to control ghost speed relative to Pacman
    private int moveCount = 0;

    /**
     * Initializes the game board and state.
     * @param level The level number (not used in this implementation).
     * @param id The user ID.
     * @return "OK" if initialization succeeds.
     */
    @Override
    public String init(int level, String id, boolean cy, long seed, double res, int dt, int scenario) {
        this.board = createClassicMap();
        this.status = PLAY;
        return "OK";
    }

    /**
     * Returns the current game board matrix.
     * 0 = Empty, 1 = Wall, 2 = Dot, 3 = Power-up.
     */
    @Override
    public int[][] getGame(int id) { return board; }

    /**
     * Returns the current position of the Pacman as a string "x,y".
     */
    @Override
    public String getPos(int id) { return pos; }

    /**
     * Returns the array of Ghost objects with their current status and position.
     */
    @Override
    public GhostCL[] getGhosts(int id) {
        GhostCL[] ghosts = new GhostCL[ghostsPos.length];
        for (int i = 0; i < ghostsPos.length; i++) {
            final String p = ghostsPos[i];
            // Ghost is blue (type 1) only if power-up is active and it hasn't been eaten yet
            final int type = (powerUpTimer > 0 && !ghostsEaten[i]) ? 1 : 0;

            ghosts[i] = new GhostCL() {
                @Override public String getPos(int n) { return p; }
                @Override public int getStatus() { return 0; }
                @Override public double remainTimeAsEatable(int n) { return (double)powerUpTimer; }
                @Override public String getInfo() { return ""; }
                @Override public int getType() { return type; }
            };
        }
        return ghosts;
    }

    /**
     * Moves the Pacman in the given direction and updates the game state.
     * Handles movement, collisions, scoring, and ghost AI.
     * @param dir The direction to move (UP, DOWN, LEFT, RIGHT).
     * @return "OK" or "DONE" if the game is over.
     */
    @Override
    public String move(int dir) {
        if (status == DONE) return "DONE";

        moveCount++;

        // --- Pacman Movement ---
        int[] xy = parseXY(pos);
        int nextX = xy[0], nextY = xy[1];

        if (dir == UP) nextY--;
        if (dir == DOWN) nextY++;
        if (dir == LEFT) nextX--;
        if (dir == RIGHT) nextX++;

        // Handle tunnel (teleportation)
        if (nextX < 0) nextX = board.length - 1;
        else if (nextX >= board.length) nextX = 0;

        // Manage power-up timer
        if (powerUpTimer > 0) {
            powerUpTimer--;
            if (powerUpTimer == 0) {
                // Reset eaten status when power-up ends
                for(int i=0; i<ghostsEaten.length; i++) ghostsEaten[i] = false;
            }
        }

        // Validate move and update board
        if (isValid(nextX, nextY)) {
            pos = nextX + "," + nextY;

            // Eat items
            if (board[nextX][nextY] == 2) { // Regular dot
                board[nextX][nextY] = 0;
                score += 10;
            } else if (board[nextX][nextY] == 3) { // Power-up
                board[nextX][nextY] = 0;
                score += 50;
                powerUpTimer = POWER_UP_DURATION;
                // Reset eaten status for new power-up
                for(int i=0; i<ghostsEaten.length; i++) ghostsEaten[i] = false;
            }
        }

        checkCollision();

        // --- Ghost AI ---
        // Ghosts move only 3 out of 4 ticks to make Pacman slightly faster
        if (moveCount % 4 != 0) {
            moveGhostsSmart();
        }

        checkCollision();

        if (isGameOver()) status = DONE;

        return "OK";
    }

    /**
     * Checks if Pacman collides with any ghost.
     * Handles both losing condition and eating ghosts.
     */
    private void checkCollision() {
        int[] pacXY = parseXY(pos);
        for (int i = 0; i < ghostsPos.length; i++) {
            int[] ghostXY = parseXY(ghostsPos[i]);
            if (pacXY[0] == ghostXY[0] && pacXY[1] == ghostXY[1]) {
                if (powerUpTimer > 0 && !ghostsEaten[i]) {
                    // Pacman eats ghost
                    score += 200;
                    ghostsPos[i] = "13,14"; // Send back to ghost house
                    ghostsEaten[i] = true;
                } else {
                    // Ghost catches Pacman
                    status = DONE;
                }
            }
        }
    }

    /**
     * Checks if all dots have been eaten.
     * @return true if the board is clear of dots.
     */
    private boolean isGameOver() {
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[0].length; y++) {
                if (board[x][y] == 2 || board[x][y] == 3) return false;
            }
        }
        return true;
    }

    /**
     * Controls the movement logic for all ghosts.
     * Implements different behaviors (chase, scatter, frighten) and BFS pathfinding.
     */
    private void moveGhostsSmart() {
        int[] pacXY = parseXY(pos);

        for (int i = 0; i < ghostsPos.length; i++) {
            int[] gxy = parseXY(ghostsPos[i]);
            int gx = gxy[0], gy = gxy[1];

            // 1. Exit ghost house logic
            if (gy >= 12 && gy <= 16 && gx >= 10 && gx <= 17) {
                if (isValid(gx, gy - 1) || (gx == 13 && gy == 12)) {
                    ghostsPos[i] = gx + "," + (gy - 1);
                } else if (gx < 13 && isValid(gx+1, gy)) {
                    ghostsPos[i] = (gx+1) + "," + gy;
                } else if (gx > 13 && isValid(gx-1, gy)) {
                    ghostsPos[i] = (gx-1) + "," + gy;
                } else {
                    moveRandom(i, gx, gy);
                }
                continue;
            }

            // 2. Main movement logic
            boolean isScared = (powerUpTimer > 0 && !ghostsEaten[i]);
            boolean moveRandomly = false;

            if (isScared) {
                moveRandomly = true; // Ghosts scatter when scared
            } else {
                // Determine behavior based on ghost index (simulating personality)
                int chance = rand.nextInt(100);

                if (i == 0) {
                    // Red ghost: Aggressive, small chance of error (40%)
                    if (chance < 40) moveRandomly = true;
                } else if (i == 1) {
                    // Pink ghost: Moderate chance of error (60%)
                    if (chance < 60) moveRandomly = true;
                } else {
                    // Blue ghost: Mostly random (80%)
                    if (chance < 80) moveRandomly = true;
                }
            }

            if (moveRandomly) {
                moveRandom(i, gx, gy);
            } else {
                // Use BFS to find the shortest path to Pacman
                int[] bestMove = getNextMoveBFS(gx, gy, pacXY[0], pacXY[1]);
                if (bestMove != null) {
                    ghostsPos[i] = bestMove[0] + "," + bestMove[1];
                } else {
                    moveRandom(i, gx, gy);
                }
            }
        }
    }

    /**
     * Moves a ghost in a random valid direction.
     */
    private void moveRandom(int i, int gx, int gy) {
        int[][] dirs = {{0,-1}, {0,1}, {-1,0}, {1,0}};
        // Shuffle directions
        for (int k = 0; k < dirs.length; k++) {
            int r = rand.nextInt(dirs.length);
            int[] temp = dirs[k]; dirs[k] = dirs[r]; dirs[r] = temp;
        }

        for(int[] d : dirs) {
            int nx = gx + d[0];
            int ny = gy + d[1];

            // Handle tunnel
            if (nx < 0) nx = board.length - 1;
            else if (nx >= board.length) nx = 0;

            if(isValid(nx, ny)) {
                ghostsPos[i] = nx + "," + ny;
                return;
            }
        }
    }

    /**
     * Calculates the next move using BFS (Breadth-First Search).
     * @return The coordinates of the next step towards the target.
     */
    private int[] getNextMoveBFS(int startX, int startY, int targetX, int targetY) {
        int w = board.length;
        int h = board[0].length;
        int[][] dist = new int[w][h];
        for(int i=0; i<w; i++) for(int j=0; j<h; j++) dist[i][j] = -1;

        Queue<int[]> q = new LinkedList<>();
        q.add(new int[]{targetX, targetY});
        dist[targetX][targetY] = 0;

        while(!q.isEmpty()) {
            int[] curr = q.poll();
            int cx = curr[0], cy = curr[1];
            if (cx == startX && cy == startY) break;

            int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}};
            for(int[] d : dirs) {
                int nx = cx + d[0];
                int ny = cy + d[1];

                // Handle tunnel during search
                if (nx < 0) nx = w - 1; else if (nx >= w) nx = 0;

                if (isValid(nx, ny) && dist[nx][ny] == -1) {
                    dist[nx][ny] = dist[cx][cy] + 1;
                    q.add(new int[]{nx, ny});
                }
            }
        }

        // Find neighbor with the smallest distance value
        int bestDist = 999999;
        int[] bestMove = null;
        int[][] dirs = {{0,-1}, {0,1}, {-1,0}, {1,0}};

        for(int[] d : dirs) {
            int nx = startX + d[0];
            int ny = startY + d[1];
            if (nx < 0) nx = w - 1; else if (nx >= w) nx = 0;

            if (isValid(nx, ny) && dist[nx][ny] != -1) {
                if (dist[nx][ny] < bestDist) {
                    bestDist = dist[nx][ny];
                    bestMove = new int[]{nx, ny};
                }
            }
        }
        return bestMove;
    }

    /**
     * Checks if a coordinate is walkable (not a wall).
     */
    private boolean isValid(int x, int y) {
        return x >= 0 && x < board.length && y >= 0 && y < board[0].length && board[x][y] != 1;
    }

    @Override public int getStatus() { return status; }

    /**
     * Generates the game board map.
     * @return A 2D array representing the map.
     */
    private int[][] createClassicMap() {
        int W = 28;
        int H = 31;
        int[][] b = new int[W][H];

        String[] mapStr = {
                "1111111111111111111111111111",
                "1222222222222112222222222221",
                "1211112111112112111112111121",
                "1311112111112112111112111131",
                "1211112111112112111112111121",
                "1222222222222222222222222221",
                "1211112112111111112112111121",
                "1211112112111111112112111121",
                "1222222112222112222112222221",
                "1111112111110110111112111111",
                "0000012111110110111112100000",
                "0000012110000000000112100000",
                "0000012110000000000112100000",
                "1111112110100000010112111111",
                "0000002000100000010002000000",
                "1111112110100000010112111111",
                "0000012110111111110112100000",
                "0000012110000000000112100000",
                "0000012110111111110112100000",
                "1111112110111111110112111111",
                "1222222222222112222222222221",
                "1211112111112112111112111121",
                "1222112222222002222222112221",
                "1112112112111111112112112111",
                "1112112112111111112112112111",
                "1222222112222112222112222221",
                "1211111111112112111111111121",
                "1311111111112112111111111131",
                "1211111111112112111111111121",
                "1222222222222222222222222221",
                "1111111111111111111111111111"
        };

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                char c = mapStr[y].charAt(x);
                b[x][y] = Character.getNumericValue(c);
            }
        }

        return b;
    }

    private int[] parseXY(String s) {
        try {
            String[] p = s.split(",");
            return new int[]{(int)Double.parseDouble(p[0]), (int)Double.parseDouble(p[1])};
        } catch (Exception e) { return new int[]{1, 1}; }
    }

    @Override public String getData(int id) { return "Score: " + score; }

    @Override public boolean isCyclic() { return false; }
    @Override public void play() {}
    @Override public String end(int id) { status = DONE; return "DONE"; }
    @Override public Character getKeyChar() { return null; }
}