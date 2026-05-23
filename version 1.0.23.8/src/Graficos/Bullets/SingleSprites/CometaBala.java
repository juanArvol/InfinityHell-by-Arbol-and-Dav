package Graficos.Bullets.SingleSprites;

import java.awt.image.BufferedImage;
import Graficos.Loader;

public class CometaBala {
    private BufferedImage sprite;

    public CometaBala() {
        sprite = Loader.imageLoader("/Source/bullets/cometa.png");
    }

    public BufferedImage getSprite() {
        return sprite;
    }
}