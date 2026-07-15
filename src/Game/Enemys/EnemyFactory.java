package Game.Enemys;

import Game.Enemys.Types.EnemyType;
import Game.Enemys.Types.Flying.Class.EnemyFlying;
import Game.Enemys.Types.Ground.Class.EnemyNormal;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Sprites.Enemys.EnemyAssets;
import Sprites.Core.SpriteHandle;

import java.awt.image.BufferedImage;

/**
 * Fábrica de enemigos.
 *
 * ── HRFC-002: MIGRACIÓN DE ASSETS ─────────────────────────────────────────
 *
 * ANTES:
 *   - getRandomTexture() accedía a EnemyAssets.Enormal.getFrames() y
 *     EnemyAssets.Eflying.getFrames() (clases EnemyNormal/EnemyFlying).
 *   - Elegía un frame aleatorio como BufferedImage y lo pasaba al constructor.
 *   - El enemigo arrancaba con un frame fijo y sin sistema de animación.
 *
 * AHORA:
 *   - getHandle() obtiene el SpriteHandle del tipo de enemigo.
 *   - El handle se pasa al constructor del Enemy (MovingObjects).
 *   - El Enemy añade AnimationController con ese handle.
 *   - La animación "idle" se reproduce desde el primer frame automáticamente.
 *
 * ── COMPATIBILIDAD ────────────────────────────────────────────────────────
 * Los constructores de EnemyNormal y EnemyFlying todavía reciben BufferedImage
 * por su cadena de herencia (MovingObjects). Usamos resolveDefaultImage()
 * como puente hasta que MovingObjects migre a SpriteHandle nativo.
 *
 * El AnimationController se añade dentro de los constructores de Enemy
 * (ver EnemyNormal / EnemyFlying). EnemyFactory provee el handle correcto.
 */
public class EnemyFactory {

    public static Enemy createEnemy(EnemyType type, Vector2D position) {
        EnemyPhysics  physics = createPhysics(type);
        SpriteHandle  handle  = getHandle(type);

        // Imagen inicial para el SpriteRenderer base (legacy bridge).
        // AnimationController tomará el control desde el primer update().
        BufferedImage initialTexture = handle.resolveDefault().getImage();

        return switch (type) {
            case NORMAL_GROUND -> new EnemyNormal(position, initialTexture, physics, handle);
            case FLYING        -> new EnemyFlying(position, initialTexture, physics, handle);
        };
    }

    private static EnemyPhysics createPhysics(EnemyType type) {
        EnemyPhysicsConfig config = switch (type) {
            case NORMAL_GROUND -> EnemyPhysicsConfig.groundStandard();
            case FLYING        -> EnemyPhysicsConfig.flyingStandard();
        };
        return new EnemyPhysics(config);
    }

    /**
     * Devuelve el SpriteHandle correspondiente al tipo de enemigo.
     * Los handles están registrados en el AssetRegistry por EnemyAssets.init().
     */
    private static SpriteHandle getHandle(EnemyType type) {
        return switch (type) {
            case NORMAL_GROUND -> EnemyAssets.normalHandle;
            case FLYING        -> EnemyAssets.flyingHandle;
        };
    }
}
