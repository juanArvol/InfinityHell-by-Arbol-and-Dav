package Game.Items.Types.Bullets.BulletComport.BulletClass;

import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.Definition.ProjectileContext;
import Game.Items.Types.Bullets.Movement.GravityMovement;
import Game.Items.Types.Bullets.Definition.ProjectileData;
import Game.Items.Types.Bullets.ProjectileMovement;

/**
 * Behavior del MetheorBullet — proyectil de alta masa con explosión al impacto.
 *
 * ── HRFC — Consolidación y Limpieza de Legacy (MetheorBullet Migration) ───
 *
 * Este behavior reconstruye conceptualmente el MetheorBullet del sistema legacy,
 * adaptándolo a la arquitectura actual de proyectiles.
 *
 * ── COMPORTAMIENTO ────────────────────────────────────────────────────────
 *
 * Proyectil pesado que:
 *   - Tiene gravedad (composición via GravityMovement)
 *   - Explota al impactar enemigos o terreno
 *   - Genera daño en área escalado con velocidad de caída
 *   - Empuje radial a entities cercanas
 *
 * ── MIGRACIÓN DESDE SISTEMA LEGACY ────────────────────────────────────────
 *
 * Sistema antiguo:
 *   - onCollisionWith(Player/EnimyNormal/Ambiente) — manual dispatch
 *   - Acceso directo a player.getEnemies()
 *   - Mutación directa de posiciones de entities
 *
 * Sistema actual:
 *   - onCollision(bullet, hitEntity) — polimorfismo via BulletBehavior
 *   - ProjectileContext.applyAreaEffect() — explosión en área sin conocer entities
 *   - Engine de física maneja empuje via fuerzas, no mutación directa
 *
 * ── ESCALADO DE EXPLOSIÓN ─────────────────────────────────────────────────
 *
 * La potencia de explosión escala con la velocidad de caída:
 *
 *   explosionPower = |velocityY| * 2.3
 *   maxRadius = 250 + (explosionPower * 1.5)
 *   damage = baseDamage + (explosionPower * (1 - distance/maxRadius))
 *
 * Mientras más cae, más destrucción causa. Fidelidad conceptual al diseño original.
 *
 * ── DIFERENCIAS CON IMPLEMENTACIÓN LEGACY ─────────────────────────────────
 *
 * 1. ProjectileContext abstrae el acceso al mundo (no conoce Player directamente)
 * 2. Explosión implementada via applyAreaEffect (delegación al Engine)
 * 3. No hay dependencia de EnimyNormal ni Ambiente (usa CollisionProfile)
 * 4. Movement composition (GravityMovement) en lugar de hasGravity() flag
 *
 * ── REGISTRO EN BulletType ────────────────────────────────────────────────
 *
 * Para activar en el juego, añadir a BulletType.java:
 *
 *   VOIDMETEOR (MetheorBullet::new, ItemRarity.RARE,
 *               "Meteoro del Vacío",
 *               "Proyectil de alta masa que genera explosiones devastadoras."),
 *
 * ── CÓDIGO LEGACY ORIGINAL (COMENTADO PARA REFERENCIA) ────────────────────
 *
 * /* package Game.Bullets.BulletCharger.BulletClass;

import Game.Ambiente;
import Game.Bullets.Bullet;
import Game.Bullets.BulletCharger.BulletClass.BulletClassUpdater.BulletOnUpdate;
import Game.Bullets.BulletCharger.BulletComport;
import Game.Colisions.SystemColisions.Collidable;
import Game.EnimyNormal;
import Game.GameObjects;
import Game.Player;

public class MetheorBullet extends BulletComport {
    private EnimyNormal enemy;
    private Player player;
    private Ambiente ambiente;
    @Override
    public double getBspeed() { return 100; }

    @Override
    public boolean hasGravity() { return true; }

    @Override
    public int getDamage() { return 10; }

    @Override
    public void onUpdate(Bullet bullet, GameObjects algo) {
        switch (algo) {
            case Player p -> onUpdateWith(bullet, p);
            case EnimyNormal e -> onUpdateWith(bullet,e);
            case Bullet b -> onUpdateWith(bullet,b);
            case Ambiente a -> onUpdateWith(bullet,a);
            default -> onUpdateWith((Bullet)bullet, (Bullet) bullet);
        }
    }
    public void onUpdateWith(Bullet bullet, GameObjects algo){
        BulletOnUpdate.bulletOnUpdate(bullet, algo);
    }
    //Recibe el tipo de colision
    @Override
    public void onCollision(Bullet bullet, GameObjects algo) {
        acceptCollision(algo);                      //colision unitaria
        bulletAcceptCollisionWith(bullet, algo);    //colision doble
    }
    
    //aceptacion de la colision
    @Override
    public void acceptCollision(Collidable other) {
        super.acceptCollision(other);               //envia y define la colision con el other
    }
    @Override
    public void bulletAcceptCollisionWith(Bullet b, Collidable other) {
        super.bulletAcceptCollisionWith(b, other);  //envia y define la colision doble entre bala y other (que pasa con ambos)
    }

    //COLISION UNITARIA
    @Override
    public void onCollisionWith(Player player) {
        //System.out.println("bala colisiono con jugador");
    }
    @Override
    public void onCollisionWith(EnimyNormal enemy) {
    }

    @Override
    public void onCollisionWith(Bullet bullet) {
    }
    @Override
    public void onCollisionWith(Ambiente ambiente) {
        //System.out.println("aAaaaAAaaaa");
    }
    
    //COLISION DOBLE
    @Override
    public void bulletOnCollisionWith(Bullet b, Player player) {
        //System.out.println("david e puto");
    }
    @Override
    public void bulletOnCollisionWith(Bullet b, EnimyNormal enemy) {
        explode(b);
        //System.out.println("posicion del enemy: "+ enemy.getEnemyPosition());
    }
    @Override
    public void bulletOnCollisionWith(Bullet b, Ambiente ambiente) {
        explode(b);
    }
    @Override
    public void setGameObject(GameObjects algo){
        if(algo instanceof EnimyNormal e){
            this.enemy=e;
        }
        if(algo instanceof Player p){
            this.player=p;
        }
        if(algo instanceof Ambiente a){
            this.ambiente=a;
        }
    }
    private void explode(Bullet b){
        double baseDamage = 35; // daño base
        double explosionPower = Math.abs(b.getBphysics().getVelocity().getY())*2.3 ; // mientras más caiga, más rompe
        double maxRadius = 250 + (explosionPower*1.5); // el rango depende de la velocidad
        double centerX = b.getPosition().getX();
        double centerY = b.getPosition().getY();
        double force=1;

        for (EnimyNormal e : player.getEnemies()) {
            double dx = e.getPosition().getX() - centerX;
            double dy = e.getPosition().getY() - centerY;
            double distance = Math.sqrt(dx*dx + dy*dy);
        
        // Evitamos operar entre 0 por si el enemigo está exactamente en el centro
        if (distance == 0) distance = 1;

        if (distance <= maxRadius) {
            force = (maxRadius - distance) * 0.1; // más cerca = más empuje
        }

        // Calculamos fuerza SOLO en los ejes necesarios
            double pushX = 0;
            double pushY = 0;
        
            // Si hay distancia real en X, empujamos en X
            if (Math.abs(dx) > 1) {
                pushX = (dx / distance) * force;
            }

            // Si hay distancia real en Y, empujamos en Y
            if (Math.abs(dy) > 1) {
                pushY = (dy / distance) * force;
            }

            e.getEnemyPosition().setX(e.getEnemyPosition().getX() + pushX);
            e.getEnemyPosition().setY(e.getEnemyPosition().getY() + pushY);

            player.getPosition().setX(player.getPosition().getX() + pushX);
            player.getPosition().setY(player.getPosition().getY() + pushY);

            double damage = baseDamage + (explosionPower * (1 - distance / maxRadius));
            System.out.println("Enemy recibió " + (int)damage + " daño por explosión");
            System.out.println("Enemy empujado con fuerza: " + force);
        }
    }
}
 */

import Game.Ambiente;
import Game.Bullets.Bullet;
import Game.Bullets.BulletCharger.BulletClass.BulletClassUpdater.BulletOnUpdate;
import Game.Bullets.BulletCharger.BulletComport;
import Game.Colisions.SystemColisions.Collidable;
import Game.EnimyNormal;
import Game.GameObjects;
import Game.Player;

public class MetheorBullet extends BulletComport {
    private EnimyNormal enemy;
    private Player player;
    private Ambiente ambiente;
    @Override
    public double getBspeed() { return 100; }

    @Override
    public boolean hasGravity() { return true; }

    @Override
    public int getDamage() { return 10; }

    @Override
    public void onUpdate(Bullet bullet, GameObjects algo) {
        switch (algo) {
            case Player p -> onUpdateWith(bullet, p);
            case EnimyNormal e -> onUpdateWith(bullet,e);
            case Bullet b -> onUpdateWith(bullet,b);
            case Ambiente a -> onUpdateWith(bullet,a);
            default -> onUpdateWith((Bullet)bullet, (Bullet) bullet);
        }
    }
    public void onUpdateWith(Bullet bullet, GameObjects algo){
        BulletOnUpdate.bulletOnUpdate(bullet, algo);
    }
    //Recibe el tipo de colision
    @Override
    public void onCollision(Bullet bullet, GameObjects algo) {
        acceptCollision(algo);                      //colision unitaria
        bulletAcceptCollisionWith(bullet, algo);    //colision doble
    }
    
    //aceptacion de la colision
    @Override
    public void acceptCollision(Collidable other) {
        super.acceptCollision(other);               //envia y define la colision con el other
    }
    @Override
    public void bulletAcceptCollisionWith(Bullet b, Collidable other) {
        super.bulletAcceptCollisionWith(b, other);  //envia y define la colision doble entre bala y other (que pasa con ambos)
    }

    //COLISION UNITARIA
    @Override
    public void onCollisionWith(Player player) {
        //System.out.println("bala colisiono con jugador");
    }
    @Override
    public void onCollisionWith(EnimyNormal enemy) {
    }

    @Override
    public void onCollisionWith(Bullet bullet) {
    }
    @Override
    public void onCollisionWith(Ambiente ambiente) {
        //System.out.println("aAaaaAAaaaa");
    }
    
    //COLISION DOBLE
    @Override
    public void bulletOnCollisionWith(Bullet b, Player player) {
        //System.out.println("david e puto");
    }
    @Override
    public void bulletOnCollisionWith(Bullet b, EnimyNormal enemy) {
        explode(b);
        //System.out.println("posicion del enemy: "+ enemy.getEnemyPosition());
    }
    @Override
    public void bulletOnCollisionWith(Bullet b, Ambiente ambiente) {
        explode(b);
    }
    @Override
    public void setGameObject(GameObjects algo){
        if(algo instanceof EnimyNormal e){
            this.enemy=e;
        }
        if(algo instanceof Player p){
            this.player=p;
        }
        if(algo instanceof Ambiente a){
            this.ambiente=a;
        }
    }
    private void explode(Bullet b){
        double baseDamage = 35; // daño base
        double explosionPower = Math.abs(b.getBphysics().getVelocity().getY())*2.3 ; // mientras más caiga, más rompe
        double maxRadius = 250 + (explosionPower*1.5); // el rango depende de la velocidad
        double centerX = b.getPosition().getX();
        double centerY = b.getPosition().getY();
        double force=1;

        for (EnimyNormal e : player.getEnemies()) {
            double dx = e.getPosition().getX() - centerX;
            double dy = e.getPosition().getY() - centerY;
            double distance = Math.sqrt(dx*dx + dy*dy);
        
        // Evitamos operar entre 0 por si el enemigo está exactamente en el centro
        if (distance == 0) distance = 1;

        if (distance <= maxRadius) {
            force = (maxRadius - distance) * 0.1; // más cerca = más empuje
        }

        // Calculamos fuerza SOLO en los ejes necesarios
            double pushX = 0;
            double pushY = 0;
        
            // Si hay distancia real en X, empujamos en X
            if (Math.abs(dx) > 1) {
                pushX = (dx / distance) * force;
            }

            // Si hay distancia real en Y, empujamos en Y
            if (Math.abs(dy) > 1) {
                pushY = (dy / distance) * force;
            }

            e.getEnemyPosition().setX(e.getEnemyPosition().getX() + pushX);
            e.getEnemyPosition().setY(e.getEnemyPosition().getY() + pushY);

            player.getPosition().setX(player.getPosition().getX() + pushX);
            player.getPosition().setY(player.getPosition().getY() + pushY);

            double damage = baseDamage + (explosionPower * (1 - distance / maxRadius));
            System.out.println("Enemy recibió " + (int)damage + " daño por explosión");
            System.out.println("Enemy empujado con fuerza: " + force);
        }
    }
}
 */
 
/**
 * ── IMPLEMENTACIÓN ACTUAL (MIGRADA) ───────────────────────────────────────
 */
public class MetheorBullet extends BulletBehavior {

    private static final double BASE_DAMAGE         = 35.0;
    private static final double BASE_SPEED          = 100.0;
    private static final double GRAVITY_STRENGTH    = 0.5;  // intensidad de gravedad
    private static final double EXPLOSION_POWER_MULT = 2.3;
    private static final double RADIUS_BASE         = 250.0;
    private static final double RADIUS_SCALE        = 1.5;
    private static final int    DEFAULT_LIFETIME    = 300;  // 5 segundos a 60fps

    @Override
    public String getName() {
        return "Meteor";
    }

    @Override
    public ProjectileData getDefaultData() {
        return new ProjectileData(
                1.0,        // speedFactor (base speed viene de WeaponStats o BulletType)
                10.0,       // damage directo (explosión usa BASE_DAMAGE)
                DEFAULT_LIFETIME,
                "void_meteor" // assetKey
        );
    }

    @Override
    public ProjectileMovement getMovement() {
        // GravityMovement se compondrá con el movimiento lineal base del proyectil
        return new GravityMovement(GRAVITY_STRENGTH);
    }

    @Override
    public void onCollision(Bullet bullet, Object hitEntity) {
        // Detectar colisión con enemigos o terreno
        CollisionProfile profile = bullet.getCollisionProfile();
        
        // MetheorBullet explota al impactar enemigos o ambiente (terreno)
        boolean shouldExplode = false;
        
        if (hitEntity != null) {
            // Verificar si impactó algo sólido (enemigo o terreno)
            // El CollisionProfile ENEMY_BULLET colisiona con: Player, Terrain, Shields
            // Asumimos que cualquier colisión de un proyectil enemigo es válida para explosión
            shouldExplode = true;
        }
        
        if (shouldExplode) {
            explode(bullet);
        }
        
        // Matar el proyectil tras la explosión
        bullet.getBulletLife().kill();
    }

    /**
     * Genera la explosión en área con daño escalado por velocidad de caída.
     */
    private void explode(Bullet bullet) {
        // Obtener contexto del mundo
        ProjectileContext context = bullet.getContext();
        if (context == null) {
            // Sin contexto, no se puede generar explosión en área
            // (esto solo ocurre en tests sin ProjectileContext inyectado)
            return;
        }

        // Calcular potencia de explosión basada en velocidad de caída
        double velocityY = bullet.getBphysics().getVelocity().getY();
        double explosionPower = Math.abs(velocityY) * EXPLOSION_POWER_MULT;
        
        // Radio de explosión escala con la potencia
        double maxRadius = RADIUS_BASE + (explosionPower * RADIUS_SCALE);
        
        // Posición del impacto (epicentro)
        double centerX = bullet.getPosition().getX();
        double centerY = bullet.getPosition().getY();
        
        // Aplicar daño en área via ProjectileContext
        // El Engine se encarga de iterar entities cercanas y aplicar daño/empuje
        context.applyAreaEffect(
                centerX, centerY, maxRadius,
                (entity, distance) -> {
                    // Calcular daño escalado con distancia
                    double damageFactor = 1.0 - (distance / maxRadius);
                    double finalDamage = BASE_DAMAGE + (explosionPower * damageFactor);
                    
                    // Aplicar daño a la entity
                    // El Engine maneja este damage() internamente
                    return finalDamage;
                }
        );
        
        // Nota: El empuje radial se maneja automáticamente por el Engine
        // basado en la distancia y el damageFactor retornado por el callback
    }

    @Override
    public void onExpire(Bullet bullet) {
        // MetheorBullet no explota al expirar (solo al impactar)
        // Simplemente desaparece si no impacta nada
    }
}
