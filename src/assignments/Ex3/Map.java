package assignments.Ex3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;


/**
 * This class represents a 2D map as a "screen" or a raster matrix or maze over integers.
 * @author boaz.benmoshe
 *
 */
public class Map implements Map2D {
	private int[][] _map;
	private boolean _cyclicFlag = true;
	
	/**
	 * Constructs a w*h 2D raster map with an init value v.
	 * @param w
	 * @param h
	 * @param v
	 */
	public Map(int w, int h, int v) {init(w,h, v);}
	/**
	 * Constructs a square map (size*size).
	 * @param size
	 */
	public Map(int size) {this(size,size, 0);}
	
	/**
	 * Constructs a map from a given 2D array.
	 * @param data
	 */
	public Map(int[][] data) {
		init(data);
	}
    @Override
    public void init(int w, int h, int v) {
        if (w <= 0 || h <= 0) {
            throw new RuntimeException("Bad dimensions");
        }
        _map = new int[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                _map[x][y] = v;
            }
        }

    }
    @Override
    public void init(int[][] arr) {
        if (arr == null) {
            throw new RuntimeException("Bad array");
        }
        int w = arr.length;
        int h = arr[0].length;

        for (int i = 0; i < w; i++) {
            if (arr[i] == null || arr[i].length != h) {
                throw new RuntimeException("Ragged array");
            }
        }

        _map = new int[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                _map[x][y] = arr[x][y];
            }
        }

    }

    @Override
    public int[][] getMap() {
        int w = getWidth();
        int h = getHeight();
        int[][] copy = new int[w][h];

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                copy[x][y] = _map[x][y];
            }
        }
        return copy;
    }


    @Override
    public int getWidth() {
        return _map.length;
    }

    @Override
    public int getHeight() {
        return _map[0].length;
    }

    @Override
    public int getPixel(int x, int y) {
        return _map[x][y];
    }

	@Override
	/////// add your code below ///////
	public int getPixel(Pixel2D p) {
		return this.getPixel(p.getX(),p.getY());
	}

    @Override
    public void setPixel(int x, int y, int v) {
        _map[x][y] = v;
    }

    @Override
    public void setPixel(Pixel2D p, int v) {
        setPixel(p.getX(), p.getY(), v);
    }

	@Override
	/** 
	 * Fills this map with the new color (new_v) starting from p.
	 * https://en.wikipedia.org/wiki/Flood_fill
	 */
    public int fill(Pixel2D xy, int new_v) {
        if (xy == null) return 0;
        if (!isInside(xy)) return 0;

        int sx = xy.getX();
        int sy = xy.getY();

        int old = getPixel(sx, sy);
        if (old == new_v) return 0;

        java.util.ArrayDeque<Pixel2D> q = new java.util.ArrayDeque<>();
        q.add(new Index2D(sx, sy));
        setPixel(sx, sy, new_v);
        int count = 1;

        while (!q.isEmpty()) {
            Pixel2D p = q.removeFirst();
            int x = p.getX();
            int y = p.getY();

            int[] dx = {1, -1, 0, 0};
            int[] dy = {0, 0, 1, -1};

            for (int i = 0; i < 4; i++) {
                int nx = x + dx[i];
                int ny = y + dy[i];

                if (_cyclicFlag) {
                    nx = ((nx % getWidth()) + getWidth()) % getWidth();
                    ny = ((ny % getHeight()) + getHeight()) % getHeight();
                } else {
                    if (nx < 0 || nx >= getWidth() || ny < 0 || ny >= getHeight()) continue;
                }

                if (getPixel(nx, ny) == old) {
                    setPixel(nx, ny, new_v);
                    q.add(new Index2D(nx, ny));
                    count++;
                }
            }
        }
        return count;
    }

	@Override
	/**
	 * BFS like shortest the computation based on iterative raster implementation of BFS, see:
	 * https://en.wikipedia.org/wiki/Breadth-first_search
	 */
    public Pixel2D[] shortestPath(Pixel2D p1, Pixel2D p2, int obsColor) {
        if (p1 == null || p2 == null) return null;
        if (!isInside(p1) || !isInside(p2)) return null;

        if (getPixel(p1) == obsColor || getPixel(p2) == obsColor) return null;

        if (p1.equals(p2)) {
            return new Pixel2D[]{ new Index2D(p1) };
        }

        int w = getWidth(), h = getHeight();
        boolean[][] visited = new boolean[w][h];
        Pixel2D[][] parent = new Pixel2D[w][h];

        Deque<Pixel2D> q = new ArrayDeque<>();
        q.add(new Index2D(p1));
        visited[p1.getX()][p1.getY()] = true;

        while (!q.isEmpty()) {
            Pixel2D cur = q.removeFirst();
            if (cur.equals(p2)) break;

            for (Pixel2D nb : neighbors(cur, _cyclicFlag)) {
                int nx = nb.getX(), ny = nb.getY();
                if (visited[nx][ny]) continue;
                if (getPixel(nx, ny) == obsColor) continue;

                visited[nx][ny] = true;
                parent[nx][ny] = cur;
                q.addLast(nb);
            }
        }

        if (!visited[p2.getX()][p2.getY()]) return null;

        List<Pixel2D> rev = new ArrayList<>();
        Pixel2D cur = p2;
        while (cur != null) {
            rev.add(new Index2D(cur));
            if (cur.equals(p1)) break;
            cur = parent[cur.getX()][cur.getY()];
        }

        Pixel2D[] path = new Pixel2D[rev.size()];
        for (int i = 0; i < rev.size(); i++) {
            path[i] = rev.get(rev.size() - 1 - i);
        }
        return path;
    }

    @Override
    public boolean isInside(Pixel2D p) {
        if (p == null) return false;
        int x = p.getX();
        int y = p.getY();
        return x >= 0 && x < getWidth() && y >= 0 && y < getHeight();
    }


    @Override
    public boolean isCyclic() {
        return _cyclicFlag;
    }

    @Override
    public void setCyclic(boolean cy) {
        _cyclicFlag = cy;
    }


    @Override
    public Map2D allDistance(Pixel2D start, int obsColor) {
        if (start == null) return null;
        if (!isInside(start)) return null;

        int w = getWidth(), h = getHeight();
        Map ans = new Map(w, h, -1);

        if (getPixel(start) == obsColor) return ans;

        boolean[][] visited = new boolean[w][h];
        Deque<Pixel2D> q = new ArrayDeque<>();

        ans.setPixel(start, 0);
        visited[start.getX()][start.getY()] = true;
        q.addLast(new Index2D(start));

        while (!q.isEmpty()) {
            Pixel2D cur = q.removeFirst();
            int curDist = ans.getPixel(cur);

            for (Pixel2D nb : neighbors(cur, _cyclicFlag)) {
                int nx = nb.getX(), ny = nb.getY();
                if (visited[nx][ny]) continue;
                if (getPixel(nx, ny) == obsColor) continue;

                visited[nx][ny] = true;
                ans.setPixel(nx, ny, curDist + 1);
                q.addLast(nb);
            }
        }
        return ans;
    }

    ////////////////////// Private Methods ///////////////////////
    /**
     * Returns all valid neighboring pixels of the given pixel p.
     * A neighbor is defined as a pixel that is one step away in one of the four cardinal directions: up, down, left, or right.
     * Diagonal neighbors are not included.
     * If cyclic is false, only neighbors that are inside the map boundaries are returned.
     * If cyclic is true, the map is treated as cyclic, meaning that neighbors that go beyond one edge wrap around to the opposite edge.
     * The method always returns an array of assignments.Ex3.Pixel2D objects, containing up to four neighbors.
     * @param p
     * @param cyclic
     * @return
     */
    private Pixel2D[] neighbors(Pixel2D p, boolean cyclic) {
        int x = p.getX(), y = p.getY();
        int w = getWidth(), h = getHeight();

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        List<Pixel2D> ans = new ArrayList<>(4);

        for (int i = 0; i < 4; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];

            if (cyclic) {
                if (nx < 0) nx = w - 1;
                if (nx >= w) nx = 0;
                if (ny < 0) ny = h - 1;
                if (ny >= h) ny = 0;
                ans.add(new Index2D(nx, ny));
            } else {
                if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
                    ans.add(new Index2D(nx, ny));
                }
            }
        }
        return ans.toArray(new Pixel2D[0]);
    }
}

