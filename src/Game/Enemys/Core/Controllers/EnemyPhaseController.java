package Game.Enemys.Core.Controllers;

import Game.Enemys.Core.Contracts.EnemyPhase;
import Game.Enemys.Core.Contracts.PhaseTransition;
import Game.Enemys.Core.Enemy;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de fases del Enemy.
 *
 * Gestiona el ciclo de vida de las fases: cuándo activar la siguiente,
 * llamar onEnter/onExit, y actualizar la fase activa cada frame.
 *
 * ── Las fases NO son enteros ──────────────────────────────────────────────
 * No existe phase = 2. Existe una EnemyPhase activa que define completamente
 * el comportamiento del enemigo en ese estado.
 *
 * ── Modelo de fases ──────────────────────────────────────────────────────
 * Las fases se registran en orden. Cada fase lleva asociada una
 * PhaseTransition que define cuándo salir de ella hacia la siguiente.
 * La última fase no necesita transición (es la fase final).
 *
 * El controlador evalúa la transición de la fase activa cada frame.
 * Si la condición se cumple, avanza automáticamente a la siguiente.
 *
 * ── Uso en assembler (enemigo simple, sin fases) ─────────────────────────
 * No llamar nada. El controlador sin fases es un no-op.
 *
 * ── Uso en assembler (Boss con 2 fases) ──────────────────────────────────
 *   EnemyPhaseController phases = enemy.getPhaseController();
 *   phases.addPhase(new Phase1(), new HealthThresholdTransition(0.5));
 *   phases.addPhase(new Phase2(), null); // fase final
 *   phases.start(enemy);                // activa Phase1 inmediatamente
 */
public final class EnemyPhaseController {

    private record PhaseEntry(EnemyPhase phase, PhaseTransition transition) {}

    private final List<PhaseEntry> phases = new ArrayList<>();
    private int     currentIndex  = -1;   // -1 = sin fases activas
    private boolean started       = false;

    // ── Registro ─────────────────────────────────────────────────────────

    /**
     * Registra una fase con su condición de transición.
     *
     * @param phase      la fase a registrar.
     * @param transition condición para avanzar a la siguiente fase;
     *                   null si es la fase final.
     */
    public void addPhase(EnemyPhase phase, PhaseTransition transition) {
        phases.add(new PhaseEntry(phase, transition));
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    /**
     * Activa la primera fase. Llamar una vez después de registrar todas las fases.
     * No hace nada si no hay fases registradas.
     *
     * @param enemy el Enemy propietario.
     */
    public void start(Enemy enemy) {
        if (phases.isEmpty()) return;
        started = true;
        transitionTo(0, enemy);
    }

    /**
     * Actualización por frame.
     * Evalúa la transición activa y avanza si la condición se cumple.
     * Llama update() en la fase activa.
     *
     * ── HRFC — Real DeltaTime Authority ──────────────────────────────────
     * Recibe deltaTime para propagarlo a la fase activa.
     *
     * @param enemy el Enemy propietario.
     * @param deltaTime tiempo real del simulation step en segundos
     */
    public void update(Enemy enemy, double deltaTime) {
        if (!started || currentIndex < 0) return;

        PhaseEntry current = phases.get(currentIndex);
        current.phase().update(enemy, deltaTime);

        // Evaluar condición de salida
        if (current.transition() != null
                && current.transition().shouldTransition(enemy)
                && currentIndex + 1 < phases.size()) {
            transitionTo(currentIndex + 1, enemy);
        }
    }

    /**
     * Fuerza una transición a la fase indicada por índice.
     * Útil para scripting de Bosses o eventos especiales.
     *
     * @param index índice de la fase destino.
     * @param enemy el Enemy propietario.
     */
    public void forcePhase(int index, Enemy enemy) {
        if (index < 0 || index >= phases.size()) return;
        transitionTo(index, enemy);
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    public boolean hasPhases()         { return !phases.isEmpty(); }
    public int     getCurrentIndex()   { return currentIndex; }
    public boolean isStarted()         { return started; }

    public EnemyPhase getCurrentPhase() {
        if (currentIndex < 0 || currentIndex >= phases.size()) return null;
        return phases.get(currentIndex).phase();
    }

    public boolean isInPhase(String phaseId) {
        EnemyPhase current = getCurrentPhase();
        return current != null && current.id().equals(phaseId);
    }

    // ── Interno ───────────────────────────────────────────────────────────

    private void transitionTo(int index, Enemy enemy) {
        // Salir de la fase actual
        if (currentIndex >= 0) {
            phases.get(currentIndex).phase().onExit(enemy);
        }

        currentIndex = index;

        // Entrar en la nueva fase
        phases.get(currentIndex).phase().onEnter(enemy);
    }
}
