package Game.Items.Types.Bullets.BulletComport;

/**
 * Datos calculados de un proyectil para presentación en UI.
 *
 * ── HRFC — Projectile Construction & Transformation Pipeline ─────────────
 *
 * BulletStats es un DTO inmutable destinado a la capa de UI (CrossHairHUD,
 * tooltips, previews). No se usa para construir proyectiles.
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 *
 * Convertido a record: todos los campos son final. La API de acceso es
 * compatible con el código existente: getSpeed(), getDamage(), getLifeTime(),
 * hasGravity() se mantienen como métodos de conveniencia sobre los componentes
 * del record.
 *
 * ── hasGravity ────────────────────────────────────────────────────────────
 *
 * hasGravity ya NO se deriva de ProjectileData.gravityValue().
 * Se deriva del movement real del ProjectileBlueprint, vía BulletFactory.statsFrom().
 * Si el movement del Blueprint incluye GravityMovement (o CompositeMovement),
 * hasGravity es true. Esta es la única fuente de verdad para UI.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   ProjectileBlueprint bp = ProjectileBlueprint.from(behavior, speed, damage);
 *   BulletStats stats = BulletFactory.statsFrom(bp);
 *   // En CrossHairHUD:
 *   double gravity = stats.hasGravity() ? 0.4 : 0.0;
 */
public record BulletStats(
        double  speed,
        double  damage,
        int     lifeTime,
        boolean hasGravity
) {
    // ── API de compatibilidad con el código existente ─────────────────────
    // CrossHairHUD y cualquier otro consumidor usa los getters —
    // los mantenemos para no romper los call sites actuales.

    public double  getSpeed()    { return speed;    }
    public double  getDamage()   { return damage;   }
    public int     getLifeTime() { return lifeTime; }
    public boolean hasGravity()  { return hasGravity; }
}
