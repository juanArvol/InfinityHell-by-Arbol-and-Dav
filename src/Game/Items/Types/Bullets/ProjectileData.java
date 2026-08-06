package Game.Items.Types.Bullets;

/**
 * Datos de configuración inmutables de un proyectil.
 *
 * ── HRFC — Projectile System Refactor ────────────────────────────────────
 *
 * CAMBIOS RESPECTO A LA VERSIÓN ANTERIOR:
 *
 *   ELIMINADO: hasGravity (boolean)
 *     Era redundante. gravityValue > 0 ya implica gravedad activa.
 *     gravityValue == 0.0 implica sin gravedad.
 *     Tener los dos campos juntos duplicaba la representación del mismo
 *     concepto y obligaba a mantenerlos sincronizados manualmente.
 *
 *   AÑADIDO: assetKey (String, nullable)
 *     Clave del sprite a usar para este proyectil.
 *     null = sprite por defecto (la bala estándar de BulletAssets).
 *     Permite que cada tipo de proyectil tenga su propio sprite sin
 *     modificar BulletFactory.
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 *
 * ProjectileData es un record — todos sus campos son final.
 * Si un behavior necesita cambiar velocidad en runtime, lo hace sobre
 * BulletPhysics directamente. ProjectileData solo define valores de spawn.
 *
 * ── CAMPOS ────────────────────────────────────────────────────────────────
 *
 *   damage       — daño base del proyectil al impactar.
 *   speedFactor  — multiplicador sobre bulletSpeedBase del arma. 1.0 = normal.
 *   lifeTime     — ticks de vida máximos antes de expirar.
 *   gravityValue — aceleración gravitacional por frame.
 *                  0.0 = sin gravedad (lineal).
 *                  > 0 = cae con esa aceleración.
 *                  < 0 = sube (antigravedad, proyectiles ascendentes).
 *   width/height — dimensiones del collider en píxeles.
 *   assetKey     — clave del sprite (null = sprite por defecto).
 */
public record ProjectileData(
        int    damage,
        double speedFactor,
        int    lifeTime,
        double gravityValue,
        int    width,
        int    height,
        String assetKey
) {

    // ── Constructores de conveniencia ─────────────────────────────────────

    /**
     * Sin gravedad, collider 8×8, sprite por defecto.
     * El caso más común — balas rectas, rayos, proyectiles de energía.
     */
    public static ProjectileData flat(int damage, double speedFactor, int lifeTime) {
        return new ProjectileData(damage, speedFactor, lifeTime, 0.0, 8, 8, null);
    }

    /**
     * Con gravedad, collider 8×8, sprite por defecto.
     * Para flechas, bombas, bolas de fuego, proyectiles físicos.
     */
    public static ProjectileData gravity(int damage, double speedFactor, int lifeTime,
                                         double gravityValue) {
        return new ProjectileData(damage, speedFactor, lifeTime, gravityValue, 8, 8, null);
    }

    /**
     * Sin gravedad, tamaño personalizado, sprite por defecto.
     */
    public static ProjectileData sized(int damage, double speedFactor, int lifeTime,
                                       int width, int height) {
        return new ProjectileData(damage, speedFactor, lifeTime, 0.0, width, height, null);
    }

    // ── Compatibilidad — constructor 5-campo (sin width/height/assetKey) ─────

    /**
     * Constructor de compatibilidad con código que usaba la versión anterior
     * de 5 campos (damage, speedFactor, lifeTime, hasGravity, gravityValue).
     *
     * hasGravity se reemplaza por: gravityValue > 0 implica gravedad activa.
     * Si hasGravity era true, pasar el gravityValue real.
     * Si hasGravity era false, pasar 0.0 como gravityValue.
     *
     * @param damage       daño base
     * @param speedFactor  multiplicador de velocidad
     * @param lifeTime     ticks de vida
     * @param gravityValue aceleración gravitacional (0.0 = sin gravedad)
     */
    public ProjectileData(int damage, double speedFactor, int lifeTime, double gravityValue) {
        this(damage, speedFactor, lifeTime, gravityValue, 8, 8, null);
    }

    // ── Consultas de conveniencia ─────────────────────────────────────────

    /** @return true si este proyectil tiene gravedad activa (gravityValue != 0). */
    public boolean hasGravity() {
        return gravityValue != 0.0;
    }

    // ── Withers — derivaciones puntuales ──────────────────────────────────

    /** Retorna una copia con un damage diferente. */
    public ProjectileData withDamage(int newDamage) {
        return new ProjectileData(newDamage, speedFactor, lifeTime, gravityValue, width, height, assetKey);
    }

    /** Retorna una copia con un lifeTime diferente. */
    public ProjectileData withLifeTime(int newLifeTime) {
        return new ProjectileData(damage, speedFactor, newLifeTime, gravityValue, width, height, assetKey);
    }

    /** Retorna una copia con un speedFactor diferente. */
    public ProjectileData withSpeedFactor(double newSpeedFactor) {
        return new ProjectileData(damage, newSpeedFactor, lifeTime, gravityValue, width, height, assetKey);
    }

    /** Retorna una copia con tamaño de collider diferente. */
    public ProjectileData withSize(int newWidth, int newHeight) {
        return new ProjectileData(damage, speedFactor, lifeTime, gravityValue, newWidth, newHeight, assetKey);
    }

    /** Retorna una copia con un assetKey diferente. */
    public ProjectileData withAsset(String newAssetKey) {
        return new ProjectileData(damage, speedFactor, lifeTime, gravityValue, width, height, newAssetKey);
    }

    /** Retorna una copia con gravedad configurada. */
    public ProjectileData withGravity(double newGravityValue) {
        return new ProjectileData(damage, speedFactor, lifeTime, newGravityValue, width, height, assetKey);
    }
}
