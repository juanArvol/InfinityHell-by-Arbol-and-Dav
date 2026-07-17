package Game.Enemys.Bosses.Sans.Variables;

/**
 * Constantes de configuración de Sans.
 *
 * ── HRFC-006 ──────────────────────────────────────────────────────────────
 * Migrado a EnemyStats / EnemyFlags / EnemyAttributes.
 *
 * SansVariables ya NO contiene claves de EnemyVariables.
 * Contiene únicamente constantes de valores concretos para cada fase,
 * que los assemblers y fases usan para configurar los módulos tipados.
 *
 * ── Dónde viven los datos de Sans ahora ──────────────────────────────────
 *
 *   Invulnerabilidad temporal →  enemy.getFlags().setInvincible(true/false)
 *   Timer de invulnerabilidad →  SansInvincibilityComponent (campo interno)
 *   Cooldown de ataque        →  enemy.getStats().setAttackCooldown(n)
 *   Rango de teletransporte   →  enemy.getStats().setTeleportRange(n)
 *   Daño                      →  enemy.getStats().setDamage(n)
 *   Velocidad                 →  enemy.getStats().setSpeed(n)
 *
 * Las claves String ("sans.atk_cooldown", etc.) han sido eliminadas.
 * Los flags booleanos ya no se almacenan en EnemyVariables.
 */
public final class SansVariables {

    // ── Vida ──────────────────────────────────────────────────────────────
    /** Sans muere de un golpe... si le das. */
    public static final int PHASE1_HP = 1;

    // ── Fase 1 — el perezoso ──────────────────────────────────────────────
    public static final int    PHASE1_ATK_COOLDOWN   = 120;    // 2 seg a 60 fps
    public static final double PHASE1_TELEPORT_RANGE = 300.0;
    public static final int    PHASE1_DAMAGE         = 4;
    public static final double PHASE1_SPEED          = 1.8;

    // ── Fase 2 — la determinación ─────────────────────────────────────────
    public static final int    PHASE2_ATK_COOLDOWN   = 30;     // más agresivo
    public static final double PHASE2_TELEPORT_RANGE = 800.0;
    public static final int    PHASE2_DAMAGE         = 16;
    public static final double PHASE2_SPEED          = 2.4;

    // ── Timer de invulnerabilidad post-teleporte ──────────────────────────
    /** Frames de invulnerabilidad que se activan al teletransportarse. */
    public static final int INVINCIBLE_FRAMES = 30;

    private SansVariables() {}
}
