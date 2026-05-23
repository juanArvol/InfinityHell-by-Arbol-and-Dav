package Graficos.Player.Animations;

import java.awt.image.BufferedImage;
import Graficos.Loader;

public class WalkIzq {
    private BufferedImage[] frames;

    public WalkIzq() {
        frames = new BufferedImage[] {
            Loader.imageLoader("/Source/player/ladoHizquierdo.png"),
            Loader.imageLoader("/Source/player/ladoHizquierd1o.png"),
            Loader.imageLoader("/Source/player/ladoHizquierd2o.png"),
        };
    }

    public BufferedImage[] getFrames() {
        return frames;
    }
}