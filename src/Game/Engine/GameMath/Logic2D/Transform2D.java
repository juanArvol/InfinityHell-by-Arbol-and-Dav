package Game.Engine.GameMath.Logic2D;

public class Transform2D {
    private Vector2D position = new Vector2D();
    private double rotation = 0;
    public Vector2D getPosition() { return position; }
    public void setPosition(Vector2D pos){ this.position = pos; }

    public void setX(double x) { position.setX(x); }
    public void setY(double y) { position.setY(y); }

    public double getX(){ return position.getX(); }
    public double getY(){ return position.getY(); }

    public double getRotation(){ return rotation; }
    public void setRotation(double r){ rotation = r; }
}