package Game.Enemys.Core.Controllers;

import Game.Enemys.Core.Contracts.EnemyComponent;
import Game.Enemys.Core.Enemy;
import java.util.ArrayList;
import java.util.List;

/**
 * Registro de EnemyComponents opcionales del Enemy.
 *
 * Gestiona el ciclo de vida (onAttach, update, onDetach) de todos los
 * componentes del framework de enemigos.
 *
 * ── Diferencia con GameObjects.addComponent() ────────────────────────────
 * Game.Engine.Component pertenece al motor: física, colisiones, renderer.
 * EnemyComponent pertenece al framework de enemigos: aura, regeneración,
 * inmunidades, invocaciones, explosión al morir, etc.
 *
 * Ambos sistemas coexisten. Un Enemy tiene ambos tipos:
 *   - Components del engine (HealthComponent, ColliderComponent, etc.)
 *   - EnemyComponents del framework (RegenComponent, AuraComponent, etc.)
 *
 * ── Uso en assembler ─────────────────────────────────────────────────────
 *   enemy.getComponentRegistry().add(new RegenComponent(2), enemy);
 *   enemy.getComponentRegistry().add(new AuraComponent(30, Color.RED), enemy);
 *
 * ── Uso en fase ──────────────────────────────────────────────────────────
 *   // Activar inmunidad al fuego al entrar en fase 2:
 *   enemy.getComponentRegistry().add(new FireImmunityComponent(), enemy);
 *   // Quitarla al salir:
 *   enemy.getComponentRegistry().remove(FireImmunityComponent.class, enemy);
 */
public final class EnemyComponentRegistry {

    private final List<EnemyComponent> components = new ArrayList<>();

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    /**
     * Registra y adjunta un EnemyComponent.
     * Llama onAttach() inmediatamente.
     *
     * @param component el componente a añadir.
     * @param enemy     el Enemy propietario.
     */
    public void add(EnemyComponent component, Enemy enemy) {
        components.add(component);
        component.onAttach(enemy);
    }

    /**
     * Elimina todos los componentes del tipo indicado.
     * Llama onDetach() en cada uno.
     *
     * @param type  tipo del componente a eliminar.
     * @param enemy el Enemy propietario.
     */
    public void remove(Class<? extends EnemyComponent> type, Enemy enemy) {
        components.removeIf(c -> {
            if (type.isInstance(c)) {
                c.onDetach(enemy);
                return true;
            }
            return false;
        });
    }

    /**
     * Elimina todos los EnemyComponents.
     * Útil al hacer transición de fase cuando se quiere reconfigurar
     * completamente las capacidades del Enemy.
     *
     * @param enemy el Enemy propietario.
     */
    public void clear(Enemy enemy) {
        for (EnemyComponent c : components) {
            c.onDetach(enemy);
        }
        components.clear();
    }

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * Actualiza todos los componentes registrados.
     * Llamado por Enemy.update() cada frame.
     *
     * ── HRFC — Real DeltaTime Authority ──────────────────────────────────
     * Recibe deltaTime para propagarlo a cada EnemyComponent.
     *
     * @param enemy el Enemy propietario.
     * @param deltaTime tiempo real del simulation step en segundos
     */
    public void update(Enemy enemy, double deltaTime) {
        for (EnemyComponent c : components) {
            c.update(enemy, deltaTime);
        }
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    public <T extends EnemyComponent> T get(Class<T> type) {
        for (EnemyComponent c : components) {
            if (type.isInstance(c)) return type.cast(c);
        }
        return null;
    }

    /**
     * Devuelve true si existe al menos un componente del tipo indicado.
     * Alias semántico de has() — ambos nombres son válidos.
     *
     * @param type tipo del componente a buscar.
     */
    public boolean contains(Class<? extends EnemyComponent> type) {
        return get(type) != null;
    }

    /** @deprecated Usar contains() o get() != null. Mantenido por compatibilidad. */
    @Deprecated
    public boolean has(Class<? extends EnemyComponent> type) {
        return contains(type);
    }
}
