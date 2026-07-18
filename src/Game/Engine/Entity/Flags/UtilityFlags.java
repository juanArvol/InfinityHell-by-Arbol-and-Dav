package Game.Engine.Entity.Flags;

/**
 * Estados especiales que no encajan en Capability, State, Impairment ni Damage.
 *
 * ── HRFC-007 — Nueva categoría del Living Entity Core ────────────────────
 * UtilityFlags agrupa estados misceláneos útiles para gameplay, IA,
 * render y animaciones, pero que no representan incapacitaciones ni daño.
 *
 * ── Responsabilidad única ─────────────────────────────────────────────────
 * UtilityFlags NO implementa ninguna lógica.
 * Los sistemas externos leen estos flags para tomar decisiones:
 *
 *   if (entity.getFlags().utility().isStealthed())   → la IA no detecta
 *   if (entity.getFlags().utility().isMarked())      → el renderer aplica outline
 *   if (entity.getFlags().utility().isChanneling())  → la animación de cast continúa
 *
 * ── Campos ────────────────────────────────────────────────────────────────
 *   stealthed       — no detectable por sistemas de IA.
 *   marked          — marcado como objetivo prioritario (p.ej. por hechizo).
 *   revealed        — forzado a ser visible aunque esté en sigilo.
 *   trackingTarget  — la entidad está rastreando activamente a un objetivo.
 *   channeling      — ejecutando una habilidad de canalización.
 *   charging        — cargando un ataque o habilidad.
 */
public class UtilityFlags {

    private boolean stealthed      = false;
    private boolean marked         = false;
    private boolean revealed       = false;
    private boolean trackingTarget = false;
    private boolean channeling     = false;
    private boolean charging       = false;

    public boolean isStealthed()                   { return stealthed; }
    public UtilityFlags setStealthed(boolean v)    { stealthed = v; return this; }

    public boolean isMarked()                      { return marked; }
    public UtilityFlags setMarked(boolean v)       { marked = v; return this; }

    public boolean isRevealed()                    { return revealed; }
    public UtilityFlags setRevealed(boolean v)     { revealed = v; return this; }

    public boolean isTrackingTarget()                      { return trackingTarget; }
    public UtilityFlags setTrackingTarget(boolean v)       { trackingTarget = v; return this; }

    public boolean isChanneling()                  { return channeling; }
    public UtilityFlags setChanneling(boolean v)   { channeling = v; return this; }

    public boolean isCharging()                    { return charging; }
    public UtilityFlags setCharging(boolean v)     { charging = v; return this; }

    /** True si la entidad no es visible ni detectable por la IA. */
    public boolean isHidden() {
        return stealthed && !revealed;
    }
}
