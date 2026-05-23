package Graficos.Obstacles.SingleSprites;

import java.awt.image.BufferedImage;
import Graficos.Loader;

public class Mondongo {
    private BufferedImage sprite;

    public Mondongo() {
        sprite = Loader.imageLoader("/Source/mondongo.png");
    }
    public BufferedImage getSprite() {
        return sprite;
    }
}