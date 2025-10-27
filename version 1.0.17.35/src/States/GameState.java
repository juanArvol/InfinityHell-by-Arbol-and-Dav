package States;

import Game.Ambiente;
import Game.Colisions.CollisionManager;
import Game.EnimyNormal;
import Game.GameObjects;
import Game.Player;
import Game.WeaponSelected;
import graficos.Assets;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.JPanel;
import math.Vector2D;

public class GameState extends JPanel {

    private Player player;
    private Ambiente ambiente;
    public static GameState instance;
    private ArrayList<GameObjects> objects;            // Objetos activos en el juego
    private ArrayList<GameObjects> pendingAdditions;   // Objetos por agregar (cola segura)
    private ArrayList<GameObjects> pendingRemovals;    // Objetos por eliminar
    private ArrayList<EnimyNormal> enemies;
    
    public GameState() {
        instance = this;
        objects = new ArrayList<>();
        pendingAdditions = new ArrayList<>();
        pendingRemovals = new ArrayList<>();
        enemies = new ArrayList<>();
        player = new Player(new Vector2D(50.0, 190), Assets.cubo, this);
        spawnEnemies(0);
        player.setEnemies(enemies);
        player.setWeapon(new WeaponSelected(player, 1,3));
        objects.add(player);
        
        // crear suelo
        BufferedImage texturaSuelo = Assets.suelo;
        int anchoPantalla = 1200;
        int ySuelo = 485;

        ambiente = new Ambiente(new Vector2D(0, ySuelo), texturaSuelo, anchoPantalla, 75);
        objects.add(ambiente);
    }

    // Método seguro para agregar objetos (por ejemplo, balas)
    public void addGameObject(GameObjects obj) {
        pendingAdditions.add(obj);
    }

    // Método seguro para eliminar objetos (opcional)
    public void removeGameObject(GameObjects obj) {
        pendingRemovals.add(obj);
    }
    public ArrayList<GameObjects> getObjects() {
        return objects;
    }
    public void spawnEnemies(int count) {
        for (int i = 0; i < count; i++) {
            double x = Math.random() * 800;
            double y = Math.random() * 600;

            ArrayList<Vector2D> path = new ArrayList<>();
            for (int j = 0; j < 5; j++) {
                double pox = Math.random() * 800;
                double poy = Math.random() * 600;
                path.add(new Vector2D(pox, poy));
            }

            BufferedImage texture = Assets.enimy1[(int) (Math.random() * Assets.enimy1.length)];
            EnimyNormal enemy = new EnimyNormal(new Vector2D(x, y), texture, path, 0, false, player);
            enemies.add(enemy);
            objects.add(enemy);
        }
    }

    public void update() {
        // Actualiza todos los objetos activos
        for (GameObjects obj : objects) {
            obj.update();
        }

        // Maneja colisiones
        CollisionManager.checkColicion(objects);

        // Aplica las adiciones pendientes (para evitar congelamientos)
        if (!pendingAdditions.isEmpty()) {
            objects.addAll(pendingAdditions);
            pendingAdditions.clear();
        }

        // Aplica las eliminaciones pendientes
        if (!pendingRemovals.isEmpty()) {
            objects.removeAll(pendingRemovals);
            pendingRemovals.clear();
        }
    }

    public void draw(Graphics g) {
        for (GameObjects obj : objects) {
            obj.draw(g);
        }
    }
}
