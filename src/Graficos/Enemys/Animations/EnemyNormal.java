package Graficos.Enemys.Animations;

import java.awt.image.BufferedImage;
import Graficos.Loader;

public class EnemyNormal {
    private BufferedImage[] frames;

    public EnemyNormal() {
        frames = new BufferedImage[4];
        frames[0] = Loader.imageLoader("/Source/enemies/zotopia1.png");
        for (int i = 1; i < frames.length; i++) {
            frames[i] = Loader.imageLoader("/Source/enemies/zotopia" + (i + 1) + ".png");
        }
    }

    public BufferedImage[] getFrames() {
        return frames;
    }
}