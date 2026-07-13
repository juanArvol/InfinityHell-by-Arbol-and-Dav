package Game.World.WorldObjects;

import Game.Engine.GameObjects;
import Game.Engine.Systems.CollisionsSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
 * SOLUCIÓN — Destroyable:
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
 * REFACTOR — CollisionsSystem inyectable:
 *   PROBLEMA ANTERIOR:
 *     private final CollisionsSystem collisionsSystem = new CollisionsSystem();
 *     El sistema de colisiones era instanciado internamente sin punto de control
 *     externo. No era posible:
 *     - Inyectar un mock para tests.
 *     - Deshabilitar colisiones para mundos sin física (hub area, cutscene).
 *     - Usar una variante con configuración diferente (sin gravedad, sin sweepAABB).
 *
 *   SOLUCIÓN:
 *     Constructor secundario que acepta CollisionsSystem por parámetro.
 *     Constructor sin args mantiene el comportamiento original como conveniencia.
 *     No se rompe ningún caller existente.
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

    private final CollisionsSystem collisionsSystem;

    /**
     * Updater de objetos inyectable.
     *
     * Por defecto: obj.update() para cada objeto (comportamiento original).
     * Cuando World tiene un Player rastreado, sustituye este updater por uno
     * que pasa EnemyContext a los Enemy, sin que WorldObjectsContainer conozca
     * Player ni Enemy directamente.
     *
     * Uso desde World:
     *   objects.setObjectUpdater(list ->
     *       WorldEnemyUpdater.updateAll(list, player));
     */
    private Consumer<List<GameObjects>> objectUpdater =
        list -> list.forEach(GameObjects::update);

    // ── Constructores ─────────────────────────────────────────────────────────

    /**
     * Constructor por defecto — crea su propio CollisionsSystem.
     * Comportamiento original: retrocompatible con todo el código existente.
     */
    public WorldObjectsContainer() {
        this(new CollisionsSystem());
    }

    /**
     * Constructor con CollisionsSystem inyectado.
     *
     * Usar cuando se necesite:
     *   - Un mock de colisiones en tests.
     *   - Deshabilitar colisiones para mundos especiales (hub, cutscene).
     *   - Una variante con configuración diferente (sin gravedad, broadphase extendido).
     *
     * @param collisionsSystem sistema de colisiones a usar; no puede ser null.
     */
    public WorldObjectsContainer(CollisionsSystem collisionsSystem) {
        if (collisionsSystem == null) throw new IllegalArgumentException("collisionsSystem no puede ser null");
        this.collisionsSystem = collisionsSystem;
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void update() {
        flush();

        objectUpdater.accept(objects);

        // Recopilar los pendientes de destrucción ANTES de pasar al sistema de
        // colisiones. Un objeto que murió en su update() (enemy.onDeath()) ya
        // está marcado aquí; no tiene sentido que reciba eventos de colisión
        // en el mismo frame en que murió (doble loot, doble daño, etc.).
        for (GameObjects obj : objects) {
            if (obj instanceof Destroyable d && d.isPendingDestruction()) {
                pendingRemove.add(obj);
            }
        }

        // Construir la lista de objetos vivos para el sistema de colisiones.
        // Usar el mismo Set que pendingRemove evita instanciar una lista nueva
        // cada frame: si pendingRemove está vacío (el caso más común), la lista
        // activa ES objects y la comparación es barata.
        if (pendingRemove.isEmpty()) {
            collisionsSystem.update(objects);
        } else {
            java.util.List<GameObjects> alive = new java.util.ArrayList<>(objects);
            alive.removeAll(pendingRemove);
            collisionsSystem.update(alive);
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

    /**
     * Reemplaza el updater de objetos.
     * Llamar desde World cuando el player rastreado cambie, para que los
     * enemigos reciban EnemyContext correcto en cada update().
     *
     * El updater recibe la lista viva de objetos y es responsable de llamar
     * update() en cada uno (con o sin contexto según el tipo).
     *
     * @param updater consumer que actualiza todos los objetos de la lista.
     *                Nunca null; usar el default (obj.update()) si se quiere resetear.
     */
    public void setObjectUpdater(Consumer<List<GameObjects>> updater) {
        if (updater == null) throw new IllegalArgumentException("updater no puede ser null");
        this.objectUpdater = updater;
    }
}
