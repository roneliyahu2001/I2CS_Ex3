package assignments.Ex3;

import exe.ex3.game.Game;
import exe.ex3.game.PacManAlgo;
import exe.ex3.game.PacmanGame;

import java.awt.Color;

public class Ex3Algo implements PacManAlgo {

    // Debug
    private static final boolean DEBUG = true;
    private static final int PRINT_EVERY = 10;

    private int step = 0;

    // cyclic planning (assignments.Ex3.Map2D tunnel)
    private boolean useCyclic = true;

    // anti-stuck / auto-disable cyclic if engine doesn't teleport
    private int prevX = Integer.MIN_VALUE, prevY = Integer.MIN_VALUE;
    private boolean lastPlannedWrap = false;

    @Override
    public String getInfo() {
        return "BFS shortest-path with assignments.Ex3.Map2D cyclic tunnels + auto-disable if engine doesn't teleport";
    }

    @Override
    public int move(PacmanGame game) {
        int code = 0;
        step++;

        int[][] board = game.getGame(code);

        // colors
        int WALL  = Game.getIntColor(Color.BLUE,  code);
        int PINK  = Game.getIntColor(Color.PINK,  code);
        int GREEN = Game.getIntColor(Color.GREEN, code);

        Pixel2D pac = getPacmanPixel(game, code);
        if (pac == null) {
            dbg("PAC null -> STAY");
            return Game.STAY;
        }

        int width  = board.length;       // assignments.Ex3.Map2D is [x][y]
        int height = board[0].length;

        // If last step planned a wrap, but Pac-Man didn't move -> engine likely doesn't support teleport.
        if (pac.getX() == prevX && pac.getY() == prevY && lastPlannedWrap && useCyclic) {
            useCyclic = false;
            dbg("AUTO: planned WRAP but PAC didn't move -> disabling cyclic tunnels");
        }

        // Build map
        Map myMap = new Map(board);
        myMap.setCyclic(useCyclic);

        // Pick target: prefer GREEN if it's close, else nearest PINK.
        Pixel2D target = pickTarget(myMap, pac, WALL, PINK, GREEN);
        if (target == null) {
            dbg("No reachable target -> random safe");
            prevX = pac.getX(); prevY = pac.getY();
            lastPlannedWrap = false;
            return randomSafe(board, pac, WALL);
        }

        // Shortest path
        Pixel2D[] path = myMap.shortestPath(pac, target, WALL);
        if (path == null || path.length < 2) {
            dbg("No path / already at target -> random safe");
            prevX = pac.getX(); prevY = pac.getY();
            lastPlannedWrap = false;
            return randomSafe(board, pac, WALL);
        }

        Pixel2D cur = path[0];
        Pixel2D nxt = path[1];

        // detect if the planned step is a wrap jump (difference > 1)
        lastPlannedWrap = isWrapStep(cur, nxt, width, height);

        int dir = directionWithWrap(cur, nxt, width, height);

        if (DEBUG && step % PRINT_EVERY == 1) {
            dbg("STEP=" + step +
                    " useCyclic=" + useCyclic +
                    " PAC=" + pac.getX() + "," + pac.getY() +
                    " TARGET=" + target.getX() + "," + target.getY() +
                    " NEXT=" + nxt.getX() + "," + nxt.getY() +
                    " wrapPlanned=" + lastPlannedWrap +
                    " dir=" + dir);
            dbg("Game consts: UP=" + Game.UP + " DOWN=" + Game.DOWN + " LEFT=" + Game.LEFT + " RIGHT=" + Game.RIGHT + " STAY=" + Game.STAY);
        }

        // Save position for next step comparison
        prevX = pac.getX(); prevY = pac.getY();

        return dir;
    }

    // Target selection
    // Choose GREEN if reachable and within a small distance threshold; else closest PINK.
    private Pixel2D pickTarget(Map myMap, Pixel2D pac, int wall, int pink, int green) {
        Map2D dist = myMap.allDistance(pac, wall);

        Pixel2D bestPink = null;
        int bestPinkD = Integer.MAX_VALUE;

        Pixel2D bestGreen = null;
        int bestGreenD = Integer.MAX_VALUE;

        int w = myMap.getWidth();
        int h = myMap.getHeight();

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int cell = myMap.getPixel(x, y);
                int d = dist.getPixel(x, y);
                if (d < 0) continue;

                if (cell == green && d < bestGreenD) {
                    bestGreenD = d;
                    bestGreen = new Index2D(x, y);
                } else if (cell == pink && d < bestPinkD) {
                    bestPinkD = d;
                    bestPink = new Index2D(x, y);
                }
            }
        }

        // Prefer green if it's reasonably close (tweak if you want)
        if (bestGreen != null && bestGreenD <= 8) return bestGreen;
        return bestPink;
    }

    // Direction mapping
    // IMPORTANT: In your engine logs, vertical is flipped:
    // moving to y+1 corresponds to Game.UP, and y-1 corresponds to Game.DOWN.
    private int directionWithWrap(Pixel2D cur, Pixel2D next, int width, int height) {
        int x = cur.getX(), y = cur.getY();
        int nx = next.getX(), ny = next.getY();

        // LEFT (including wrap from 0 -> width-1)
        if (ny == y && (nx == x - 1 || (x == 0 && nx == width - 1))) return Game.LEFT;

        // RIGHT (including wrap from width-1 -> 0)
        if (ny == y && (nx == x + 1 || (x == width - 1 && nx == 0))) return Game.RIGHT;

        // UP is y+1 (including wrap from height-1 -> 0)
        if (nx == x && (ny == y + 1 || (y == height - 1 && ny == 0))) return Game.UP;

        // DOWN is y-1 (including wrap from 0 -> height-1)
        if (nx == x && (ny == y - 1 || (y == 0 && ny == height - 1))) return Game.DOWN;

        return Game.STAY;
    }

    private boolean isWrapStep(Pixel2D a, Pixel2D b, int width, int height) {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        // normal neighbors are dx+dy==1. wrap neighbor often gives dx==width-1 or dy==height-1
        return (dx > 1 || dy > 1) || (dx == width - 1) || (dy == height - 1);
    }

    // Position parsing
    private Pixel2D getPacmanPixel(PacmanGame game, int code) {
        Object posObj = game.getPos(code);
        if (posObj == null) return null;

        if (posObj instanceof Pixel2D) {
            Pixel2D p = (Pixel2D) posObj;
            return new Index2D(p.getX(), p.getY());
        }

        // In your runs it's usually a String like "11,14"
        String s = String.valueOf(posObj).trim();
        String[] parts = s.split("[,\\s]+");
        if (parts.length >= 2) {
            try {
                int x = (int) Math.round(Double.parseDouble(parts[0]));
                int y = (int) Math.round(Double.parseDouble(parts[1]));
                return new Index2D(x, y);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    // Fallback move
    private int randomSafe(int[][] board, Pixel2D pac, int wall) {
        // try a few directions in a fixed order; if blocked -> stay
        int x = pac.getX(), y = pac.getY();

        // RIGHT
        if (isFree(board, x + 1, y, wall)) return Game.RIGHT;
        // LEFT
        if (isFree(board, x - 1, y, wall)) return Game.LEFT;
        // UP (y+1)
        if (isFree(board, x, y + 1, wall)) return Game.UP;
        // DOWN (y-1)
        if (isFree(board, x, y - 1, wall)) return Game.DOWN;

        return Game.STAY;
    }

    private boolean isFree(int[][] board, int x, int y, int wall) {
        if (x < 0 || x >= board.length) return false;
        if (y < 0 || y >= board[0].length) return false;
        return board[x][y] != wall;
    }

    private void dbg(String s) {
        if (DEBUG) System.out.println(s);
    }
}