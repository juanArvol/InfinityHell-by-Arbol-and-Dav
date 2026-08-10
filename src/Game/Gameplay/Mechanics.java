package Game.Gameplay;

import Game.Player.Player;

/**
 * Mecánicas de juego por frame.
 *
 * ── HRFC — Player Reengineering ───────────────────────────────────────────
 *
 * CAMBIOS:
 *   La lógica de aim y congelado que vivía aquí fue migrada a Player.update()
 *   y PlayerController.update() respectivamente.
 *
 *   Antes:
 *     AimSelection.getStrategy() → strategy.aim(player) → PlayerState
 *     KeyBoard.getState("c") → PlayerState.setCongelado()
 *
 *   Ahora (en Player.update()):
 *     AimSelection.apply(state) — aplica dirección directamente en PlayerState.
 *
 *   Ahora (en PlayerController.update()):
 *     KeyBoard.getState("c") → state.setAiming(true/false).
 *     congelado sigue siendo responsabilidad de sistemas externos (cutscenes,
 *     trampas) que llaman player.getState().setCongelado(true).
 *
 *   Esta clase se conserva por si en el futuro se necesita coordinación de
 *   mecánicas globales que no pertenezcan a ningún módulo específico.
 *
 * @deprecated El método updateMechanics() ya no tiene responsabilidades.
 *   Su lógica se migró a Player.update() (aim) y PlayerController.update() (C).
 *   Si se necesita lógica de mecánicas globales, añadirla aquí directamente.
 */
public class Mechanics {

    /**
     * @deprecated Vacío — la lógica fue migrada a Player.update() y
     *   PlayerController.update(). Ver javadoc de clase.
     */
    @Deprecated
    public static void updateMechanics(Player player) {
        // Aim: ahora en Player.update() → AimSelection.apply(state)
        // Congelado por tecla C: ahora en PlayerController.update()
        // Nada que hacer aquí.
    }
}
