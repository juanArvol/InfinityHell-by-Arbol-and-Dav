package Game.Bullets.BulletCharger.BulletClass.BulletClassUpdater;

import Game.Ambiente;
import Game.Bullets.Bullet;
import Game.EnimyNormal;
import Game.GameObjects;
import Game.Player;

public class BulletOnUpdate {
    public static void bulletOnUpdate(Bullet bullet, GameObjects other){
        switch (bullet.getTipo().getNumberTipo()) {
            case 1 -> {
                // Bala normal
                switch (other) {
                    case Player p -> onUpdateWith(bullet, p);
                    case EnimyNormal e -> onUpdateWith(bullet,e);
                    case Bullet b -> onUpdateWith(bullet,b);
                    case Ambiente a -> onUpdateWith(bullet,a);
                    default -> onUpdateWith((Bullet)bullet, (Bullet) bullet);
                }
            }
            case 2 -> {
                // bala saltarina
                switch (other) {
                    case Bullet b -> onUpdateWith(bullet,b);
                    default -> onUpdateWith((Bullet)bullet, (Bullet) bullet);
                }
            }
            case 3 -> {
                // bala tierra
                switch (other) {
                    case Bullet b -> onUpdateWith(bullet,b);
                    default -> onUpdateWith((Bullet)bullet, (Bullet) bullet);
                }
            }
            case 4 -> {
                // bala meteoro
                switch (other) {
                    case Player p -> onUpdateWith(bullet, p);
                    case EnimyNormal e -> onUpdateWith(bullet,e);
                    case Bullet b -> onUpdateWith(bullet,b);
                    case Ambiente a -> onUpdateWith(bullet,a);
                    default -> onUpdateWith((Bullet)bullet, (Bullet) bullet);
                }
            }
            default -> {
            }
        }
    }
    public static void onUpdateWith(Bullet b, GameObjects obj){
        if (obj instanceof Player) {
            b.getTipo().getBulletClass().setGameObject(obj);
        }
        if (obj instanceof Ambiente) {
            b.getTipo().getBulletClass().setGameObject(obj);
        }
        if (obj instanceof EnimyNormal) {
            b.getTipo().getBulletClass().setGameObject(obj);
        }
        if (obj instanceof Bullet) {
            b.getTipo().getBulletClass().setGameObject(obj);
        }
    }
}
