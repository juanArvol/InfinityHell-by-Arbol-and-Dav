package Game.Player;

import Inputs.KeyBoard;

/**
 * Controlador de input del jugador.
 *
 * ── HRFC — Player Reengineering ───────────────────────────────────────────
 *
 * CAMBIOS RESPECTO A LA VERSIÓN ANTERIOR:
 *
 *   AÑADIDO:
 *     - Manejo de tecla C (modo apuntado): inhibe movimiento horizontal normal
 *       mientras C está presionado y actualiza state.aiming.
 *     - Drop-through: cuando C + ↓ + en suelo, el jugador cae a través de
 *       la plataforma (drop-through incondicional; extensible en el futuro
 *       para solo plataformas one-way).
 *     - EntityFlags check: respeta isAbleToMove() para inhibición por
 *       efectos de estado (frozen, stunned, rooted) además de congelado.
 *
 *   SIN CAMBIOS:
 *     - No conoce Player directamente.
 *     - Recibe PlayerPhysics y PlayerState — solo lo que necesita.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 *   Input → PlayerController → PlayerPhysics / PlayerState
 *
 *   No conoce: Player, armas, Health, World, componentes ajenos.
 *
 * ── MODO APUNTADO (C) ─────────────────────────────────────────────────────
 *
 *   C presionado:
 *     1. state.setAiming(true)   → señal para AimSelection y otros sistemas.
 *     2. Movimiento horizontal   → inhibido (inputX = 0).
 *        El deslizamiento sigue operando: el jugador se frena gradualmente
 *        si estaba en movimiento cuando presionó C.
 *     3. Salto                   → inhibido.
 *     4. Drop-through            → evaluado si verticalAim == ABAJO y
 *        state.isEnElSuelo() == true.
 *
 *   C no presionado:
 *     state.setAiming(false) → comportamiento de movimiento normal.
 *
 * ── DROP-THROUGH ──────────────────────────────────────────────────────────
 *
 *   Condición: C presionado + ↓ apuntando + en suelo.
 *   Acción: physics.setOnGround(false) + physics.clearSurface()
 *           + state.setEnElSuelo(false).
 *
 *   El drop-through NO pertenece a AimStrategy — es una acción de movimiento
 *   contextual que el Controller evalúa usando el estado de aim.
 *
 *   El sistema de colisiones restablece onGround cuando el jugador vuelve a
 *   aterrizar. No se necesita ninguna lógica de "reactivar" aquí.
 *
 * ── ENTITYFLAGS ───────────────────────────────────────────────────────────
 *
 *   Si se inyecta EntityFlags en el constructor, el Controller respeta
 *   isAbleToMove() para inhibiciones por efectos de estado (frozen, stun…).
 *   Si entityFlags es null (constructor de retrocompatibilidad), solo se
 *   verifica state.isCongelado().
 */
public class PlayerController {

    private final PlayerPhysics                        physics;
    private final PlayerState                          state;

    /**
     * EntityFlags del Player para respetar impairments genéricos (frozen, stunned…).
     * Puede ser null — en ese caso solo se verifica state.isCongelado().
     */
    private final Game.Engine.Entity.Flags.EntityFlags entityFlags;

    // ── Constructores ─────────────────────────────────────────────────────

    /**
     * Constructor completo.
     *
     * @param physics     física del Player. No puede ser null.
     * @param state       estado del Player. No puede ser null.
     * @param entityFlags flags de la entidad para respetar impairments. Puede ser null.
     */
    public PlayerController(PlayerPhysics physics,
                            PlayerState state,
                            Game.Engine.Entity.Flags.EntityFlags entityFlags) {
        if (physics == null) throw new IllegalArgumentException("physics es requerido");
        if (state   == null) throw new IllegalArgumentException("state es requerido");
        this.physics     = physics;
        this.state       = state;
        this.entityFlags = entityFlags;
    }

    /**
     * Constructor de retrocompatibilidad sin EntityFlags.
     * Equivale a {@code PlayerController(physics, state, null)}.
     */
    public PlayerController(PlayerPhysics physics, PlayerState state) {
        this(physics, state, null);
    }

    // ── Update ────────────────────────────────────────────────────────────

    public void update() {
        // Prioridad 1: inhibición por sistema de juego (cutscene, trampa).
        if (state.isCongelado()) return;

        // Prioridad 2: inhibición por efectos de estado genéricos (frozen, stun…).
        if (entityFlags != null && !entityFlags.isAbleToMove()) return;

        // Tecla C: activar/desactivar modo apuntado antes de procesar movimiento.
        boolean cPressed = KeyBoard.getState("c");
        state.setAiming(cPressed);

        if (cPressed) {
            handleAimingMode();
        } else {
            handleMovementInput();
            handleJumpInput();
        }
    }

    // ── Modo apuntado (C) ─────────────────────────────────────────────────

    /**
     * Frame en modo apuntado: movimiento horizontal inhibido.
     * El deslizamiento sigue activo (moveX con inputX=0 aplica el slide).
     * Evalúa drop-through si hay intención de bajar.
     */
    private void handleAimingMode() {
        // inputX = 0 inhibe la aceleración activa; el slide hace el resto.
        physics.moveX(0, state.isEnElSuelo(), state.isRunning());

        // Drop-through: C + apuntando abajo + en el suelo.
        if (state.isMirandoAbajo() && state.isEnElSuelo()) {
            performDropThrough();
        }
    }

    /**
     * Ejecuta el drop-through: desactiva el contacto con el suelo.
     *
     * El sistema de colisiones restablece onGround automáticamente cuando el
     * jugador vuelve a aterrizar. No se necesita ninguna lógica de reversión.
     *
     * Extensión futura: antes de llamar a este método, se podría consultar
     * el tipo de superficie activa para restringir el drop-through solo a
     * plataformas one-way (una vez que CollisionsSystem exponga esa información).
     */
    private void performDropThrough() {
        physics.setOnGround(false);
        physics.clearSurface();
        state.setEnElSuelo(false);
    }

    // ── Movimiento normal ─────────────────────────────────────────────────

    private void handleMovementInput() {
        double inputX = 0;

        if (KeyBoard.getState("left")) {
            inputX = -1;
            state.setDer(false);
        }
        if (KeyBoard.getState("right")) {
            inputX = 1;
            state.setDer(true);
        }

        boolean running = KeyBoard.getState("shift");
        state.setRunning(running);

        physics.moveX(inputX, state.isEnElSuelo(), running);
    }

    private void handleJumpInput() {
        if (KeyBoard.getState("up") && state.isEnElSuelo()) {
            physics.jump(10);
            physics.setOnGround(false);
            physics.clearSurface();
            state.setEnElSuelo(false);
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public PlayerPhysics getPhysics() { return physics; }
}
