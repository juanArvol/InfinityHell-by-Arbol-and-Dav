package Game.Gameplay.Aimm;

import Game.Player.PlayerState;
import Inputs.KeyBoard;

/**
 * Selecciona la estrategia de apuntado según el input actual y aplica
 * el resultado directamente en PlayerState.
 *
 * ── HRFC — Player Reengineering ───────────────────────────────────────────
 *
 * CAMBIOS:
 *   - apply(PlayerState) reemplaza getStrategy(): en lugar de retornar
 *     una estrategia que el caller debe invocar por separado, aplica el
 *     resultado completo de una sola vez. El flujo es:
 *
 *       Input → AimSelection.apply(state) → PlayerState.aimDirection
 *
 *   - Añadido soporte para tecla C (modo apuntado).
 *     Con C presionado: solo se usa el input vertical (↑/↓) para apuntar.
 *     El movimiento horizontal es inhibido por PlayerController cuando
 *     isAiming() == true.
 *
 *   - AimStrategy ya no recibe Player. Recibe PlayerState.
 *
 * ── FLUJO DE C ────────────────────────────────────────────────────────────
 *
 *   C presionado
 *       │
 *       ├── si ↓ y jugador sobre plataforma traversable → drop-through
 *       │     (evaluado por PlayerController, no aquí)
 *       │
 *       ├── verticalAim = ARRIBA/ABAJO/NONE según ↑/↓
 *       │
 *       └── dirección horizontal → mantenida (sin input de movimiento)
 *
 *   C no presionado → comportamiento clásico con todas las teclas.
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 *   AimSelection    → selecciona estrategia y aplica AimDirection en PlayerState
 *   AimStrategy     → calculadora pura de dirección dada la entrada
 *   PlayerState     → única fuente de verdad del estado de apuntado
 *   PlayerController → consume isAiming() para inhibir movimiento
 */
public class AimSelection {

    /**
     * Evalúa el input actual, selecciona la estrategia de apuntado correcta,
     * calcula la dirección y la escribe en {@code state}.
     *
     * <p>Este es el único punto de entrada del sistema de aim. Llamar una
     * vez por frame desde el ciclo de update del Player.
     *
     * @param state estado del jugador. Nunca null.
     */
    public static void apply(PlayerState state) {
        boolean up    = KeyBoard.getState("up");
        boolean down  = KeyBoard.getState("down");
        boolean left  = KeyBoard.getState("left");
        boolean right = KeyBoard.getState("right");
        boolean c     = KeyBoard.getState("c");

        AimStrategy strategy = selectStrategy(up, down, left, right, c, state);
        AimDirection result  = strategy.calculateDirection(state);
        state.setAimDirection(result.getDirection());
    }

    // ── Selección de estrategia ────────────────────────────────────────────

    private static AimStrategy selectStrategy(
            boolean up, boolean down,
            boolean left, boolean right,
            boolean c, PlayerState state) {

        if (c) {
            // Modo apuntado: C activo.
            // La dirección horizontal se mantiene; solo se actualiza vertical.
            return buildAimingStrategy(up, down, state);
        }

        // Modo normal: todas las teclas participan en la dirección.
        double dx = 0;
        double dy = 0;
        if (up)    dy -= 1;
        if (down)  dy += 1;
        if (left)  dx -= 1;
        if (right) dx += 1;

        if (dx == 0 && dy == 0) {
            return new AidleStrategy();
        }

        final double fdx = dx;
        final double fdy = dy;

        return buildMovingStrategy(fdx, fdy);
    }

    /**
     * Estrategia para modo apuntado (C presionado).
     * Solo actualiza el eje vertical; la dirección horizontal se preserva
     * del estado actual del Player (última dirección mirando).
     */
    private static AimStrategy buildAimingStrategy(
            boolean up, boolean down, PlayerState state) {

        return playerState -> {
            // Actualizar eje vertical
            if (up && !down) {
                playerState.setVerticalAim(PlayerState.VerticalAim.ARRIBA);
            } else if (down && !up) {
                playerState.setVerticalAim(PlayerState.VerticalAim.ABAJO);
            } else {
                playerState.setVerticalAim(PlayerState.VerticalAim.NONE);
            }

            // Dirección horizontal: mantener la última conocida
            double hx = playerState.isDer() ? 1.0 : -1.0;

            double vy = switch (playerState.getVerticalAim()) {
                case ARRIBA -> -1.0;
                case ABAJO  ->  1.0;
                case NONE   ->  0.0;
            };

            return new AimDirection(hx, vy);
        };
    }

    /**
     * Estrategia para movimiento normal (C no presionado, hay input).
     */
    private static AimStrategy buildMovingStrategy(double dx, double dy) {
        return playerState -> {
            if (dx != 0) {
                playerState.setDer(dx > 0);
            }
            if (dy < 0) {
                playerState.setVerticalAim(PlayerState.VerticalAim.ARRIBA);
            } else if (dy > 0) {
                playerState.setVerticalAim(PlayerState.VerticalAim.ABAJO);
            } else {
                playerState.setVerticalAim(PlayerState.VerticalAim.NONE);
            }
            return new AimDirection(dx, dy);
        };
    }
}
