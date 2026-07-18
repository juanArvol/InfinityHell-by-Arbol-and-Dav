package Game.Engine.Entity.Stats;

import Game.Engine.Entity.Stats.Modifier.ModifierWriter;

/**
 * Contrato del dominio para cualquier objeto que aporte modificaciones
 * de estadísticas a una entidad viva.
 *
 * ── HRFC-012 — API declarativa mediante ModifierWriter ───────────────────
 *
 * CAMBIOS RESPECTO A HRFC-011:
 *
 *   ELIMINADO:
 *     StatModifier[] getContributions()
 *
 *   AÑADIDO:
 *     void contribute(ModifierWriter writer)
 *
 *   MOTIVACIÓN:
 *     Con getContributions(), toda la lógica de construcción de modificadores
 *     podía quedar dispersa entre constructores, arrays de instancia,
 *     inicializadores estáticos y campos recalculados en distintos momentos.
 *     El lector debía rastrear dónde y cuándo se construyó el array para
 *     entender qué aporta realmente el contributor.
 *
 *     Con contribute(ModifierWriter), toda esa lógica reside
 *     obligatoriamente dentro del propio método. Abrir un contributor
 *     y leer contribute() es suficiente para entender la totalidad de
 *     sus contribuciones. No hay que buscar nada más.
 *
 * ── Contrato ───────────────────────────────────────────────────────────────
 *   contribute(writer) declara las contribuciones llamando a writer.add().
 *   RuntimeStats crea el writer, llama contribute(), y añade los
 *   modificadores escritos al ModifierContainer usando el contributor
 *   como source para la posterior revocación.
 *
 *   Reglas:
 *   - No retener ni almacenar la referencia al writer fuera del método.
 *     El writer pertenece a RuntimeStats — su ciclo de vida es la llamada.
 *   - No llamar writer.add() fuera de contribute(). La semántica del método
 *     es "escribe ahora lo que aportas" — no "prepara para entregar después".
 *   - Si el contributor no tiene contribuciones activas, implementar con
 *     cuerpo vacío: la constante NO_CONTRIBUTIONS ya no es necesaria.
 *
 * ── Cómo implementar ──────────────────────────────────────────────────────
 *   Caso A — contribuciones fijas:
 *
 *       public class RageEffect implements StatContributor {
 *           private final double damage;
 *           private final double speedBonus;
 *
 *           @Override
 *           public void contribute(ModifierWriter writer) {
 *               writer.add(StatTarget.COMBAT_DAMAGE,  ModifierOperations.FLAT, damage);
 *               writer.add(StatTarget.MOVEMENT_SPEED, ModifierOperations.FLAT, speedBonus);
 *           }
 *       }
 *
 *   Caso B — sin contribuciones de stats (efecto solo de comportamiento):
 *
 *       public class StunEffect implements StatContributor {
 *           @Override
 *           public void contribute(ModifierWriter writer) {
 *               // sin contribuciones de stats
 *           }
 *       }
 *
 *   Caso C — contribuciones calculadas dinámicamente:
 *
 *       public class ScalingAura implements StatContributor {
 *           private int level;
 *
 *           public void setLevel(int level) { this.level = level; }
 *
 *           @Override
 *           public void contribute(ModifierWriter writer) {
 *               writer.add(StatTarget.COMBAT_DAMAGE, ModifierOperations.FLAT, level * 10.0);
 *           }
 *       }
 *
 *   Caso D — contribuciones compuestas (Freeze Power, Poison Power, etc.):
 *
 *       public class FreezeEffect implements StatContributor {
 *           private final double power;
 *           private final double slowAmount;
 *
 *           @Override
 *           public void contribute(ModifierWriter writer) {
 *               writer.add(StatTarget.FREEZE_POWER,   ModifierOperations.FLAT, power);
 *               writer.add(StatTarget.MOVEMENT_SPEED, ModifierOperations.FLAT, -slowAmount);
 *           }
 *       }
 *
 * ── Identidad y revocación ────────────────────────────────────────────────
 *   El mecanismo de revocación no cambia.
 *   RuntimeStats.revoke(contributor) elimina por identidad == todos los
 *   StatModifiers cuya fuente sea ese contributor — exactamente igual que
 *   antes. ModifierWriter construye cada StatModifier con el contributor
 *   como source, garantizando esta invariante de forma automática.
 *
 *   El contributor nunca gestiona su propia identidad: es RuntimeStats quien
 *   la mantiene. El contributor solo declara qué aporta.
 *
 * ── Sin complejidad accidental ────────────────────────────────────────────
 *   Esta interfaz es deliberadamente mínima. No introduce:
 *     - Identificadores (IDs, Strings, tokens, handles).
 *     - Mecanismos de registro o descubrimiento.
 *     - Ciclo de vida propio (apply/revoke los gestiona RuntimeStats).
 *     - Arrays, listas ni colecciones propias.
 *   El contributor declara qué aporta. El motor lo usa.
 *
 * @see RuntimeStats#apply(StatContributor)
 * @see RuntimeStats#revoke(StatContributor)
 * @see ModifierWriter
 */
public interface StatContributor {

    /**
     * Declara las contribuciones de este objeto al motor de estadísticas.
     *
     * <p>Llamar a {@code writer.add()} por cada modificador que este objeto
     * aporta. El writer construye cada {@link StatModifier} con {@code this}
     * como source, lo que garantiza que {@link RuntimeStats#revoke(StatContributor)}
     * pueda localizar y eliminar exactamente estas contribuciones.
     *
     * <p>Toda la lógica que determina qué y cuánto se aporta debe residir
     * dentro de este método. No preparar los valores en otro lugar y
     * delegarlos aquí — escribirlos directamente.
     *
     * <p>Si este contributor no tiene modificadores que aplicar, implementar
     * con cuerpo vacío.
     *
     * <p>No retener la referencia a {@code writer} fuera de este método.
     *
     * @param writer receptor de contribuciones. Nunca null.
     */
    void contribute(ModifierWriter writer);
}
