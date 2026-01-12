package assignments.Ex3.server;

import exe.ex3.game.GhostCL;
import exe.ex3.game.PacmanGame;

public class MyPacmanGame implements PacmanGame {

    private int[][] board;
    private int pacX = 1, pacY = 1;
    private boolean cyclic = false;
    private int status = INIT;

    @Override
    public String init(int level, String id, boolean cy, long seed, double res, int dt, int scenario) {
        this.cyclic = cy;

        this.board = makeSimpleBoard(10, 10);
        this.pacX = 1;
        this.pacY = 1;
        this.status = PLAY;
        return "OK";
    }

    @Override
    public int[][] getGame(int id) {
        return board;
    }

    @Override
    public String getPos(int id) {
        return pacX + "," + pacY;
    }

    @Override
    public boolean isCyclic() {
        return cyclic;
    }

    @Override
    public String move(int dir) {
        if (board == null) return "ERR:not_inited";

        int nx = pacX, ny = pacY;

        if (dir == LEFT)  nx--;
        if (dir == RIGHT) nx++;
        if (dir == UP)    ny--;
        if (dir == DOWN)  ny++;

        if (cyclic) {
            if (nx < 0) nx = board.length - 1;
            if (nx >= board.length) nx = 0;
            if (ny < 0) ny = board[0].length - 1;
            if (ny >= board[0].length) ny = 0;
        } else {
            if (nx < 0 || nx >= board.length || ny < 0 || ny >= board[0].length) {
                return "BLOCK:out";
            }
        }

        if (board[nx][ny] == 1) {
            return "BLOCK:wall";
        }

        pacX = nx;
        pacY = ny;
        return "OK";
    }

    @Override
    public void play() {}

    @Override
    public String end(int id) {
        status = DONE;
        return "DONE";
    }

    @Override
    public String getData(int id) { return ""; }

    @Override
    public int getStatus() { return status; }

    @Override
    public Character getKeyChar() { return null; }

    @Override
    public GhostCL[] getGhosts(int id) { return null; }

    //  helper
    private static int[][] makeSimpleBoard(int w, int h) {
        int[][] b = new int[w][h];
        for (int x = 0; x < w; x++) {
            b[x][0] = 1;
            b[x][h - 1] = 1;
        }
        for (int y = 0; y < h; y++) {
            b[0][y] = 1;
            b[w - 1][y] = 1;
        }
        return b;
    }
}