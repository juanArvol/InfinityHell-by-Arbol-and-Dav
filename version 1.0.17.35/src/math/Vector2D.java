package math;

public class Vector2D {
    public double x, y;
    private double angle;
    
    public Vector2D() {
        this.x = 0;
        this.y = 0;
    }

    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector2D add(Vector2D v) {
        return new Vector2D(this.x + v.getX(), this.y + v.getY());
    }

    public Vector2D subtract(Vector2D v) {
        return new Vector2D(this.x - v.getX(), this.y - v.getY());
    }

    public Vector2D scale(double value) {
        return new Vector2D(this.x * value, this.y * value);
    }

    public double getX() {
        return x;
    }
    public double getY() {
        return y;
        }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }

    @Override
    public String toString() {
        return "Vector2D(" + x + ", " + y + ")";
    }
    public double length() {
    return Math.sqrt(x * x + y * y);
}

public Vector2D normalize() {
    double len = length();
    if (len != 0) {
        return new Vector2D(x / len, y / len);
    }
    return new Vector2D(0, 0); 
}
public double magnitude() {
    return Math.sqrt(x * x + y * y);
}
public Vector2D limit(double max) {
    double mag = magnitude();
    if (mag > max) {
        return this.normalize().escale(max);
    }
    return this;
}


public Vector2D escale(double scalar) {
        return new Vector2D(x * scalar, y * scalar);
    }
}