package assignments.Ex3.server;

import exe.ex3.game.GhostCL;
import exe.ex3.game.PacmanGame;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/**
 * Server Logic Implementation.
 * Manages game rules, ghost AI using BFS, and collision handling.
 * Includes specific logic to prevent ghosts from re-entering the "Ghost House".
 */
public class MyPacmanGame implements PacmanGame {
    private int[][] board;
    private String pos = "13,22"; // Pacman starting position
    private int status = INIT;
    private int score = 0;

    // Ghosts starting positions (inside the Ghost House)
    private String[] ghostsPos = {"13,13", "14,13", "12,13"};
    private boolean[] ghostsEaten = {false, false, false};

    private Random rand = new Random();
    private int powerUpTimer = 0;
    private static final int POWER_UP_DURATION = 40;
    private int moveCount = 0;

    @Override
    public String init(int level, String id, boolean cy, long seed, double res, int dt, int scenario) {
        this.board = createClassicMap();
        this.status = PLAY;
        return "OK";
    }

    @Override
    public int[][] getGame(int id) { return board; }

    @Override
    public String getPos(int id) { return pos; }

    @Override
    public GhostCL[] getGhosts(int id) {
        GhostCL[] ghosts = new GhostCL[ghostsPos.length];
        for (int i = 0; i < ghostsPos.length; i++) {
            final String p = ghostsPos[i];
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

    @Override
    public String move(int dir) {
        if (status == DONE) return "DONE";

        moveCount++;

        // --- 1. Handle Pacman Movement ---
        int[] xy = parseXY(pos);
        int nextX = xy[0], nextY = xy[1];

        // Adjust coordinates based on direction (UP decreases Y)
        if (dir == UP) nextY--;
        if (dir == DOWN) nextY++;
        if (dir == LEFT) nextX--;
        if (dir == RIGHT) nextX++;

        // Handle Tunneling (Teleportation)
        if (nextX < 0) nextX = board.length - 1;
        else if (nextX >= board.length) nextX = 0;

        // Handle PowerUp Timer
        if (powerUpTimer > 0) {
            powerUpTimer--;
            if (powerUpTimer == 0) {
                // Reset eaten status when powerup ends
                for(int i=0; i<ghostsEaten.length; i++) ghostsEaten[i] = false;
            }
        }

        // Validate Move and Update State
        if (isValid(nextX, nextY)) {
            pos = nextX + "," + nextY;

            // Handle coin collection
            if (board[nextX][nextY] == 2) {
                board[nextX][nextY] = 0;
                score += 10;
            } else if (board[nextX][nextY] == 3) { // Power pellet
                board[nextX][nextY] = 0;
                score += 50;
                powerUpTimer = POWER_UP_DURATION;
                for(int i=0; i<ghostsEaten.length; i++) ghostsEaten[i] = false;
            }
        }

        checkCollision();

        // --- 2. Handle Ghost Movement (Every 4th frame) ---
        if (moveCount % 4 == 0) {
            moveGhostsSmart();
        }

        checkCollision();

        if (isGameOver()) status = DONE;

        return "OK";
    }

    private void checkCollision() {
        int[] pacXY = parseXY(pos);
        for (int i = 0; i < ghostsPos.length; i++) {
            int[] ghostXY = parseXY(ghostsPos[i]);

            // Check if Pacman and Ghost occupy the same tile
            if (pacXY[0] == ghostXY[0] && pacXY[1] == ghostXY[1]) {
                if (powerUpTimer > 0 && !ghostsEaten[i]) {
                    // Eat Ghost
                    score += 200;
                    ghostsPos[i] = "13,14"; // Send back to house
                    ghostsEaten[i] = true;
                } else {
                    // Game Over
                    status = DONE;
                }
            }
        }
    }

    private boolean isGameOver() {
        // Check if any coins remain
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[0].length; y++) {
                if (board[x][y] == 2 || board[x][y] == 3) return false;
            }
        }
        return true;
    }

    /**
     * Main Ghost AI Logic.
     * Handles exiting the Ghost House and chasing Pacman via BFS.
     */
    private void moveGhostsSmart() {
        int[] pacXY = parseXY(pos);

        for (int i = 0; i < ghostsPos.length; i++) {
            int[] gxy = parseXY(ghostsPos[i]);
            int gx = gxy[0], gy = gxy[1];

            // A. Ghost House Exit Logic
            // If ghost is inside the house box (approx coords), force it out.
            if (gy >= 12 && gy <= 16 && gx >= 10 && gx <= 17) {
                // Guide ghost to center (13,14) then UP
                if (gx == 13 || gx == 14) {
                    if (isValid(gx, gy - 1) || gy == 12) {
                        ghostsPos[i] = gx + "," + (gy - 1);
                    }
                } else if (gx < 13) {
                    ghostsPos[i] = (gx + 1) + "," + gy; // Move Right
                } else {
                    ghostsPos[i] = (gx - 1) + "," + gy; // Move Left
                }
                continue; // Skip standard movement for this turn
            }

            // B. Standard Movement (Chase or Flee)
            boolean isScared = (powerUpTimer > 0 && !ghostsEaten[i]);

            if (isScared) {
                moveRandom(i, gx, gy); // Random movement when scared
            } else {
                // BFS Chase Logic
                int[] bestMove = getNextMoveBFS(gx, gy, pacXY[0], pacXY[1]);
                if (bestMove != null) {
                    ghostsPos[i] = bestMove[0] + "," + bestMove[1];
                } else {
                    moveRandom(i, gx, gy);
                }
            }
        }
    }

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
            // Handle tunneling
            if (nx < 0) nx = board.length - 1; else if (nx >= board.length) nx = 0;

            if(isValid(nx, ny)) {
                ghostsPos[i] = nx + "," + ny;
                return;
            }
        }
    }

    /**
     * Calculates the Shortest Path to the target using BFS.
     * Crucially, treats the "Ghost House" as a wall to prevent re-entry.
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
                if (nx < 0) nx = w - 1; else if (nx >= w) nx = 0;

                boolean insideGhostHouse = (ny >= 12 && ny <= 16 && nx >= 10 && nx <= 17);

                if (isValid(nx, ny) && !insideGhostHouse && dist[nx][ny] == -1) {
                    dist[nx][ny] = dist[cx][cy] + 1;
                    q.add(new int[]{nx, ny});
                }
            }
        }

        int bestDist = Integer.MAX_VALUE;
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

    private boolean isValid(int x, int y) {
        return x >= 0 && x < board.length && y >= 0 && y < board[0].length && board[x][y] != 1;
    }

    @Override public int getStatus() { return status; }

    private int[][] createClassicMap() {
        int W = 28, H = 31;
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