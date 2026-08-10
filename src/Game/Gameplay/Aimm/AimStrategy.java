package Game.Gameplay.Aimm;

import Game.Player.PlayerState;

/**
 * Estrategia de apuntado — calculadora pura de dirección.
 *
 * ── HRFC — Player Reengineering ───────────────────────────────────────────
 *
 * CAMBIOS RESPECTO A LA VERSIÓN ANTERIOR:
 *
 *   ELIMINADO:
 *     private boolean dir
 *     private boolean aimingUpOrDown
 *     setDir() / getDir()
 *     setAimingUpOrDown() / getAimingUpOrDown()
 *     aim(Player player) — dependencia directa de Player
 *     class abstract — convertida a @FunctionalInterface
 *
 *   MOTIVACIÓN:
 *     AimStrategy mantenía dir y aimingUpOrDown como estado interno,
 *     duplicando lo que PlayerState ya almacena. Eso creaba dos fuentes
 *     de verdad con riesgo de divergencia entre frames.
 *
 *     La estrategia de apuntado es una CALCULADORA, no un PROPIETARIO de
 *     estado. Su responsabilidad es:
 *
 *       PlayerState → AimStrategy → AimDirection
 *
 *     Convertirla a @FunctionalInterface permite usar lambdas directamente
 *     en AimSelection, eliminando la necesidad de subclases anónimas.
 *
 * ── CONTRATO ──────────────────────────────────────────────────────────────
 *
 *   Cada implementación recibe PlayerState y devuelve un AimDirection.
 *   Si necesita actualizar el estado (ej. cambiar mirandoDerecha o
 *   verticalAim), lo hace directamente sobre PlayerState — que es la
 *   única fuente de verdad.
 *
 *   No se retiene ninguna referencia entre llamadas.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   AimSelection.apply(state) — punto de entrada único por frame.
 *   Las implementaciones concretas (AidleStrategy, lambdas en AimSelection)
 *   no necesitan conocer Player — solo PlayerState.
 */
@FunctionalInterface
public interface AimStrategy {

    /**
     * Calcula la dirección de apuntado para este frame.
     *
     * <p>La implementación puede leer y escribir en {@code state} para
     * reflejar los cambios de dirección (mirandoDerecha, verticalAim,
     * aimDirection). No debe almacenar ningún valor entre llamadas.
     *
     * <p>Para leer input usa {@link Inputs.KeyBoard#getState(String)}.
     *
     * @param state estado del jugador. Nunca null.
     * @return dirección calculada para este frame. Nunca null.
     */
    AimDirection calculateDirection(PlayerState state);
}
