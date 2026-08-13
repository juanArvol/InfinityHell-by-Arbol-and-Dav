package Game.Items.Types.Bullets;

import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.Definition.ProjectileData;
import Game.Items.Types.Bullets.Movement.GravityMovement;
import Game.Items.Types.Bullets.Movement.LinearMovement;

/**
 * Definición completa y resuelta de un proyectil antes de que exista la instancia Bullet.
 *
 * ── HRFC — Projectile Construction & Transformation Pipeline ─────────────
 *
 * ProjectileBlueprint es el contrato entre el sistema de armas/enemigos y
 * BulletFactory. Representa la "receta final" del proyectil: todos los valores
 * ya calculados, todas las transformaciones ya aplicadas.
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 *   ProjectileData     = datos declarativos del BulletBehavior (inmutable, por tipo)
 *   ProjectileBlueprint= definición resuelta para esta instancia concreta
 *   BulletFactory      = construcción pura de Bullet desde Blueprint
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 *
 * ProjectileBlueprint es inmutable. Los withers crean nuevas instancias.
 * El pipeline de modificadores trabaja sobre withers, nunca mutando el Blueprint.
 * Esto hace el pipeline composable, seguro ante concurrencia y predecible.
 *
 * ── CONSTRUCCIÓN ──────────────────────────────────────────────────────────
 *
 * La entrada estándar es ProjectileBlueprint.from(behavior, speed, damage).
 * Este método resuelve el movimiento definitivo desde getDefaultMovement(),
 * incluyendo la composición de gravedad si el behavior la declara en su
 * ProjectileData — esa lógica pertenece aquí, no en BulletFactory.
 *
 * ── PIPELINE DE MODIFIERS ─────────────────────────────────────────────────
 *
 *   ProjectileBlueprint bp = ProjectileBlueprint.from(behavior, speed, damage);
 *   bp = gravityModifier.apply(bp);   // o cualquier ProjectileModifier
 *   bp = damageModifier.apply(bp);
 *   Bullet bullet = BulletFactory.build(bp, position, direction);
 *
 * ── CAMPOS ────────────────────────────────────────────────────────────────
 *
 *   behavior         — qué hace al impactar y cada frame.
 *   movement         — cómo se mueve cada frame (ya resuelto, incluyendo gravedad).
 *   speed            — velocidad escalar total (unidades/frame).
 *   damage           — daño al impactar (ya calculado: base + weapon bonus).
 *   lifeTime         — ticks de vida máximos.
 *   width / height   — dimensiones del collider.
 *   assetKey         — clave del sprite (null = default "bullet.bala").
 *   collisionProfile — perfil de colisión (null = default PLAYER_BULLET en Bullet).
 */
public final class ProjectileBlueprint {

    // Campos inmutables — todos finales
    private final BulletBehavior     behavior;
    private final ProjectileMovement movement;
    private final double             speed;
    private final double             damage;
    private final int                lifeTime;
    private final int                width;
    private final int                height;
    private final String             assetKey;
    private final CollisionProfile   collisionProfile;

    // ── Constructor privado ───────────────────────────────────────────────

    private ProjectileBlueprint(
            BulletBehavior   behavior,
            ProjectileMovement movement,
            double           speed,
            double           damage,
            int              lifeTime,
            int              width,
            int              height,
            String           assetKey,
            CollisionProfile collisionProfile
    ) {
        // Defensivo — los withers no deben producir nulls en comportamiento obligatorio
        this.behavior         = (behavior != null) ? behavior : new DefaultBehavior();
        this.movement         = (movement != null) ? movement : LinearMovement.INSTANCE;
        this.speed            = speed;
        this.damage           = damage;
        this.lifeTime         = (lifeTime > 0) ? lifeTime : 1;
        this.width            = (width  > 0) ? width  : 8;
        this.height           = (height > 0) ? height : 8;
        this.assetKey         = assetKey;         // null = default, permitido
        this.collisionProfile = collisionProfile; // null = default PLAYER_BULLET en Bullet
    }

    // ── Factory estático principal ────────────────────────────────────────

    /**
     * Construye un Blueprint desde un BulletBehavior con speed y damage finales.
     *
     * Este método encapsula la resolución de movimiento, incluyendo la composición
     * de gravedad si el behavior la declara en su ProjectileData. Esta lógica
     * pertenece aquí — no en BulletFactory.
     *
     * Regla de gravedad:
     *   Si ProjectileData.gravityValue() != 0.0 Y el movement declarado por el
     *   behavior no incluye ya una GravityMovement, se compone automáticamente.
     *   Si el behavior ya declara GravityMovement en getDefaultMovement() (como
     *   BulletJump), no se añade gravedad adicional.
     *   Un solo punto de resolución — sin doble fuente de verdad.
     *
     * @param behavior   behavior del proyectil (define movement y data base)
     * @param speed      velocidad escalar final (calculada externamente)
     * @param damage     daño final (calculado externamente)
     * @return Blueprint listo para pasar a un pipeline de modifiers o a BulletFactory
     */
    public static ProjectileBlueprint from(BulletBehavior behavior,
                                           double speed,
                                           double damage) {
        ProjectileData     data     = behavior.getDefaultData();
        ProjectileMovement movement = behavior.getDefaultMovement();

        // Resolución de gravedad: una sola fuente de verdad.
        // Si el behavior declara gravityValue en ProjectileData Y el movement
        // base no incluye ya GravityMovement, se compone aquí.
        // Delegamos a BulletFactory.containsGravity() — única fuente de verdad.
        if (data.hasGravity() && !BulletFactory.containsGravity(movement)) {
            movement = movement.andThen(new GravityMovement(data.gravityValue()));
        }

        return new ProjectileBlueprint(
                behavior,
                movement,
                speed,
                damage,
                data.lifeTime(),
                data.width(),
                data.height(),
                data.assetKey(),
                null  // collisionProfile: default PLAYER_BULLET — Bullet lo aplica
        );
    }

    /**
     * Versión con collisionProfile explícito.
     * Usar cuando el proyectil no es del jugador (p. ej. proyectiles enemigos).
     *
     * @param behavior         behavior del proyectil
     * @param speed            velocidad escalar final
     * @param damage           daño final
     * @param collisionProfile perfil de colisión explícito
     */
    public static ProjectileBlueprint from(BulletBehavior behavior,
                                           double speed,
                                           double damage,
                                           CollisionProfile collisionProfile) {
        return from(behavior, speed, damage).withCollisionProfile(collisionProfile);
    }

    // ── Withers — derivaciones inmutables ────────────────────────────────

    /** Retorna una copia con un behavior diferente. */
    public ProjectileBlueprint withBehavior(BulletBehavior newBehavior) {
        return new ProjectileBlueprint(newBehavior, movement, speed, damage,
                lifeTime, width, height, assetKey, collisionProfile);
    }

    /** Retorna una copia con un movement diferente. */
    public ProjectileBlueprint withMovement(ProjectileMovement newMovement) {
        return new ProjectileBlueprint(behavior, newMovement, speed, damage,
                lifeTime, width, height, assetKey, collisionProfile);
    }

    /** Retorna una copia con una speed diferente. */
    public ProjectileBlueprint withSpeed(double newSpeed) {
        return new ProjectileBlueprint(behavior, movement, newSpeed, damage,
                lifeTime, width, height, assetKey, collisionProfile);
    }

    /** Retorna una copia con un damage diferente. */
    public ProjectileBlueprint withDamage(double newDamage) {
        return new ProjectileBlueprint(behavior, movement, speed, newDamage,
                lifeTime, width, height, assetKey, collisionProfile);
    }

    /** Retorna una copia con un lifeTime diferente. */
    public ProjectileBlueprint withLifeTime(int newLifeTime) {
        return new ProjectileBlueprint(behavior, movement, speed, damage,
                newLifeTime, width, height, assetKey, collisionProfile);
    }

    /** Retorna una copia con dimensiones de collider diferentes. */
    public ProjectileBlueprint withSize(int newWidth, int newHeight) {
        return new ProjectileBlueprint(behavior, movement, speed, damage,
                lifeTime, newWidth, newHeight, assetKey, collisionProfile);
    }

    /** Retorna una copia con un assetKey diferente. */
    public ProjectileBlueprint withAssetKey(String newAssetKey) {
        return new ProjectileBlueprint(behavior, movement, speed, damage,
                lifeTime, width, height, newAssetKey, collisionProfile);
    }

    /** Retorna una copia con un CollisionProfile diferente. */
    public ProjectileBlueprint withCollisionProfile(CollisionProfile newProfile) {
        return new ProjectileBlueprint(behavior, movement, speed, damage,
                lifeTime, width, height, assetKey, newProfile);
    }

    /** Retorna una copia con movement compuesto (this.movement.andThen(extra)). */
    public ProjectileBlueprint andThenMovement(ProjectileMovement extra) {
        return withMovement(this.movement.andThen(extra));
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public BulletBehavior     behavior()         { return behavior;         }
    public ProjectileMovement movement()          { return movement;          }
    public double             speed()             { return speed;             }
    public double             damage()            { return damage;            }
    public int                lifeTime()          { return lifeTime;          }
    public int                width()             { return width;             }
    public int                height()            { return height;            }
    public String             assetKey()          { return assetKey;          }
    public CollisionProfile   collisionProfile()  { return collisionProfile;  }



    // ── Behavior vacío de fallback (defensive) ────────────────────────────

    /**
     * Behavior nulo-seguro para el caso patológico de constructor con null behavior.
     * No debería ocurrir en código correcto, pero evita NPE si ocurre.
     */
    private static final class DefaultBehavior extends BulletBehavior {
        @Override
        public ProjectileData getDefaultData() {
            return ProjectileData.flat(1, 1.0, 10);
        }
    }

    // ── Debug ─────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "ProjectileBlueprint["
                + "behavior=" + behavior.getClass().getSimpleName()
                + ", speed=" + speed
                + ", damage=" + damage
                + ", lifeTime=" + lifeTime
                + ", assetKey=" + assetKey
                + ", collisionProfile=" + (collisionProfile != null ? "explicit" : "default")
                + "]";
    }
}
