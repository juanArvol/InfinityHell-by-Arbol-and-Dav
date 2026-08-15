package Game.Engine.Physics.KineticPhysics;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.Physics.KineticPhysics.Types.Physics2D;

/**
 * Utilidad para aplicar impulso de retroceso (recoil) al disparar armas.
 *
 * ── HRFC — Kinetic Physics: Forces, Impulses & Motion Intent ─────────────
 *
 * ── CLASIFICACIÓN: IMPULSE (NO Motion Intent) ────────────────────────────
 *
 * El recoil se clasifica como IMPULSE simple porque:
 *
 * 1. Es una REACCIÓN FÍSICA DIRECTA al disparo (ley de acción-reacción)
 * 2. NO expresa intención de gameplay ("quiero retroceder")
 * 3. La magnitud es FIJA por arma (no depende de capacidades del shooter)
 * 4. Se aplica instantáneamente en dirección opuesta al disparo
 *
 * A diferencia de Motion Intent (Jump, Dash), el recoil NO consulta
 * PhysicalCapabilities del shooter. Es puramente newtoniano:
 *
 *   Acción:   Proyectil sale con momentum +P
 *   Reacción: Shooter recibe impulso -P (escalado por recoilForce)
 *
 * ── FÍSICA DEL RECOIL ─────────────────────────────────────────────────────
 *
 * El recoil es un impulso aplicado en dirección opuesta al disparo:
 *
 *   direction_shot = (dx, dy)  // dirección del proyectil
 *   direction_recoil = -(dx, dy)  // dirección opuesta
 *   J_recoil = recoilForce × direction_recoil
 *   physics.addForce(Jx, Jy)
 *
 * El cambio de velocidad resultante:
 *
 *   Δv = J / m
 *
 * Esto significa que:
 *   - Shooters más masivos reciben menor Δv (más estables)
 *   - Shooters más ligeros reciben mayor Δv (más knockback)
 *
 * ── ESCALADO POR MASA ─────────────────────────────────────────────────────
 *
 * Un jugador con mass=40 y un enemigo con mass=80 reciben el mismo impulso
 * pero diferente cambio de velocidad:
 *
 *   Player:  Δv = 30 / 40 = 0.75 px/frame
 *   Enemy:   Δv = 30 / 80 = 0.375 px/frame
 *
 * Esto es físicamente correcto y deseable: entidades más masivas son más
 * estables al disparar.
 *
 * ── MODIFICADORES ─────────────────────────────────────────────────────────
 *
 * Si se desea que el recoil sea modificable por gameplay (ej: "Recoil
 * Stabilizer" amulet), aplicar el modificador ANTES de llamar a applyRecoil():
 *
 *   double effectiveRecoil = weaponStats.getRecoilForce() * stabilizationFactor;
 *   RecoilImpulse.applyRecoil(shooterPhysics, shotDirection, effectiveRecoil);
 *
 * NO se usa PhysicalCapabilities porque el recoil no es una capacidad
 * muscular del shooter — es una propiedad del arma.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 * Desde ModifiedWeapon.tryShoot() o similar:
 *
 *   // Después de construir los proyectiles
 *   if (owner instanceof AbstractEntity) {
 *       AbstractEntity shooter = (AbstractEntity) owner;
 *       Physics2DComponent physicsComp = shooter.getComponent(Physics2DComponent.class);
 *       if (physicsComp != null) {
 *           RecoilImpulse.applyRecoil(
 *               physicsComp.getPhysics(),
 *               shotDirection,
 *               resolved.stats().getRecoilForce()
 *           );
 *       }
 *   }
 *
 * ── DIRECCIÓN DEL RECOIL ──────────────────────────────────────────────────
 *
 * El recoil se aplica en dirección OPUESTA al disparo:
 *
 *   Disparo hacia la derecha (+X) → Recoil hacia la izquierda (-X)
 *   Disparo hacia arriba (-Y)     → Recoil hacia abajo (+Y)
 *   Disparo diagonal              → Recoil diagonal opuesto
 *
 * ── RECOIL VERTICAL vs HORIZONTAL ─────────────────────────────────────────
 *
 * Esta implementación aplica recoil en AMBOS ejes según la dirección del
 * disparo. Si se desea recoil solo horizontal (ignorar componente Y), usar
 * applyRecoilHorizontalOnly().
 *
 * ── COMPATIBILIDAD CON addForce() ─────────────────────────────────────────
 *
 * RecoilImpulse usa Physics2D.addForce() internamente, integrándose
 * correctamente con el sistema de fuerzas/impulsos consolidado.
 */
public final class RecoilImpulse {

    private RecoilImpulse() {
        // Clase de utilidad — no instanciar
    }

    /**
     * Aplica impulso de retroceso al shooter en dirección opuesta al disparo.
     *
     * Fórmula:
     *   J_recoil = recoilForce × (-direction_normalized)
     *   physics.addForce(Jx, Jy)
     *
     * @param physics física del shooter que recibe el recoil
     * @param shotDirection dirección del disparo (hacia donde salió el proyectil)
     * @param recoilForce magnitud del impulso de retroceso (debe ser >= 0)
     */
    public static void applyRecoil(Physics2D physics,
                                    Vector2D shotDirection,
                                    double recoilForce) {
        if (physics == null) {
            throw new IllegalArgumentException("physics no puede ser null");
        }
        if (shotDirection == null) {
            throw new IllegalArgumentException("shotDirection no puede ser null");
        }
        if (recoilForce < 0) {
            throw new IllegalArgumentException("recoilForce debe ser >= 0");
        }
        if (recoilForce == 0) {
            return; // Sin recoil
        }

        // Normalizar dirección del disparo
        Vector2D dirNormalized = shotDirection.normalize();

        // Calcular dirección opuesta (recoil)
        double recoilDirX = -dirNormalized.getX();
        double recoilDirY = -dirNormalized.getY();

        // Calcular impulso de recoil
        double impulseX = recoilForce * recoilDirX;
        double impulseY = recoilForce * recoilDirY;

        // Aplicar impulso
        physics.addForce(impulseX, impulseY);
    }

    /**
     * Aplica impulso de retroceso solo en el eje horizontal, ignorando
     * la componente vertical del disparo.
     *
     * Útil para armas donde el recoil vertical es negligible o se maneja
     * de otra forma (ej: camera shake, animación).
     *
     * @param physics física del shooter que recibe el recoil
     * @param shotDirection dirección del disparo
     * @param recoilForce magnitud del impulso de retroceso
     */
    public static void applyRecoilHorizontalOnly(Physics2D physics,
                                                  Vector2D shotDirection,
                                                  double recoilForce) {
        if (physics == null) {
            throw new IllegalArgumentException("physics no puede ser null");
        }
        if (shotDirection == null) {
            throw new IllegalArgumentException("shotDirection no puede ser null");
        }
        if (recoilForce < 0) {
            throw new IllegalArgumentException("recoilForce debe ser >= 0");
        }
        if (recoilForce == 0) {
            return; // Sin recoil
        }

        // Solo considerar componente X
        double dirX = shotDirection.getX();
        double magnitude = Math.abs(dirX);

        if (magnitude > 0) {
            double recoilDirX = -dirX / magnitude; // Normalizar y oponer
            double impulseX = recoilForce * recoilDirX;
            physics.addForce(impulseX, 0);
        }
    }

    /**
     * Aplica recoil con factor de escalado.
     * Útil para aplicar modificadores de gameplay (amulets, buffs).
     *
     * Ejemplo:
     *   // Recoil Stabilizer: reduce recoil a 50%
     *   applyRecoilScaled(physics, direction, baseRecoil, 0.5);
     *
     * @param physics física del shooter
     * @param shotDirection dirección del disparo
     * @param baseRecoilForce fuerza base de retroceso
     * @param scaleFactor multiplicador (1.0 = sin cambio, 0.5 = mitad, 2.0 = doble)
     */
    public static void applyRecoilScaled(Physics2D physics,
                                         Vector2D shotDirection,
                                         double baseRecoilForce,
                                         double scaleFactor) {
        if (scaleFactor < 0) {
            throw new IllegalArgumentException("scaleFactor debe ser >= 0");
        }

        double effectiveRecoil = baseRecoilForce * scaleFactor;
        applyRecoil(physics, shotDirection, effectiveRecoil);
    }

    /**
     * Calcula el cambio de velocidad que produciría el recoil sin aplicarlo.
     * Útil para preview y debug.
     *
     * @param physics física del shooter (para consultar masa)
     * @param recoilForce magnitud del impulso
     * @return magnitud del cambio de velocidad (px/frame)
     */
    public static double calculateRecoilVelocityChange(Physics2D physics, double recoilForce) {
        if (physics == null) {
            throw new IllegalArgumentException("physics no puede ser null");
        }
        if (recoilForce < 0) {
            throw new IllegalArgumentException("recoilForce debe ser >= 0");
        }

        // Δv = J / m
        return recoilForce / physics.getMass();
    }
}
