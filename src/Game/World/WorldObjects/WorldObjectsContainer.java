package Game.World.WorldObjects;

import Game.Engine.GameObjects;
import Game.Engine.Systems.CollisionsSystem;

import java.util.ArrayList;
import java.util.List;

/**
 * Contenedor de objetos del mundo — refactorizado para eliminar instanceof Bullet.
 *
 * PROBLEMA ANTERIOR (violación Open/Closed):
 *   El contenedor detectaba `Bullet` con `instanceof` para eliminar balas muertas:
 *
 *     if (obj instanceof Bullet bullet) {
 *         if (!bullet.getBulletLife().isAlive()) pendingRemove.add(obj);
 *     }
 *
 *   Esto viola OCP: cada nuevo tipo destruible (granadas, trampas, partículas,
 *   efectos de fx) requeriría un nuevo instanceof aquí.
 *
 * SOLUCIÓN:
 *   Se introduce la interfaz `Destroyable` que cualquier objeto puede implementar.
 *   WorldObjectsContainer solo conoce `Destroyable`, no `Bullet`.
 *
 *   Bullet implementa Destroyable:
 *     public class Bullet extends GameObjects implements Destroyable {
 *         public boolean isPendingDestruction() { return !getBulletLife().isAlive(); }
 *     }
 *
 *   WorldItem ya tiene `isPendingRemoval()` — puede implementar Destroyable trivialmente.
 *
 * OPEN/CLOSED después del cambio:
 *   Agregar un nuevo tipo destruible = implementar Destroyable.
 *   No hay que tocar WorldObjectsContainer.
 *
 * COMPATIBILIDAD:
 *   Bullet existente solo necesita implementar la interfaz Destroyable.
 *   El comportamiento de limpieza es idéntico.
 */
public class WorldObjectsContainer {

    /**
     * Interfaz para objetos que tienen un ciclo de vida finito y pueden
     * auto-marcarse para remoción.
     *
     * Implementar en: Bullet, WorldItem (ya tiene isPendingRemoval()),
     * futuros Particle, Grenade, Trap, etc.
     */
    public interface Destroyable {
        /**
         * @return true si este objeto debe ser removido del mundo en el próximo flush.
         */
        boolean isPendingDestruction();
    }

    // ── Estado ────────────────────────────────────────────────────────────────

    private final List<GameObjects> objects       = new ArrayList<>();
    private final List<GameObjects> pendingAdd    = new ArrayList<>();
    private final List<GameObjects> pendingRemove = new ArrayList<>();

    private final CollisionsSystem collisionsSystem = new CollisionsSystem();

    // ── Update ────────────────────────────────────────────────────────────────

    public void update() {
        flush();

        for (GameObjects obj : objects) {
            obj.update();
        }

        collisionsSystem.update(objects);

        // Limpiar cualquier objeto Destroyable que se haya auto-marcado
        // OCP: no importa qué tipo concreto es — solo si implementa Destroyable.
        for (GameObjects obj : objects) {
            if (obj instanceof Destroyable d && d.isPendingDestruction()) {
                pendingRemove.add(obj);
            }
        }
    }

    public void flush() {
        if (!pendingAdd.isEmpty()) {
            objects.addAll(pendingAdd);
            pendingAdd.clear();
        }
        if (!pendingRemove.isEmpty()) {
            objects.removeAll(pendingRemove);
            pendingRemove.clear();
        }
    }

    public void add(GameObjects obj)    { pendingAdd.add(obj);    }
    public void remove(GameObjects obj) { pendingRemove.add(obj); }

    public List<GameObjects> getObjects() { return objects; }
}
