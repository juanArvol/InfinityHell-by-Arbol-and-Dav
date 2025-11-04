package graficos;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Loader {
    //esto me va acargar metodos estaticos osea
    //manera en que java guarda imagenes en memor
public static BufferedImage imageLoader(String path){
try {
    return ImageIO.read(Loader.class.getResource(path));
} catch (IOException e) {
    e.printStackTrace();
}
return null;
}
}
