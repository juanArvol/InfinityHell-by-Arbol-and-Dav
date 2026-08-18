package Game.Engine.Physics.SimulaticWorld.Fields;

import Game.Engine.GameObjects;
import java.util.ArrayList;
import java.util.List;

/**
 * Sistema de campos del mundo — aplica todos los WorldField activos
 * sobre los objetos presentes en el mundo cada frame.
 *
 * ── HRFC-015 — World Simulation Core ──────────────────────────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * WorldFieldSystem es el primer escalón de la cadena causal del World
 * Simulation Core:
 *
 *   Influences
 *       ↓
 *   [WorldFieldSystem]   ← aquí
 *       ↓
 *   Simulation Modules
 *       ↓
 *   Physical State
 *       ↓
 *   Interaction Registry
 *       ↓
 *   Gameplay
 *
 * Su responsabilidad única es:
 *   1. Mantener el registro de campos activos.
 *   2. Cada frame, hacer tick() en cada campo (actualizar posición, decrementar vida).
 *   3. Aplicar cada campo activo sobre todos los objetos del mundo.
 *   4. Eliminar los campos que han expirado.
 *
 * WorldFieldSystem NO evalúa consecuencias. No sabe qué sucede cuando un
 * objeto recibe calor, carga o una fuerza. Eso pertenece a las siguientes
 * capas de la simulación.
 *
 * ── CAMPOS Y OBJETOS ─────────────────────────────────────────────────────
 * WorldFieldSystem no conoce tipos concretos de campos ni de objetos.
 * Cada campo tiene un ScalarApplicator o VectorApplicator inyectado que
 * sabe cómo modificar los componentes del objeto destino. WorldFieldSystem
 * solo llama field.applyTo(object) por cada par (campo, objeto).
 *
 * ── GESTIÓN DE CAMPOS ────────────────────────────────────────────────────
 * Los campos se añaden y eliminan con add() / remove().
 * La expiración automática ocurre en update() cuando tick() retorna false.
 * No hay bucle de adición diferida — los campos se añaden en el siguiente update().
 * Si se necesita diferir adiciones (thread safety, callbacks), el WorldSimulation
 * orquestador puede gestionar la cola antes de llamar update().
 *
 * ── RENDIMIENTO ──────────────────────────────────────────────────────────
 * Complejidad: O(F × N) donde F = campos activos, N = objetos en el mundo.
 * En la práctica, los campos son pocos (típicamente 0–10) y los objetos
 * raramente superan los 200, por lo que el coste es despreciable.
 * Si en el futuro el número de campos crece significativamente, se puede
 * introducir particionamiento espacial (quadtree) sin cambiar la interfaz.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   WorldFieldSystem fieldSystem = new WorldFieldSystem();
 *
 *   // Añadir un campo térmico (piromante genera calor):
 *   fieldSystem.add(WorldFieldPresets.thermalField(x, y, 100, 20.0, pyromancer));
 *
 *   // Añadir un campo gravitacional de Sans:
 *   fieldSystem.add(WorldFieldPresets.gravityField(x, y, 200, 15.0, sans));
 *
 *   // En el game loop (WorldSimulation lo llama en orden):
 *   fieldSystem.update(worldObjects);
 */
public final class WorldFieldSystem {

    /** Campos activos. Se itera y se compacta en cada update(). */
    private final List<WorldField<?>> fields = new ArrayList<>();

    /** Buffer para campos a añadir entre frames (evita ConcurrentModification). */
    private final List<WorldField<?>> pendingAdd = new ArrayList<>();

    // ── API pública ───────────────────────────────────────────────────────

    /**
     * Registra un nuevo campo activo.
     * El campo se incorpora al inicio del siguiente update().
     *
     * @param field campo a registrar. No hace nada si null.
     */
    public void add(WorldField<?> field) {
        if (field != null) pendingAdd.add(field);
    }

    /**
     * Elimina un campo específico antes de que expire naturalmente.
     * Útil para cancelar campos persistentes (PERMANENT) al finalizar
     * un poder, una fase o una zona de efecto.
     *
     * @param field campo a eliminar.
     */
    public void remove(WorldField<?> field) {
        fields.remove(field);
        pendingAdd.remove(field);
    }

    /** Elimina todos los campos activos (cambio de mundo, reset de escena). */
    public void clear() {
        fields.clear();
        pendingAdd.clear();
    }

    /** Número de campos activos actualmente. */
    public int activeCount() {
        return fields.size() + pendingAdd.size();
    }

    /** True si no hay campos activos ni pendientes. */
    public boolean isEmpty() {
        return fields.isEmpty() && pendingAdd.isEmpty();
    }

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * Procesa todos los campos activos sobre la lista de objetos del mundo.
     *
     * Pasos por frame:
     *   1. Incorporar campos pendientes.
     *   2. Para cada campo activo:
     *      a. tick() — actualiza posición y vida.
     *      b. Si sigue vivo: applyTo(object) para cada objeto del mundo.
     *      c. Si expiró: marcarlo para eliminación.
     *   3. Compactar la lista eliminando los expirados.
     *
     * @param objects lista de objetos activos en el mundo este frame.
     */
    public void update(List<GameObjects> objects) {
        // 1. Incorporar campos pendientes
        if (!pendingAdd.isEmpty()) {
            fields.addAll(pendingAdd);
            pendingAdd.clear();
        }

        if (fields.isEmpty() || objects == null || objects.isEmpty()) return;

        // 2. Procesar campos — compactación in-place
        int alive = 0;
        for (int i = 0; i < fields.size(); i++) {
            WorldField<?> field = fields.get(i);

            boolean stillAlive = field.tick(1);

            if (stillAlive) {
                // Aplicar el campo sobre todos los objetos del mundo
                for (GameObjects obj : objects) {
                    field.applyTo(obj);
                }
                fields.set(alive++, field);
            }
            // Si no está vivo: se descarta (no se copia a la posición alive)
        }

        // 3. Eliminar el tail de campos expirados
        if (alive < fields.size()) {
            fields.subList(alive, fields.size()).clear();
        }
    }
}
