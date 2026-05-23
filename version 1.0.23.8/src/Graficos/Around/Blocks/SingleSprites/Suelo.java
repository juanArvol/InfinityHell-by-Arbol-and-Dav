package Graficos.Around.Blocks.SingleSprites;

import java.awt.image.BufferedImage;
import Graficos.Loader;

public class Suelo {
    private BufferedImage sprite;

    public Suelo() {
        sprite = Loader.imageLoader("/Source/ambiente/pasto.png");
    }

    public BufferedImage getSprite() {
        return sprite;
    }
}