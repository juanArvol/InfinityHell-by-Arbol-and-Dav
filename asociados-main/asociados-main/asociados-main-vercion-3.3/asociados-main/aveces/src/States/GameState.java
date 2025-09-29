package States;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.JPanel;

import Game.Ambiente;
import Game.CollisionManager;
import Game.EnimyNormal;
import Game.GameObjects;
import Game.Player;
import graficos.Assets;
import math.Vector2D;

public class GameState extends JPanel {
    private Player player;
    private Ambiente ambiente;
    private ArrayList<GameObjects> objects;
    private ArrayList<EnimyNormal> enemies;
    private int enemyCount = 1;

    public GameState() {
   
    player = new Player(new Vector2D(50.0, 190), Assets.cubo);
    objects = new ArrayList<>();
    enemies = new ArrayList<>();
    spawnEnemies(3);
    objects.add(player);

    // crer suelo
    BufferedImage texturaSuelo = Assets.suelo;
    int anchoPantalla = 1900; // o el ancho real de tu ventana
    int ySuelo = 485;


    ambiente = new Ambiente(new Vector2D(0, ySuelo), texturaSuelo, anchoPantalla, 40);
    objects.add(ambiente);

  

 
    }

    public void spawnEnemies(int count) {
        for (int i = 0; i < count; i++) {
        double x = Math.random() * 800; 
        double y = Math.random() * 600; 
        
        ArrayList<Vector2D> path = new ArrayList<>();
        for (int j = 0 ; j<5 ; j++) {
        double pox = Math.random() * 800; 
        double poy = Math.random() * 600; 
        path.add(new Vector2D(pox, poy));
        }
        BufferedImage texture = Assets.enimy1[(int) (Math.random() * Assets.enimy1.length)];
        EnimyNormal enemy = new EnimyNormal(new Vector2D(x, y), texture, path, 0, true, player);
        enemies.add(enemy);
        objects.add(enemy); 
    }
    }

public void update() {
    for (GameObjects obj : objects) {
        obj.update();
    }

    CollisionManager.checkColicion(objects);
}

    public void draw(Graphics g) {
    for (GameObjects obj : objects) {
        obj.draw(g);
    }
}
    }
