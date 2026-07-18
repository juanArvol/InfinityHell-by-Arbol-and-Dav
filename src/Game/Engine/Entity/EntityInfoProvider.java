package Game.Engine.Entity;

import Game.Engine.AbstractEntity;
import Game.Engine.Entity.Attributes.EntityAttributes;
import Game.Engine.Entity.Combat.AttackSources;
import Game.Engine.Entity.Flags.EntityFlags;
import Game.Engine.Entity.Stats.EntityStats;
import Game.Engine.Entity.Stats.RuntimeStats;

/**
 * Contrato formal de cualquier objeto del juego que represente una entidad viva.
 *
 * ── HRFC-0013 — Consolidación Arquitectónica del Dominio Entity ───────────
 *
 * PROBLEMA QUE RESUELVE:
 *   Antes de este HRFC, la referencia {@code entity instanceof Living} aparecía
 *   en comentarios de RuntimeStats y StatusEffectComponent como el patrón
 *   canónico para que un StatusEffect pudiera revocar sus contribuciones al
 *   expirar. Sin embargo, la interfaz Living no existía en ningún lugar del
 *   codebase — era un contrato prometido pero no implementado.
 *
 *   Sin Living, un StatusEffect que modifica stats de una entidad tenía que
 *   hacer {@code instanceof Enemy} — acoplando el sistema de efectos a un
 *   tipo concreto. Imposible extender a Player, NPC, invocaciones o compañeros
 *   sin añadir más ramas instanceof.
 *
 * QUÉ ES Living:
 *   Living es el contrato mínimo que cualquier objeto debe cumplir para
 *   participar en el sistema RPG de estadísticas del Engine:
 *
 *   - Tiene estadísticas base           → getStats()
 *   - Tiene estadísticas efectivas      → getRuntimeStats()
 *   - Tiene flags de estado booleanos   → getFlags()
 *   - Tiene atributos de dominio        → getAttributes()
 *   - Tiene fuentes de ataque           → getAttackSources()
 *
 * QUÉ NO ES Living:
 *   Living NO extiende GameObjects, Entity ni ninguna clase del engine.
 *   Es una interfaz pura de dominio. Cualquier clase puede implementarla
 *   independientemente de su posición en la jerarquía de herencia.
 *
 * USO EN StatusEffects:
 *   Con esta interfaz, el patrón canónico prometido en los comentarios
 *   de RuntimeStats ahora funciona realmente:
 *
 *     {@code @Override}
 *     {@code public void onExpire(GameObjects entity) {}
 *         if (entity instanceof Living living) {
 *             living.getRuntimeStats().revoke(this);
 *         }
 *     }}
 *
 *   El sistema de efectos nunca necesita saber si la entidad es Enemy,
 *   Player, NPC o Summon — solo necesita saber si es Living.
 *
 * IMPLEMENTADORES ACTUALES:
 *   - {@link Game.Enemys.Core.Enemy} — única entidad con sistema RPG completo.
 *
 * IMPLEMENTADORES FUTUROS:
 *   - Player (cuando migre a EntityStats/RuntimeStats)
 *   - NPC, Companion, Summon, Boss (heredan la implementación via Enemy o su propia)
 *
 * RELACIÓN CON Entity (clase):
 *   {@link AbstractEntity} gestiona HealthComponent y StatusEffectComponent — ciclo
 *   de vida de salud y efectos de estado. Living gestiona el sistema RPG de
 *   estadísticas. Son capas ortogonales: una entidad puede tener salud sin
 *   tener stats RPG (Bullet, WorldItem), y una entidad puede tener stats RPG
 *   sin ser una entidad del engine (futuro: objetos de mundo con stats).
 */
public interface EntityInfoProvider {

    /**
     * Estadísticas base de la entidad.
     *
     * <p>Contiene los valores permanentes configurados por el Assembler:
     * velocidad, daño, defensa, rangos, cooldowns, resistencias, percepción.
     *
     * <p>En combate, leer desde {@link #getRuntimeStats()} para obtener
     * los valores efectivos con todos los modificadores aplicados.
     *
     * @return estadísticas base. Nunca null.
     */
    EntityStats getStats();

    /**
     * Estadísticas efectivas en tiempo de ejecución.
     *
     * <p>Combina {@link #getStats()} con todos los modificadores activos
     * (StatusEffects, fases, buffs, debuffs). Es la fuente de verdad para
     * cualquier cálculo de combate: daño, velocidad efectiva, cooldowns reales.
     *
     * <p>Usar siempre en código de combate, AI y sistemas de gameplay.
     * Usar {@link #getStats()} solo desde Assemblers y configuración inicial.
     *
     * @return estadísticas efectivas. Nunca null.
     */
    RuntimeStats getRuntimeStats();

    /**
     * Flags de estado de la entidad.
     *
     * <p>Contiene capabilities (canMove, canAttack), states (invincible, flying),
     * impairments (stunned, frozen), damage flags (burning, poisoned) y
     * utility flags (stealthed, channeling).
     *
     * @return flags de estado. Nunca null.
     */
    EntityFlags getFlags();

    /**
     * Atributos de dominio de la entidad.
     *
     * <p>Describe QUÉ ES la entidad: facción, elemento, alineación, clase
     * narrativa y tier de dificultad. Ninguno de estos atributos implica
     * comportamiento concreto.
     *
     * @return atributos de dominio. Nunca null.
     */
    EntityAttributes getAttributes();

    /**
     * Fuentes de ataque disponibles para la entidad.
     *
     * <p>Determina de dónde proviene la capacidad de la entidad para generar
     * ataques: NATURAL, MAGIC, WEAPON, TECHNOLOGY, SUMMONING, CURSE, DIVINE,
     * MUTATION, ENVIRONMENTAL.
     *
     * @return fuentes de ataque. Nunca null.
     */
    AttackSources getAttackSources();
}
