package assignments.Ex3.test;

import assignments.Ex3.Ex3Algo;
import exe.ex3.game.GhostCL;
import exe.ex3.game.PacManAlgo;
import exe.ex3.game.PacmanGame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Ex3AlgoTest {

    // Fake minimal game for tests
    static class FakeGame implements PacmanGame {
        private final int[][] board;
        private String pacPos;
        private final boolean cyclic;

        FakeGame(int[][] board, String pacPos, boolean cyclic) {
            this.board = board;
            this.pacPos = pacPos;
            this.cyclic = cyclic;
        }

        @Override public String getPos(int id) { return pacPos; }
        @Override public int[][] getGame(int id) { return board; }
        @Override public boolean isCyclic() { return cyclic; }

        // not used in our unit tests / algo (return safe defaults)
        @Override public Character getKeyChar() { return null; }
        @Override public GhostCL[] getGhosts(int id) { return null; }
        @Override public String move(int dir) { return ""; }
        @Override public void play() {}
        @Override public String end(int id) { return ""; }
        @Override public String getData(int id) { return ""; }
        @Override public int getStatus() { return PLAY; }
        @Override public String init(int level, String id, boolean cy, long seed, double res, int dt, int scenario) { return ""; }
    }

    @Test
    public void testAlgoCreation() {
        PacManAlgo algo = new Ex3Algo();
        assertNotNull(algo);
    }

    @Test
    public void testMoveReturnsLegalDirection() {
        PacManAlgo algo = new Ex3Algo();
        int[][] board = new int[5][5];      // empty
        FakeGame game = new FakeGame(board, "2,2", false);

        int dir = algo.move(game);

        assertTrue(dir == 0 || dir == 1 || dir == 2 || dir == 3 || dir == 4,
                "Direction must be one of {0,1,2,3,4}");
    }


}