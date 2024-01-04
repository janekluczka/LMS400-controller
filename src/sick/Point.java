package sick;

public class Point {

    private final double x;
    private final double y;
    private final double distance;
    private final double angle;

    public Point(int distance, int angle) {
        this.distance = distance;
        this.angle = ((double) angle) / 10000;

        double angleRad = Math.toRadians(this.angle);

        this.x = distance * Math.cos(angleRad);
        this.y = distance * Math.sin(angleRad);
    }

    public void printInfo() {
        System.out.println("POINT");
        System.out.println("POLAR: (" + this.distance + "," + this.angle + ")");
        System.out.println("CARTESIAN: (" + this.x + "," + this.y + ")");
    }

    public double getDistance() {
        return this.distance;
    }

    public double getAngle() {
        return this.angle;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }
}