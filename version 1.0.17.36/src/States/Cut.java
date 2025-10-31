package States;


import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import Game.GameObjects;
import Game.Player;
import math.Vector2D;


public class Cut extends GameObjects {
    private Player player;
    private int ancho; 
    private int alto; 
    private double displayedVidaWidth = -1;//esto, esto papa es
    private double shrinkSpeed = 8.0;// ye esto es la velovidad de el fotograma  es decir pixeles por fotograma
    public  Cut (Vector2D position, BufferedImage texture, int ancho, int alto, Player player){
      super(position, texture);
      this.ancho = ancho;
      this.player = player;
    }

    @Override
    public void update() {
    int kife = player.getLife();
    int lifeMax = player.getLifeMax();
    int whithBrra = 400; 

    // tonces pille, inicializamos en el primer fotograma
    if(displayedVidaWidth < 0){
        displayedVidaWidth = (double) kife /  (double) lifeMax * whithBrra;
    }
    double targetWidth = ((double) kife / (double) lifeMax) * whithBrra;
    if (displayedVidaWidth > targetWidth) {
    // esto nos reducira d  manera un poco mas suave la barra
    displayedVidaWidth -= shrinkSpeed;
    if (displayedVidaWidth < targetWidth) displayedVidaWidth = targetWidth;
    } else if (displayedVidaWidth < targetWidth) {
    // con esto crece de forma gradual
    displayedVidaWidth += shrinkSpeed;
    if (displayedVidaWidth > targetWidth) displayedVidaWidth = targetWidth;
    }
    }
    
    @Override
    public void draw(Graphics g) {     
         int life = player.getLife();
        int vidaMax = player.getLifeMax();

        int barWidth = 200;  
        int barHeight = 20; 
        int x = 85;         
        int y = 20;

        // borde
        g.setColor(java.awt.Color.BLACK);
        g.drawRect(x, y, barWidth, barHeight);

        // relleno proporcional a la vida
        
        int vidaWidth = (int) Math.round(displayedVidaWidth >= 0 ? displayedVidaWidth : ((life/ (double) vidaMax) * barWidth));

        // color  segune el porcentage de vida
        double percent = vidaMax > 0 ? (life / (double) vidaMax) : 0.0;
        if (percent > 0.6) {
            g.setColor(java.awt.Color.RED);
        } else if (percent > 0.3) {
            g.setColor(java.awt.Color.RED);
        } else {
            g.setColor(java.awt.Color.RED);
        }
        g.fillRect(x + 1, y + 1, Math.max(0, vidaWidth ), Math.max(0, barHeight - 1));
        
      
        // número en consola
        g.drawImage(texture, (int) position.getX(), (int) position.getY(), ancho, 20, null);
        System.out.println("Vida Player: " + life + "/" + vidaMax);
        g.drawImage(texture, (int) position.getX(), (int) position.getY(), ancho, 100, null);
       

       
    }
    @Override
    public Rectangle getBounds() {
        return new Rectangle((int) 0, 0, 0, 0);
    }
}
