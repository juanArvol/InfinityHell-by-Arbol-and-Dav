package Game.Engine;

public abstract class Component {

    protected GameObjects gameObject;

    void setGameObject(GameObjects obj){
        this.gameObject = obj;
    }

    public void start() {}
    public void update() {}
    public void scale(double scaleX, double scaleY){

    }
}