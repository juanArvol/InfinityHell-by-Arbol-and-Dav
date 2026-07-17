package Game.Living.Flags;

/**
 * Estados internos activos de una entidad viva en un momento dado.
 *
 * ── HRFC-007 — Generalización al Living Entity Core ──────────────────────
 * Movido desde Game.Enemys.Core.Flags a Game.Living.Flags.
 * StateFlags describe condiciones activas que NO son incapacitaciones
 * (eso es ImpairmentFlags) ni daño periódico (eso es DamageFlags).
 *
 * ── Quién escribe StateFlags ─────────────────────────────────────────────
 *   StatusEffects  — efectos que alteran el estado de la entidad.
 *   Phases         — fases de combate que activan/desactivan estados.
 *   Assemblers     — configuran el estado inicial de la entidad.
 *
 * ── Quién lee StateFlags ─────────────────────────────────────────────────
 *   Update loop    — decide si delegar a controladores este frame.
 *   AttackPatterns — comprueban isInvincible() antes de disparar.
 *   AnimationSystem — selecciona animación según estado activo.
 *   DamageSystem   — comprueba isInvincible() antes de aplicar daño.
 *   AI             — considera isRageMode() para ajustar su comportamiento.
 *
 * ── Los flags NO sustituyen al StatusEffect ───────────────────────────────
 * Los flags existen únicamente como vista rápida del estado actual.
 * La lógica de activación, duración y efectos vive en StatusEffect.
 *
 * ── Campos ────────────────────────────────────────────────────────────────
 *   invincible  — ignora todo daño recibido.
 *   flying      — en modo vuelo activo (sin gravedad terrestre).
 *   invisible   — no detectado visualmente por sistemas de IA.
 *   rageMode    — buffeado: mayor velocidad y daño (fases de Boss).
 */
public class StateFlags {

    private boolean invincible = false;
    private boolean flying     = false;
    private boolean invisible  = false;
    private boolean rageMode   = false;

    public boolean isInvincible()              { return invincible; }
    public StateFlags setInvincible(boolean v) { invincible = v; return this; }

    public boolean isFlying()              { return flying; }
    public StateFlags setFlying(boolean v) { flying = v; return this; }

    public boolean isInvisible()              { return invisible; }
    public StateFlags setInvisible(boolean v) { invisible = v; return this; }

    public boolean isRageMode()              { return rageMode; }
    public StateFlags setRageMode(boolean v) { rageMode = v; return this; }
}
