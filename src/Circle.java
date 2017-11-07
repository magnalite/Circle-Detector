
public class Circle	{
	public int x, y, r;
	
	public Circle(int x, int y, int r) {
		this.x = x;
		this.y = y;
		this.r = r;
	}
	
	public boolean isInside(Circle c) {
		if (Math.sqrt((x - c.x)*(x - c.x) + (y-c.y) * (y-c.y)) < c.r) {
			return true;
		} else {
			return false;
		}
	}
}
