package Game.Items.Types.Bullets.BulletComport.BulletClass;

import Game.Enemys.Core.Enemy;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Player.Player;
import Game.World.WorldObjects.Visuals.BackGround;

public class BulletJump extends BulletBehavior {

    private final double jumpBoost = -10; // velocidad Y al rebotar (negativo = hacia arriba)

    public BulletJump() {
        super(5, 0.9, true,1,20); // velocidad=10, daño=15, gravedad=true
    }

    @Override
    public void update(Bullet bullet) {
        // opcional: aquí podrías añadir aceleración o lógica extra por frame
    }

    @Override
    public void onCollision(Bullet bullet, Enemy enemy) {
        bullet.getBulletLife().setDead(); // destruye bala al chocar con enemigo
    }

    @Override
    public void onCollision(Bullet bullet, Player player) {
        // opcional: afectar al jugador
    }

    @Override
    public void onCollision(Bullet bullet, BackGround ambiente) {
        // Si colisiona con el piso, rebota
        if (bullet.getPhysics().getYspeed() > 0) { // si iba hacia abajo
            bullet.getPhysics().setYspeed(jumpBoost); // lo "lanza" hacia arriba
            bullet.getBulletLife().reset(1);
        }
            bullet.getPhysics().setXspeed(bullet.getPhysics().getXspeed()/1.01);
    }
} 