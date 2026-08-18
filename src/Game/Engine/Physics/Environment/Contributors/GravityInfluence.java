package Game.Engine.Physics.Environment.Contributors;

import Game.Engine.Physics.Environment.EnvironmentState;
import Game.Engine.Physics.Environment.EnvironmentalContributor;

/**
 * Contributor que modifica la influencia gravitacional del ambiente mediante factores.
 *
 * ── HRFC-FASE2.5 — CORRECCIÓN SEMÁNTICA: OWNERSHIP DE GRAVEDAD ──────────
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * La ENTIDAD posee su gravedad propia.
 * El AMBIENTE solo la modifica mediante un FACTOR DE INFLUENCIA.
 *
 * ── OWNERSHIP CORRECTO ────────────────────────────────────────────────────
 *
 *   ENTIDAD (Physics2DComponent / PhysicalState):
 *     gravity → propiedad gravitacional PROPIA (ej: 9.8 m/s²)
 *     mass    → masa inercial (GravityProperties.MASS)
 *
 *   AMBIENTE (GravityInfluence):
 *     gravityInfluenceFactor → FACTOR multiplicador ambiental
 *     Ejemplos: 1.0 = normal, 0.0 = anulada, 2.0 = duplicada
 *
 *   RELACIÓN (evaluadores físicos):
 *     a_efectiva = entity.gravity × environment.gravityInfluenceFactor
 *
 * ── EJEMPLOS DE USO ───────────────────────────────────────────────────────
 *
 *   Condiciones terrestres normales:
 *     GravityInfluence.normal()  // factor 1.0 (sin modificación)
 *     Entity.gravity = 9.8 → efectivo: 9.8 × 1.0 = 9.8
 *
 *   Microgravedad / espacio:
 *     GravityInfluence.none()    // factor 0.0 (anula gravedad)
 *     Entity.gravity = 9.8 → efectivo: 9.8 × 0.0 = 0.0
 *
 *   Zona de gravedad reducida:
 *     GravityInfluence.scale(0.5)  // mitad de gravedad
 *     Entity.gravity = 9.8 → efectivo: 9.8 × 0.5 = 4.9
 *
 *   Zona de alta gravedad:
 *     GravityInfluence.scale(2.0)  // doble gravedad
 *     Entity.gravity = 9.8 → efectivo: 9.8 × 2.0 = 19.6
 *
 *   Gravedad lunar (aproximada):
 *     GravityInfluence.scale(0.165)  // 1.62/9.8 ≈ 0.165
 *     Entity.gravity = 9.8 → efectivo: 9.8 × 0.165 ≈ 1.62
 *
 * ── SEMÁNTICA MULTIPLICATIVA ──────────────────────────────────────────────
 * Este contributor MULTIPLICA el factor de influencia gravitacional existente.
 * Múltiples contributors se componen multiplicativamente:
 *
 *   base: factorY = 1.0
 *   + GravityInfluence.scale(0.5)
 *   + GravityInfluence.scale(2.0)
 *   → factor resultante: 1.0 × 0.5 × 2.0 = 1.0
 *
 * Para componentes X/Y independientes, use el constructor directo.
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * Esta implementación es inmutable. El factor de influencia es constante.
 * Para factores variables o localizados, crear una implementación dinámica
 * (ej: ProximityGravityModifier).
 *
 * ── IMPORTANTE ────────────────────────────────────────────────────────────
 * El ambiente NO dice: "La gravedad aquí ES 19.6."
 * El ambiente dice: "La gravedad de la entidad se ve modificada por ×2."
 *
 * La entidad es propietaria de su gravedad.
 * El ambiente solo la modifica.
 * Las Relations combinan ambos para producir efectos.
 */
public final class GravityInfluence implements EnvironmentalContributor {

    private final double influenceFactorX;
    private final double influenceFactorY;

    /**
     * Crea un modificador de influencia gravitacional con factores independientes por eje.
     *
     * @param influenceFactorX factor multiplicador en X (1.0 = sin modificación).
     * @param influenceFactorY factor multiplicador en Y (1.0 = sin modificación).
     */
    public GravityInfluence(double influenceFactorX, double influenceFactorY) {
        this.influenceFactorX = influenceFactorX;
        this.influenceFactorY = influenceFactorY;
    }

    /**
     * Condiciones terrestres normales (sin modificación de gravedad).
     *
     * @return contributor con factor 1.0 en ambos ejes.
     */
    public static GravityInfluence normal() {
        return new GravityInfluence(1.0, 1.0);
    }

    /**
     * Microgravedad / espacio (anula la gravedad de las entidades).
     *
     * @return contributor con factor 0.0 en ambos ejes.
     */
    public static GravityInfluence none() {
        return new GravityInfluence(0.0, 0.0);
    }

    /**
     * Modifica la gravedad mediante un factor escalar uniforme.
     *
     * @param factor factor multiplicador (1.0 = normal, 0.5 = mitad, 2.0 = doble).
     * @return contributor con el factor especificado en ambos ejes.
     */
    public static GravityInfluence scale(double factor) {
        return new GravityInfluence(factor, factor);
    }

    /**
     * Modifica solo el eje vertical (el más común).
     *
     * @param factorY factor multiplicador en Y (1.0 = normal, 0.0 = anulada).
     * @return contributor con factor especificado en Y, sin modificar X.
     */
    public static GravityInfluence vertical(double factorY) {
        return new GravityInfluence(1.0, factorY);
    }

    /**
     * Invierte la gravedad vertical (zona de gravedad invertida).
     *
     * @return contributor con factor -1.0 en Y (gravedad hacia arriba).
     */
    public static GravityInfluence inverted() {
        return new GravityInfluence(1.0, -1.0);
    }

    @Override
    public void contribute(EnvironmentState.Builder builder) {
        // Composición MULTIPLICATIVA de factores
        double currentX = builder.getGravityInfluenceX();
        double currentY = builder.getGravityInfluenceY();
        builder.gravityInfluenceX(currentX * influenceFactorX);
        builder.gravityInfluenceY(currentY * influenceFactorY);
    }

    @Override
    public String toString() {
        return String.format("GravityInfluence[×%.2f, ×%.2f]", influenceFactorX, influenceFactorY);
    }
}
