package Game.Engine.Entity.Stats;

/**
 * Estadísticas de resistencia elemental de cualquier entidad viva.
 *
 * ── HRFC-007 — Generalización al Living Entity Core ──────────────────────
 * Movido desde Game.Enemys.Core.Stats a Game.Living.Stats.
 * Cada resistencia es un valor en el rango [-1.0, 1.0]:
 *
 *   1.0  = inmunidad completa (absorbe el daño).
 *   0.5  = resiste el 50% (recibe la mitad).
 *   0.0  = sin resistencia (daño normal).
 *  -0.5  = vulnerabilidad (recibe 50% más de daño).
 *  -1.0  = debilidad extrema (daño doble).
 *
 * ── Campos ────────────────────────────────────────────────────────────────
 *   fireResistance      — resistencia al fuego.
 *   iceResistance       — resistencia al hielo.
 *   electricResistance  — resistencia a la electricidad.
 *   poisonResistance    — resistencia al veneno.
 *   curseResistance     — resistencia a maldiciones.
 */
public class ResistanceStats {

    private double fireResistance     = 0.0;
    private double iceResistance      = 0.0;
    private double electricResistance = 0.0;
    private double poisonResistance   = 0.0;
    private double curseResistance    = 0.0;

    public double getFireResistance()                      { return fireResistance; }
    public ResistanceStats setFireResistance(double v)     { fireResistance = clamp(v); return this; }

    public double getIceResistance()                       { return iceResistance; }
    public ResistanceStats setIceResistance(double v)      { iceResistance = clamp(v); return this; }

    public double getElectricResistance()                  { return electricResistance; }
    public ResistanceStats setElectricResistance(double v) { electricResistance = clamp(v); return this; }

    public double getPoisonResistance()                    { return poisonResistance; }
    public ResistanceStats setPoisonResistance(double v)   { poisonResistance = clamp(v); return this; }

    public double getCurseResistance()                     { return curseResistance; }
    public ResistanceStats setCurseResistance(double v)    { curseResistance = clamp(v); return this; }

    /**
     * Aplica la resistencia correspondiente a un valor de daño y devuelve el daño efectivo.
     *
     * @param rawDamage daño bruto entrante.
     * @param type      tipo elemental del daño.
     * @return daño final tras aplicar la resistencia (mínimo 0).
     */
    public int applyResistance(double rawDamage, ResistanceType type) {
        double resistance = switch (type) {
            case FIRE     -> fireResistance;
            case ICE      -> iceResistance;
            case ELECTRIC -> electricResistance;
            case POISON   -> poisonResistance;
            case CURSE    -> curseResistance;
        };
        // damage * (1 - resistance): a 0.5 resist → 0.5× damage; a -0.5 → 1.5× damage
        return Math.max(0, (int) Math.round(rawDamage * (1.0 - resistance)));
    }

    /** Tipos de daño elemental reconocidos por ResistanceStats. */
    public enum ResistanceType {
        FIRE, ICE, ELECTRIC, POISON, CURSE
    }

    private double clamp(double v) { return Math.max(-1.0, Math.min(1.0, v)); }
}
