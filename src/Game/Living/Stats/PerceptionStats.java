package Game.Living.Stats;

/**
 * Estadísticas de percepción de cualquier entidad viva.
 *
 * ── HRFC-007 — Generalización al Living Entity Core ──────────────────────
 * Movido desde Game.Enemys.Core.Stats a Game.Living.Stats.
 * Describe ÚNICAMENTE las propiedades que definen cómo y hasta dónde percibe
 * la entidad su entorno. No contiene combate ni movimiento.
 *
 * ── Campos ────────────────────────────────────────────────────────────────
 *   visionRange    — distancia máxima de detección visual (px).
 *   hearingRange   — distancia máxima de detección auditiva (px).
 *   detectionAngle — arco de visión frontal en grados (360 = omnidireccional).
 */
public class PerceptionStats {

    private double visionRange    = 400.0;
    private double hearingRange   = 200.0;
    private double detectionAngle = 360.0;

    public double getVisionRange()                     { return visionRange; }
    public PerceptionStats setVisionRange(double v)    { visionRange = v; return this; }

    public double getHearingRange()                    { return hearingRange; }
    public PerceptionStats setHearingRange(double v)   { hearingRange = v; return this; }

    public double getDetectionAngle()                  { return detectionAngle; }
    public PerceptionStats setDetectionAngle(double v) { detectionAngle = Math.max(0, Math.min(360, v)); return this; }
}
