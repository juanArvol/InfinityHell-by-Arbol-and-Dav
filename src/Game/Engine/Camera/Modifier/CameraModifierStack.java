package Game.Engine.Camera.Modifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Pila de modificadores activos de la cámara.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * CameraModifierStack gestiona la colección de modificadores activos,
 * los actualiza cada tick, los aplica en orden al CameraState y elimina
 * los expirados automáticamente.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * No thread-safe. Todas las operaciones deben ocurrir en el game loop thread.
 *
 * ── EJEMPLO DE USO ────────────────────────────────────────────────────────
 *   CameraModifierStack modifiers = new CameraModifierStack();
 *
 *   // Desde gameplay: añadir shake al recibir daño:
 *   modifiers.add(ShakeModifier.impact(8.0f, 20));
 *
 *   // Añadir zoom temporal para habilidad especial:
 *   modifiers.add(new ZoomModifier(1.5f, 30, 0.1f));
 *
 *   // En el game loop (CameraSystem.update()):
 *   CameraState state = new CameraState();
 *   modifiers.applyAll(state);
 *   // Aplicar state a GameCamera...
 */
public final class CameraModifierStack {

    private final List<CameraModifier> modifiers = new ArrayList<>();

    /** CameraState reutilizable para evitar allocations por frame. */
    private final CameraState sharedState = new CameraState();

    // ── Gestión de modificadores ──────────────────────────────────────────

    /**
     * Añade un modificador a la pila.
     * Si ya existe un modificador del mismo typeId y replaceExisting es true,
     * lo reemplaza en lugar de apilarlo.
     *
     * @param modifier       el modificador a añadir
     * @param replaceExisting si true, reemplaza modificadores del mismo tipo
     */
    public void add(CameraModifier modifier, boolean replaceExisting) {
        if (replaceExisting) {
            modifiers.removeIf(m -> m.getTypeId().equals(modifier.getTypeId()));
        }
        modifiers.add(modifier);
    }

    /**
     * Añade un modificador apilándolo sobre los existentes del mismo tipo.
     */
    public void add(CameraModifier modifier) {
        add(modifier, false);
    }

    /**
     * Elimina todos los modificadores del tipo dado.
     */
    public void remove(String typeId) {
        modifiers.removeIf(m -> m.getTypeId().equals(typeId));
    }

    /** Elimina todos los modificadores activos. */
    public void clear() { modifiers.clear(); }

    /** True si hay algún modificador activo. */
    public boolean hasModifiers() { return !modifiers.isEmpty(); }

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    /**
     * Actualiza todos los modificadores y elimina los expirados.
     * Llamar una vez por frame ANTES de applyAll().
     *
     * @param deltaTime tiempo transcurrido desde el último frame en segundos
     */
    public void update(double deltaTime) {
        modifiers.removeIf(m -> {
            m.update(deltaTime);
            return m.isExpired();
        });
    }

    /**
     * Aplica todos los modificadores activos al CameraState dado.
     * El estado se acumula: cada modificador suma/multiplica sobre los anteriores.
     *
     * @param state estado a modificar (debe estar reseteado antes de llamar)
     */
    public void applyAll(CameraState state) {
        for (CameraModifier modifier : modifiers) {
            if (!modifier.isExpired()) {
                modifier.apply(state);
            }
        }
    }

    /**
     * Conveniencia: actualiza, aplica al estado compartido y lo retorna.
     * El estado compartido es reutilizado — no guardar la referencia entre frames.
     *
     * @param deltaTime tiempo transcurrido desde el último frame en segundos
     * @return CameraState con los efectos de todos los modificadores activos.
     */
    public CameraState computeState(double deltaTime) {
        update(deltaTime);
        sharedState.reset();
        applyAll(sharedState);
        return sharedState;
    }

    public int size() { return modifiers.size(); }
}
