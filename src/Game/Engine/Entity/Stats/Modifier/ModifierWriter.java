package Game.Engine.Entity.Stats.Modifier;

import Game.Engine.Entity.Stats.StatContributor;
import Game.Engine.Entity.Stats.StatModifier;
import Game.Engine.Entity.Stats.StatTarget;

/**
 * Receptor de contribuciones de un StatContributor.
 *
 * ── HRFC-012 — API declarativa de contribuciones ─────────────────────────
 *
 * ModifierWriter es el único objeto que un StatContributor recibe en su
 * método contribute(). Su única responsabilidad es recoger las declaraciones
 * de contribución y construir los StatModifiers correspondientes.
 *
 * ── Uso desde un contributor ──────────────────────────────────────────────
 *
 *   @Override
 *   public void contribute(ModifierWriter writer) {
 *       writer.add(StatTarget.COMBAT_DAMAGE,  ModifierOperations.FLAT, damage);
 *       writer.add(StatTarget.FREEZE_POWER,   ModifierOperations.FLAT, freeze);
 *   }
 *
 * ── Lo que ModifierWriter NO es ───────────────────────────────────────────
 *   ✗ Builder     — no produce ningún objeto que el contributor reciba.
 *   ✗ Registry    — no almacena estado entre llamadas externas.
 *   ✗ Factory     — no crea objetos de dominio.
 *   ✗ Context     — no transporta estado del motor al contributor.
 *   ✗ Manager     — no tiene ciclo de vida propio.
 *
 * ── Identidad y revocación ────────────────────────────────────────────────
 *   Cada StatModifier se construye con el contributor como source.
 *   La revocación (RuntimeStats.revoke) sigue funcionando por identidad ==
 *   exactamente igual que antes — ModifierWriter no cambia ese mecanismo.
 *
 * ── Rendimiento ───────────────────────────────────────────────────────────
 *   RuntimeStats posee una única instancia reutilizable. Cada llamada a
 *   apply() invoca reset(contributor) para apuntar el writer al nuevo
 *   contributor, llama contribute(), drena el buffer al container y llama
 *   clear() para dejarlo listo para la siguiente vez. Cero allocations
 *   de ModifierWriter en cualquier punto del ciclo de vida de la entidad.
 */
public final class ModifierWriter {

    private static final int INITIAL_CAPACITY = 4;

    /** El contributor activo para esta sesión de escritura. Mutable para reutilización. */
    private StatContributor contributor;

    /** Modificadores acumulados durante la llamada a contribute(). */
    private StatModifier[] pending;
    private int            count;

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * Crea un writer vacío listo para reutilización.
     * Llamar {@link #reset(StatContributor)} antes de cada uso.
     */
    public ModifierWriter() {
        this.pending = new StatModifier[INITIAL_CAPACITY];
        this.count   = 0;
    }

    // ── Ciclo de reutilización — llamado por RuntimeStats ─────────────────

    /**
     * Prepara el writer para una nueva sesión de escritura.
     *
     * <p>Asocia el writer al contributor dado y descarta cualquier
     * contenido previo del buffer. Llamar antes de cada
     * {@link StatContributor#contribute(ModifierWriter)}.
     *
     * @param contributor contributor propietario de los modificadores
     *                    que se escribirán en esta sesión. Nunca null.
     */
    public void reset(StatContributor contributor) {
        this.contributor = contributor;
        this.count       = 0;
    }

    // ── API del contributor ───────────────────────────────────────────────

    /**
     * Declara una contribución con operación explícita y descripción.
     *
     * @param target      estadística objetivo.
     * @param operation   estrategia matemática (FLAT, MULTIPLIER, OVERRIDE...).
     * @param value       valor numérico del modificador.
     * @param description descripción para logs/debug. No afecta lógica.
     */
    public void add(StatTarget target,
                    ModifierOperation operation,
                    double value,
                    String description) {
        ensureCapacity();
        pending[count++] = new StatModifier(target, operation, value, contributor, description);
    }

    /**
     * Declara una contribución sin descripción textual.
     *
     * @param target    estadística objetivo.
     * @param operation estrategia matemática (FLAT, MULTIPLIER, OVERRIDE...).
     * @param value     valor numérico del modificador.
     */
    public void add(StatTarget target,
                    ModifierOperation operation,
                    double value) {
        add(target, operation, value, "");
    }

    // ── Acceso interno — solo RuntimeStats ───────────────────────────────

    /**
     * Número de modificadores declarados en esta sesión de escritura.
     * Usado por RuntimeStats para iterar los modificadores escritos.
     */
    public int count() {
        return count;
    }

    /**
     * Modificador en la posición {@code index} del buffer interno.
     * Usado por RuntimeStats para añadir cada modificador al container.
     *
     * <p>Solo válido para índices en el rango {@code [0, count())}.
     *
     * @param index posición en el buffer.
     * @return StatModifier en esa posición. Nunca null si el índice es válido.
     */
    public StatModifier get(int index) {
        return pending[index];
    }

    // ── Interno ───────────────────────────────────────────────────────────

    /** Duplica la capacidad del buffer si está lleno. */
    private void ensureCapacity() {
        if (count < pending.length) return;
        StatModifier[] grown = new StatModifier[pending.length * 2];
        System.arraycopy(pending, 0, grown, 0, count);
        pending = grown;
    }
}
