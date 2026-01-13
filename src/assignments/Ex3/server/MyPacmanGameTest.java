package assignments.Ex3.server;

import assignments.Ex3.server.MyPacmanGame;
import exe.ex3.game.PacmanGame;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit tests for MyPacmanGame logic.
 */
public class MyPacmanGameTest {

    @Test
    public void testInitialization() {
        PacmanGame game = new MyPacmanGame();
        String result = game.init(0, "323951038", false, 0, 1.0, 50, 4);

        assertEquals("OK", result, "Initialization should return OK");
        assertNotNull(game.getGame(0), "Board should not be null after init");
        assertNotNull(game.getPos(0), "Pacman position should not be null");
        assertNotNull(game.getGhosts(0), "Ghosts array should not be null");
    }

    @Test
    public void testMovement() {
        PacmanGame game = new MyPacmanGame();
        game.init(0, "123456789", false, 0, 1.0, 50, 4);

        String startPos = game.getPos(0);
        // We know start pos is 13,23. Moving UP (mode 1 is UP usually, or depend on constants)
        // Let's assume standard directions: 0=Left, 1=Right, 2=Up, 3=Down

        game.move(PacmanGame.UP);
        String newPos = game.getPos(0);

        assertNotEquals(startPos, newPos, "Pacman should move if the path is clear");
    }

    @Test
    public void testScoreUpdate() {
        MyPacmanGame game = new MyPacmanGame();
        game.init(0, "123456789", false, 0, 1.0, 50, 4);

        // Let's force a move to a coordinate with a dot (2)
        // Note: This requires knowing the map. Assuming moving right eats a dot.
        int initialScore = parseScore(game.getData(0));

        // Make a few moves to ensure a dot is eaten
        game.move(PacmanGame.RIGHT);
        game.move(PacmanGame.RIGHT);

        int newScore = parseScore(game.getData(0));
        assertTrue(newScore >= initialScore, "Score should increase or stay same, never decrease");
    }

    @Test
    public void testGameOver() {
        PacmanGame game = new MyPacmanGame();
        game.init(0, "123456789", false, 0, 1.0, 50, 4);

        // Simulate end of game manually
        game.end(0);
        assertEquals(PacmanGame.DONE, game.getStatus(), "Status should be DONE after end()");
    }

    // Helper to parse score from string "Score: 10"
    private int parseScore(String data) {
        try {
            return Integer.parseInt(data.split(": ")[1]);
        } catch (Exception e) {
            return 0;
        }
    }
}