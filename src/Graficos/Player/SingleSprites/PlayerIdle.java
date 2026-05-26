package Graficos.Player.SingleSprites;

import java.awt.image.BufferedImage;
import Graficos.Loader;

public class PlayerIdle {
    private BufferedImage sprite;

    public PlayerIdle() {
        sprite = Loader.imageLoader("/Source/player/eee.png");
    }

    public BufferedImage getSprite() {
        return sprite;
    }
}