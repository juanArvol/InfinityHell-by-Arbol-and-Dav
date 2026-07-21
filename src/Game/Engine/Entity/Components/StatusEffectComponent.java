package Game.Engine.Entity.Components;

import Game.Engine.Component;
import Game.Engine.GameObjects;

/**
 * Componente de efectos de estado — gestiona efectos activos sobre cualquier entidad.
 *
 * ── HRFC-011 — Integración con StatContributor ────────────────────────────
 *
 * ── Desacoplamiento Engine ↔ Living ──────────────────────────────────────
 *   StatusEffectComponent vive en Game.Engine.Components y NO puede importar
 *   Game.Living.* (eso crearía una dependencia descendente del engine hacia
 *   el juego). La integración con el sistema de stats se hace mediante el
 *   contrato StatusEffect.onExpire(GameObjects), que el efecto concreto
 *   implementa y en cuya implementación SÍ puede importar Game.Living.Stats.
 *
 *   Flujo con StatContributor (HRFC-011):
 *     1. El StatusEffect concreto implementa StatContributor y declara sus
 *        contribuciones como un array de StatModifier (con 'this' como source).
 *     2. En tick() (o al activarse), el efecto llama:
 *            entity.getRuntimeStats().apply(this)
 *        una única vez. El motor registra las contribuciones por identidad
 *        de referencia — no se necesita ningún handle ni token.
 *     3. Cuando tick() devuelve false (efecto expirado), StatusEffectComponent
 *        llama onExpire(entity) antes de eliminar el efecto.
 *     4. En onExpire() el efecto revoca sus contribuciones:
 *            entity.getRuntimeStats().revoke(this)
 *     5. StatusEffectComponent no conoce nada de esto — pura inversión
 *        de dependencias. El engine no importa Game.Living.
 *
 * ── Stacking ──────────────────────────────────────────────────────────────
 *   Por defecto, efectos del mismo tipo se acumulan. Si no deben stackearse,
 *   el efecto concreto puede llamar component.removeAll(MiEfecto.class) antes
 *   de añadirse, o verificar internamente en tick().
 *
 * ── API pública ───────────────────────────────────────────────────────────
 *   component.add(effect)                    — añade un efecto.
 *   component.removeAll(MiEfecto.class)      — elimina todos los efectos del tipo.
 *   component.hasEffect(MiEfecto.class)      — consulta tipada.
 *   component.activeCount()                  — número de efectos activos.
 *   component.clearAll()                     — elimina todos los efectos.
 *
 * ── Uso típico ────────────────────────────────────────────────────────────
 *   // En constructor de Enemy, Player, etc.:
 *   addComponent(new StatusEffectComponent());
 *
 *   // Aplicar efecto (código de gameplay, puede importar Living):
 *   StatusEffectComponent fx = entity.getComponent(StatusEffectComponent.class);
 *   if (fx != null) fx.add(new RageEffect());
 *
 *   // Consulta tipada (sin Strings):
 *   boolean enraged = fx != null && fx.hasEffect(RageEffect.class);
 */
public class StatusEffectComponent extends Component {

    /**
     * Interfaz que implementa cada efecto de estado concreto.
     *
     * <p>Los efectos concretos viven en paquetes de gameplay (Game.Game.*,
     * Game.Living.*) y pueden importar cualquier tipo necesario. Esta
     * interfaz, al estar en Game.Engine, no impone ninguna dependencia
     * hacia el gameplay.
     */
    public interface StatusEffect {

        /**
         * Procesa un tick del efecto sobre la entidad.
         *
         * <p>El efecto es responsable de aplicar sus modificaciones
         * (stats, flags, daño, etc.) sobre la entidad en cada tick.
         * Los efectos que modifican stats implementan StatContributor y
         * aplican sus contribuciones llamando a runtimeStats.apply(this)
         * una única vez (normalmente en el primer tick o al activarse).
         *
         * @param entity entidad sobre la que actúa el efecto.
         * @return {@code true} si el efecto sigue activo; {@code false} si expiró.
         */
        boolean tick(GameObjects entity);

        /**
         * Llamado por StatusEffectComponent justo antes de eliminar el efecto
         * cuando tick() ha devuelto {@code false}.
         *
         * <p>Aquí el efecto debe revertir todos sus cambios. Los efectos que
         * implementan StatContributor revocan sus contribuciones:
         *     runtimeStats.revoke(this)
         * Los efectos con flags u otros efectos secundarios también los limpian aquí.
         *
         * <p>La implementación por defecto no hace nada. Los efectos que no
         * necesitan limpieza pueden omitir este método.
         *
         * @param entity entidad sobre la que actúa el efecto.
         */
        default void onExpire(GameObjects entity) {}
    }

    // ── Almacenamiento — array dinámico manual para evitar Iterator alloc ──

    private static final int INITIAL_CAPACITY = 4;

    private StatusEffect[] effects;
    private int            size;

    // ── Constructor ───────────────────────────────────────────────────────

    public StatusEffectComponent() {
        this.effects = new StatusEffect[INITIAL_CAPACITY];
        this.size    = 0;
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    /**
     * Actualiza todos los efectos activos.
     * Los efectos que devuelven false en tick() son eliminados después de
     * llamar a onExpire().
     * Compacta el array in-place: cero allocations por frame.
     */
    @Override
    public void update() {
        if (size == 0 || gameObject == null) return;

        int alive = 0;
        for (int i = 0; i < size; i++) {
            StatusEffect effect = effects[i];
            boolean active = effect.tick(gameObject);
            if (active) {
                effects[alive++] = effect;
            } else {
                // Notificar expiración antes de descartar
                effect.onExpire(gameObject);
                effects[i] = null; // liberar referencia para GC
            }
        }
        size = alive;
    }

    // ── API ───────────────────────────────────────────────────────────────

    /**
     * Añade un efecto de estado.
     *
     * @param effect efecto a añadir. No hace nada si null.
     */
    public void add(StatusEffect effect) {
        if (effect == null) return;
        ensureCapacity();
        effects[size++] = effect;
    }

    /**
     * Elimina todos los efectos del tipo dado, llamando onExpire() en cada uno.
     * Coste: O(n) donde n = efectos activos (típicamente 0–5).
     *
     * @param type clase del tipo de efecto a eliminar.
     */
    public <T extends StatusEffect> void removeAll(Class<T> type) {
        if (type == null || size == 0) return;
        int alive = 0;
        for (int i = 0; i < size; i++) {
            StatusEffect effect = effects[i];
            if (type.isInstance(effect)) {
                if (gameObject != null) effect.onExpire(gameObject);
                effects[i] = null;
            } else {
                effects[alive++] = effects[i];
            }
        }
        size = alive;
    }

    /**
     * True si hay al menos un efecto activo del tipo dado.
     * Consulta completamente tipada — cero Strings.
     *
     * @param type clase del tipo de efecto.
     */
    public <T extends StatusEffect> boolean hasEffect(Class<T> type) {
        if (type == null) return false;
        for (int i = 0; i < size; i++) {
            if (type.isInstance(effects[i])) return true;
        }
        return false;
    }

    /**
     * Devuelve el primer efecto activo del tipo dado, o null si no existe.
     * Útil para acceder a la instancia concreta y consultar su estado interno.
     *
     * @param type clase del tipo de efecto.
     */
    @SuppressWarnings("unchecked")
    public <T extends StatusEffect> T getEffect(Class<T> type) {
        if (type == null) return null;
        for (int i = 0; i < size; i++) {
            if (type.isInstance(effects[i])) return (T) effects[i];
        }
        return null;
    }

    /**
     * Devuelve el efecto en la posición {@code index} del array interno.
     * Acceso por índice para iteración sin allocation (StatusEffectSystem).
     *
     * @param index índice en [0, activeCount()).
     * @throws ArrayIndexOutOfBoundsException si index >= activeCount().
     */
    public StatusEffect getEffectAt(int index) {
        return effects[index];
    }

    /** Número de efectos activos. */
    public int activeCount() { return size; }

    /** True si no hay efectos activos. */
    public boolean isEmpty() { return size == 0; }

    /**
     * Elimina todos los efectos activos, llamando onExpire() en cada uno.
     */
    public void clearAll() {
        if (size == 0) return;
        for (int i = 0; i < size; i++) {
            if (gameObject != null) effects[i].onExpire(gameObject);
            effects[i] = null;
        }
        size = 0;
    }

    // ── Interno ───────────────────────────────────────────────────────────

    private void ensureCapacity() {
        if (size < effects.length) return;
        StatusEffect[] grown = new StatusEffect[effects.length * 2];
        System.arraycopy(effects, 0, grown, 0, size);
        effects = grown;
    }
}
