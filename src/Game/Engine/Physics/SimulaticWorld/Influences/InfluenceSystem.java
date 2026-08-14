package Game.Engine.Physics.SimulaticWorld.Influences;

import Game.Engine.GameObjects;
import java.util.ArrayList;
import java.util.List;

/**
 * Sistema de influencias — procesa todas las Influence activas cada frame.
 *
 * ── HRFC-015 — World Simulation Core ──────────────────────────────────────
 *
 * ── POSICIÓN EN LA CADENA CAUSAL ─────────────────────────────────────────
 * InfluenceSystem es el primer eslabón ejecutado en WorldSimulation:
 *
 *   [InfluenceSystem]    ← primero: modificaciones directas sobre targets
 *       ↓
 *   WorldFieldSystem     ← campos espaciales
 *       ↓
 *   SimulationModules    ← transferencias entre entidades
 *       ↓
 *   PhysicalState        ← estado actualizado
 *       ↓
 *   InteractionRegistry  ← evaluación de consecuencias
 *       ↓
 *   Gameplay
 *
 * Las Influences se procesan primero porque representan modificaciones
 * directas e intencionales sobre propiedades del mundo (poderes, magias,
 * habilidades). Los campos y simulaciones actúan después, sobre el estado
 * ya modificado por las influencias.
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * InfluenceSystem gestiona el ciclo de vida de los InfluenceBinding:
 *   1. Recibe bindings (add).
 *   2. Cada frame: tick() → si sigue activo: apply(target).
 *   3. Si tick() retorna false: onExpire(target) y descarta el binding.
 *
 * InfluenceSystem NO sabe qué propiedad modifica cada Influence.
 * Solo sabe que hay un conjunto de pares (Influence, target) que procesar.
 *
 * ── REGISTRO ─────────────────────────────────────────────────────────────
 * Los bindings se añaden con add(). Las adiciones se difieren al inicio del
 * siguiente update() para evitar ConcurrentModificationException si una
 * Influence añade otra Influence durante apply().
 *
 * ── ELIMINACIÓN MANUAL ────────────────────────────────────────────────────
 * Un binding puede eliminarse antes de que expire mediante remove(binding).
 * Esto llama onExpire(target) en la influencia para que limpie su estado.
 * Útil para cancelar influencias persistentes al finalizar una habilidad.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   InfluenceSystem influenceSystem = new InfluenceSystem();
 *
 *   // Registrar una influencia directa (p.ej. maldición de frío sobre un target):
 *   influenceSystem.add(new ColdDrainInfluence(30), target);
 *
 *   // O usando un InfluenceBinding explícito:
 *   influenceSystem.add(InfluenceBinding.of(influence, target));
 *
 *   // En el game loop (WorldSimulation lo llama en orden):
 *   influenceSystem.update();
 */
public final class InfluenceSystem {

    /** Bindings activos. Iterados y compactados en cada update(). */
    private final List<InfluenceBinding> bindings    = new ArrayList<>();

    /** Buffer de adiciones diferidas. Vaciado al inicio de update(). */
    private final List<InfluenceBinding> pendingAdd  = new ArrayList<>();

    /** Buffer de eliminaciones diferidas. Vaciado al inicio de update(). */
    private final List<InfluenceBinding> pendingRemove = new ArrayList<>();

    // ── API pública ───────────────────────────────────────────────────────

    /**
     * Registra un InfluenceBinding para ser procesado a partir del próximo frame.
     *
     * @param binding binding a registrar. No hace nada si null.
     */
    public void add(InfluenceBinding binding) {
        if (binding != null) pendingAdd.add(binding);
    }

    /**
     * Atajo: crea un InfluenceBinding y lo registra.
     *
     * @param influence influencia a aplicar. No puede ser null.
     * @param target    objeto destino. No puede ser null.
     */
    public void add(Influence influence, GameObjects target) {
        add(InfluenceBinding.of(influence, target));
    }

    /**
     * Solicita la eliminación de un binding antes de que expire naturalmente.
     * onExpire(target) se llama en el próximo update() antes de descartarlo.
     *
     * @param binding binding a eliminar.
     */
    public void remove(InfluenceBinding binding) {
        if (binding != null) pendingRemove.add(binding);
    }

    /** Elimina todos los bindings activos, llamando onExpire en cada uno. */
    public void clear() {
        for (InfluenceBinding b : bindings) {
            b.getInfluence().onExpire(b.getTarget());
        }
        bindings.clear();
        pendingAdd.clear();
        pendingRemove.clear();
    }

    /** Número de bindings activos actualmente. */
    public int activeCount() {
        return bindings.size() + pendingAdd.size();
    }

    /** True si no hay bindings activos ni pendientes. */
    public boolean isEmpty() {
        return bindings.isEmpty() && pendingAdd.isEmpty();
    }

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * Procesa todos los bindings activos en este frame.
     *
     * Pasos:
     *   1. Incorporar adiciones pendientes.
     *   2. Procesar eliminaciones manuales solicitadas.
     *   3. Para cada binding: tick() → si activo: apply(target).
     *      Si expiró: onExpire(target) y descartar.
     *   4. Compactar la lista.
     */
    public void update() {
        // 1. Adiciones diferidas
        if (!pendingAdd.isEmpty()) {
            bindings.addAll(pendingAdd);
            pendingAdd.clear();
        }

        // 2. Eliminaciones manuales
        if (!pendingRemove.isEmpty()) {
            for (InfluenceBinding toRemove : pendingRemove) {
                if (bindings.remove(toRemove)) {
                    toRemove.getInfluence().onExpire(toRemove.getTarget());
                }
            }
            pendingRemove.clear();
        }

        if (bindings.isEmpty()) return;

        // 3. Procesar — compactación in-place
        int alive = 0;
        for (int i = 0; i < bindings.size(); i++) {
            InfluenceBinding binding    = bindings.get(i);
            Influence   influence  = binding.getInfluence();
            GameObjects target     = binding.getTarget();

            boolean active = influence.tick();

            if (active) {
                influence.apply(target);
                bindings.set(alive++, binding);
            } else {
                influence.onExpire(target);
                // No se copia — se descarta
            }
        }

        // 4. Eliminar tail de bindings expirados
        if (alive < bindings.size()) {
            bindings.subList(alive, bindings.size()).clear();
        }
    }
}
