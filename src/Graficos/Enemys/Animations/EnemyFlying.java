package Graficos.Enemys.Animations;

import java.awt.image.BufferedImage;
import Graficos.Loader;

public class EnemyFlying {
    private BufferedImage[] frames;

    public EnemyFlying() {
        frames = new BufferedImage[4];
        frames[0] = Loader.imageLoader("/Source/gato.jpg");
        for (int i = 1; i < frames.length; i++) {
            frames[i] = Loader.imageLoader("/Source/gato.jpg");
        }
    }

    public BufferedImage[] getFrames() {
        return frames;
    }
}
