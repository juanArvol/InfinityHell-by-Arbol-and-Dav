package graficos;

import java.awt.image.BufferedImage;

public class Assets {
    public static BufferedImage mondongo;
    public static BufferedImage cubo;
    public static BufferedImage suelo;
    public static BufferedImage bala;
    public static BufferedImage[] walkDere;
    public static BufferedImage[] walkHiz;

    public static BufferedImage[] enimy1;

    public static void init() {
        mondongo= Loader.imageLoader("/Source/mondongo.png");
        cubo = Loader.imageLoader("/Source/player/eee.png");
        suelo = Loader.imageLoader("/Source/ambiente/pasto.png");
        
        walkDere = new BufferedImage[] {
            Loader.imageLoader("/Source/player/dereuno.png"),
            Loader.imageLoader("/Source/player/deredos.png"),
        };
        walkHiz = new BufferedImage[] {
            Loader.imageLoader("/Source/player/ladoHizquierdo.png"),
            Loader.imageLoader("/Source/player/ladoHizquierd1o.png"),
            Loader.imageLoader("/Source/player/ladoHizquierd2o.png"),
        };
        
        bala = Loader.imageLoader("/Source/bullets/bala.png");
        
        // Caso: primer archivo sin número
        enimy1 = new BufferedImage[4];
        enimy1[0] = Loader.imageLoader("/Source/enemies/zotopia1.png");
        for (int i = 1; i < enimy1.length; i++) {
            enimy1[i] = Loader.imageLoader("/Source/enemies/zotopia" + (i + 1) + ".png");
        }
    }
}
