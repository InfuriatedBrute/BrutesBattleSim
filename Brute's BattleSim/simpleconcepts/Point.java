package simpleconcepts;

/**
 * Just a simple class that holds an x-coord and a y-coord, nothing more
 * 
 * @author InfuriatedBrute
 */
public class Point {
	public final int x;
	public final int y;

	public Point(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public int distanceFrom(Point p) {
			return Math.abs(this.x-p.x) + Math.abs(this.y-p.y);
	}
}
