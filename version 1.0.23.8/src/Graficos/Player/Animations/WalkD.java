package Graficos.Player.Animations;

import java.awt.image.BufferedImage;
import Graficos.Loader;

public class WalkD {
    private BufferedImage[] frames;

    public WalkD() {
        frames = new BufferedImage[] {
            Loader.imageLoader("/Source/player/dereuno.png"),
            Loader.imageLoader("/Source/player/deredos.png"),
        };
    }

    public BufferedImage[] getFrames() {
        return frames;
    }
}