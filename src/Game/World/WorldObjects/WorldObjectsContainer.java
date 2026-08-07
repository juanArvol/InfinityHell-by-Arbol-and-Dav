package Game.World.WorldObjects;

import Game.Engine.GameObjects;
import Game.Engine.Physics.World.WorldSimulation;
import Game.Engine.Systems.CollisionsSystem;
import Game.Engine.Systems.StatusEffectSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Contenedor de objetos del mundo.
 *
 * ── HRFC-015 — World Simulation Core ──────────────────────────────────────
 *
 * Se integra WorldSimulation como sistema opcional en el pipeline de update().
 * Cuando está presente, se ejecuta en el orden correcto de la cadena causal:
 *
 *   1. object.update()          — componentes de motor (HealthComponent, physics…)
 *   2. StatusEffectSystem       — sincroniza ImpairmentFlags / DamageFlags
 *   3. WorldSimulation          ← NUEVO: Influences → Fields → Simulations → Interactions
 *   4. CollisionsSystem         — movimiento, colisiones físicas y triggers
 *
 * WorldSimulation se ejecuta ANTES de CollisionsSystem por dos razones:
 *   a) Las fuerzas acumuladas por campos vectoriales (VectorField) necesitan
 *      estar en Physics2D.accumulate() antes de que CollisionsSystem las integre
 *      en FASE 0.5 (flushAccumulatedForces).
 *   b) El InteractionRegistry resuelve relaciones físicas cuyo estado resultante
 *      puede ser interpretado por el Gameplay (StatusEffects, GameplayEvents)
 *      antes de que CollisionsSystem ejecute su frame.
 *
 * ── RETROCOMPATIBILIDAD TOTAL ─────────────────────────────────────────────
 * Todos los constructores existentes siguen funcionando sin cambios.
 * WorldSimulation es null por defecto — sin ningún impacto en el comportamiento
 * de mundos que no lo usen.
 *
 * Para activar la simulación del mundo, usar el constructor con WorldSimulation
 * o el setter setWorldSimulation():
 *
 *   // Constructor completo:
 *   new WorldObjectsContainer(collisions, statusEffects, WorldSimulation.withDefaults())
 *
 *   // Constructor default + setter (útil cuando la simulación se configura después):
 *   var container = new WorldObjectsContainer();
 *   container.setWorldSimulation(WorldSimulation.withDefaults());
 *
 * ── DESTROYABLE (sin cambios) ─────────────────────────────────────────────
 * La interfaz Destroyable y su mecánica de limpieza permanecen idénticas.
 */
public class WorldObjectsContainer {

    /**
     * Interfaz para objetos que tienen un ciclo de vida finito y pueden
     * auto-marcarse para remoción.
     *
     * ── MIGRACIÓN (ETAPA 3) ───────────────────────────────────────────────
     * Esta interfaz interna es ahora un alias de {@link Game.Engine.Destroyable}.
     * Bullet, Enemy y WorldItem pueden migrar progresivamente a implementar
     * la interfaz del Engine directamente, sin tocar este alias.
     *
     * @deprecated Implementar {@link Game.Engine.Destroyable} directamente.
     *             Este alias se elimina en Etapa 9.
     */
    @Deprecated(forRemoval = true)
    public interface Destroyable extends Game.Engine.Destroyable {
        // alias — sin métodos adicionales
    }

    // ── Estado ────────────────────────────────────────────────────────────────

    private final List<GameObjects> objects       = new ArrayList<>();
    private final List<GameObjects> pendingAdd    = new ArrayList<>();
    private final List<GameObjects> pendingRemove = new ArrayList<>();

    private final CollisionsSystem   collisionsSystem;
    private final StatusEffectSystem statusEffectSystem;

    /**
     * Núcleo de simulación del mundo (HRFC-015).
     * Null = inactivo (mundos sin simulación física).
     * Cuando está presente, se ejecuta entre StatusEffectSystem y CollisionsSystem.
     */
    private WorldSimulation worldSimulation;

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
     * Constructor por defecto — sin simulación del mundo activa.
     * Retrocompatible con todo el código existente.
     */
    public WorldObjectsContainer() {
        this.collisionsSystem   = new CollisionsSystem();
        this.statusEffectSystem = new StatusEffectSystem();
        this.worldSimulation    = null;
    }

    /**
     * Constructor con sistemas inyectados, sin simulación del mundo.
     * Usar para tests, configuraciones especiales o mundos sin física.
     *
     * @param collisionsSystem   sistema de colisiones; no puede ser null.
     * @param statusEffectSystem sistema de sincronización de flags; no puede ser null.
     */
    public WorldObjectsContainer(CollisionsSystem collisionsSystem,
                                  StatusEffectSystem statusEffectSystem) {
        if (collisionsSystem   == null) throw new IllegalArgumentException("collisionsSystem no puede ser null");
        if (statusEffectSystem == null) throw new IllegalArgumentException("statusEffectSystem no puede ser null");
        this.collisionsSystem   = collisionsSystem;
        this.statusEffectSystem = statusEffectSystem;
        this.worldSimulation    = null;
    }

    /**
     * Constructor completo con World Simulation Core (HRFC-015).
     *
     * @param collisionsSystem   sistema de colisiones; no puede ser null.
     * @param statusEffectSystem sistema de sincronización de flags; no puede ser null.
     * @param worldSimulation    núcleo de simulación del mundo; null = inactivo.
     */
    public WorldObjectsContainer(CollisionsSystem collisionsSystem,
                                  StatusEffectSystem statusEffectSystem,
                                  WorldSimulation worldSimulation) {
        if (collisionsSystem   == null) throw new IllegalArgumentException("collisionsSystem no puede ser null");
        if (statusEffectSystem == null) throw new IllegalArgumentException("statusEffectSystem no puede ser null");
        this.collisionsSystem   = collisionsSystem;
        this.statusEffectSystem = statusEffectSystem;
        this.worldSimulation    = worldSimulation;
    }

    /**
     * Constructor de compatibilidad con CollisionsSystem solo.
     * StatusEffectSystem se crea internamente. Sin simulación del mundo.
     *
     * @param collisionsSystem sistema de colisiones; no puede ser null.
     */
    public WorldObjectsContainer(CollisionsSystem collisionsSystem) {
        if (collisionsSystem == null) throw new IllegalArgumentException("collisionsSystem no puede ser null");
        this.collisionsSystem   = collisionsSystem;
        this.statusEffectSystem = new StatusEffectSystem();
        this.worldSimulation    = null;
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void update() {
        flush();

        // 1. Actualizar todos los objetos (Components: HealthComponent,
        //    StatusEffectComponent.tick() + onExpire(), Physics, Animaciones…)
        objectUpdater.accept(objects);

        // 2. StatusEffectSystem proyecta los flags derivados (ImpairmentFlags,
        //    DamageFlags) desde StatusEffectComponent. Debe ejecutarse DESPUÉS
        //    de que StatusEffectComponent.update() procesó los efectos del frame.
        //    CollisionsSystem usa isAbleToMove() en FASE 0 → los flags deben
        //    estar actualizados antes de que CollisionsSystem corra.
        statusEffectSystem.update(objects);

        // 3. World Simulation Core (HRFC-017) — opcional.
        //    Orden de la cadena causal interna:
        //      Influences → Fields → PhysicsSolver
        //
        //    Se ejecuta ANTES de CollisionsSystem porque:
        //      a) VectorField acumula fuerzas en Physics2D.accumulate(); CollisionsSystem
        //         las integra en FASE 0.5 (flushAccumulatedForces) en el mismo frame.
        //      b) PhysicsSolver resuelve el estado físico; el estado resultante
        //         es observado por Gameplay antes de que CollisionsSystem ejecute su frame.
        if (worldSimulation != null) {
            worldSimulation.update(objects);
        }

        // 4. Recopilar los pendientes de destrucción ANTES de pasar al sistema de
        //    colisiones. Un objeto que murió en su update() (enemy.onDeath()) ya
        //    está marcado aquí; no tiene sentido que reciba eventos de colisión
        //    en el mismo frame en que murió (doble loot, doble daño, etc.).
        for (GameObjects obj : objects) {
            if (obj instanceof Destroyable d && d.isPendingDestruction()) {
                pendingRemove.add(obj);
            }
        }

        // 5. Construir la lista de objetos vivos para CollisionsSystem.
        //    Si pendingRemove está vacío (caso más común), evitamos allocations.
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
     * Limpia el historial de contactos del CollisionsSystem.
     *
     * Llamar al entrar a un nuevo mundo / escena para evitar que FASE 4
     * (CollisionListener enter/stay/exit) genere eventos "exit" o "stay"
     * espurios en el primer frame — todos los contactos deben ser "enter".
     *
     * WorldManager lo llama automáticamente después de cada transición de mundo.
     */
    public void clearCollisionContactHistory() {
        collisionsSystem.clearContactHistory();
    }

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
        if (updater == null) {
            // Resetear al default en lugar de lanzar excepción
            this.objectUpdater = list -> list.forEach(GameObjects::update);
            return;
        }
        this.objectUpdater = updater;
    }

    /**
     * Establece o reemplaza el WorldSimulation activo (HRFC-015).
     *
     * Usar cuando la simulación del mundo se configure después de construir el
     * contenedor, o para intercambiar configuraciones entre mundos distintos.
     *
     * Pasar null para desactivar la simulación (mundos sin física, cutscenes).
     * Si se activa una nueva simulación al entrar a un mundo, llamar también a
     * worldSimulation.clearTransientState() para limpiar campos e influencias
     * que podrían haber persistido del mundo anterior.
     *
     * @param worldSimulation nueva instancia, o null para desactivar.
     */
    public void setWorldSimulation(WorldSimulation worldSimulation) {
        this.worldSimulation = worldSimulation;
    }

    /**
     * Acceso directo al WorldSimulation activo.
     * Usar desde código de gameplay para registrar campos, influencias e interacciones:
     *
     *   container.getWorldSimulation().fields().add(myField);
     *   container.getWorldSimulation().influences().add(influence, target);
     *
     * @return la instancia activa, o null si no hay simulación configurada.
     */
    public WorldSimulation getWorldSimulation() {
        return worldSimulation;
    }
}
