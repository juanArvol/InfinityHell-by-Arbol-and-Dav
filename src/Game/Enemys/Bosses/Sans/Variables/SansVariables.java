package Game.Enemys.Bosses.Sans.Variables;

/**
 * Constantes de variables de Sans.
 *
 * Centraliza todas las claves y valores concretos que definen a Sans.
 * El Core nunca conoce estas constantes — solo el módulo Sans las usa.
 *
 * ── Claves de variables en EnemyVariables ────────────────────────────────
 * Las claves marcadas con * son leídas por las fases y patrones de Sans.
 *
 *   "sans.atk_cooldown"   * cooldown en frames entre ataques.
 *   "sans.teleport_range"   rango máximo de teletransporte.
 *   "sans.invincible"     * flag: Sans está en invulnerabilidad temporal.
 *   "sans.laugh_timer"      timer para el diálogo de burla.
 */
public final class SansVariables {

    // ── Claves ─────────────────────────────────────────────────────────────
    public static final String ATK_COOLDOWN   = "sans.atk_cooldown";
    public static final String TELEPORT_RANGE = "sans.teleport_range";
    public static final String INVINCIBLE     = "sans.invincible";
    public static final String LAUGH_TIMER    = "sans.laugh_timer";

    // ── Valores por fase ───────────────────────────────────────────────────
    // Fase 1 — relajado
    public static final int    PHASE1_HP              = 1;       // Sans muere de un golpe... si le das
    public static final int    PHASE1_ATK_COOLDOWN    = 120;     // 2 segundos a 60 fps
    public static final double PHASE1_TELEPORT_RANGE  = 300.0;
    public static final int    PHASE1_DAMAGE          = 4;

    // Fase 2 — determinación (< 50% HP no aplica — Sans tiene 1 HP siempre)
    // La "fase 2" de Sans es solo un cambio de comportamiento, no de HP.
    public static final int    PHASE2_ATK_COOLDOWN    = 30;      // más agresivo
    public static final double PHASE2_TELEPORT_RANGE  = 800.0;
    public static final int    PHASE2_DAMAGE          = 16;

    private SansVariables() {}
}
