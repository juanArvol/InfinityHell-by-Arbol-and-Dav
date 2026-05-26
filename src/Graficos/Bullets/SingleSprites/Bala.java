package Graficos.Bullets.SingleSprites;

import java.awt.image.BufferedImage;
import Graficos.Loader;

public class Bala {
    private BufferedImage sprite;

    public Bala() {
        sprite = Loader.imageLoader("/Source/bullets/bala.png");
    }

    public BufferedImage getSprite() {
        return sprite;
    }
}