package graficos;

import java.awt.image.BufferedImage;

public class Assets {
    public static BufferedImage cubo;
    public static BufferedImage suelo;
    public static BufferedImage bala;
    public static BufferedImage[] walkDere;
    public static BufferedImage[] walkHiz;

    public static BufferedImage[] enimy1;

    public static void init() {
        cubo = Loader.imageLoader("/recursos/eee.png");
        suelo = Loader.imageLoader("/recursos/pasto.png");
        
        walkDere = new BufferedImage[] {
            Loader.imageLoader("/recursos/dereuno.png"),
            Loader.imageLoader("/recursos/deredos.png"),
         

        };
        walkHiz = new BufferedImage[] {
            Loader.imageLoader("/recursos/ladoHizquierdo.png"),    
            Loader.imageLoader("/recursos/ladoHizquierd1o.png"),
            Loader.imageLoader("/recursos/ladoHizquierd2o.png"),       
        };
        
        bala = Loader.imageLoader("/recursos/bala.png");
        
        // Caso: primer archivo sin número
        enimy1 = new BufferedImage[4];
        enimy1[0] = Loader.imageLoader("/recursos/zotopia1.png");
        for (int i = 1; i < enimy1.length; i++) {
            enimy1[i] = Loader.imageLoader("/recursos/zotopia" + (i + 1) + ".png");
        }
    }
}
