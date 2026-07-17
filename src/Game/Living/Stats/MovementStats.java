package Game.Living.Stats;

/**
 * Estadísticas de movimiento de cualquier entidad viva.
 *
 * ── HRFC-007 — Generalización al Living Entity Core ──────────────────────
 * Movido desde Game.Enemys.Core.Stats a Game.Living.Stats.
 * Describe ÚNICAMENTE las propiedades que controlan el desplazamiento
 * de una entidad en el mundo. No contiene combate ni percepción.
 *
 * ── Campos ────────────────────────────────────────────────────────────────
 *   speed        — velocidad máxima de desplazamiento (px/frame).
 *   acceleration — cuán rápido alcanza la velocidad máxima.
 *   friction     — deceleración pasiva cuando no hay input de movimiento.
 *   jumpHeight   — impulso vertical en píxeles (0 si no puede saltar).
 *   dashDistance — distancia máxima de dash (0 si no puede hacer dash).
 */
public class MovementStats {

    private double speed        = 0.0;
    private double acceleration = 1.0;
    private double friction     = 0.85;
    private double jumpHeight   = 0.0;
    private double dashDistance = 0.0;

    public double getSpeed()                        { return speed; }
    public MovementStats setSpeed(double v)         { speed = v; return this; }

    public double getAcceleration()                 { return acceleration; }
    public MovementStats setAcceleration(double v)  { acceleration = v; return this; }

    public double getFriction()                     { return friction; }
    public MovementStats setFriction(double v)      { friction = v; return this; }

    public double getJumpHeight()                   { return jumpHeight; }
    public MovementStats setJumpHeight(double v)    { jumpHeight = v; return this; }

    public double getDashDistance()                 { return dashDistance; }
    public MovementStats setDashDistance(double v)  { dashDistance = v; return this; }
}
