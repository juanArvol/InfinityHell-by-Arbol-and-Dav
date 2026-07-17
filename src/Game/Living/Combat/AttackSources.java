package Game.Living.Combat;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Contenedor de fuentes de ataque de cualquier entidad viva.
 *
 * ── HRFC-007 — Generalización al Living Entity Core ──────────────────────
 * Movido desde Game.Enemys.Core.Combat a Game.Living.Combat.
 * AttackSources no conoce Enemy, Player ni ningún tipo concreto.
 * Es utilizable por cualquier entidad que pueda atacar.
 *
 * ── Múltiples fuentes simultáneas ────────────────────────────────────────
 * Una entidad puede tener varias fuentes activas a la vez.
 * Las fuentes pueden añadirse y eliminarse en runtime: una fase podría
 * otorgar acceso a MAGIC que antes no tenía, o un efecto podría suprimir
 * temporalmente WEAPON si la entidad pierde su arma.
 *
 * ── Uso en assemblers ─────────────────────────────────────────────────────
 *   // Zombie
 *   sources.add(AttackSource.NATURAL);
 *
 *   // Sans
 *   sources.add(AttackSource.MAGIC).add(AttackSource.NATURAL);
 *
 *   // Soldado
 *   sources.add(AttackSource.WEAPON).add(AttackSource.NATURAL);
 *
 *   // Ángel guardián (futuro Companion)
 *   sources.add(AttackSource.DIVINE).add(AttackSource.MAGIC);
 *
 * ── Uso en fases o componentes ────────────────────────────────────────────
 *   // La entidad adquiere magia en fase 2
 *   entity.getAttackSources().add(AttackSource.MAGIC);
 *
 *   // La entidad pierde su arma
 *   entity.getAttackSources().remove(AttackSource.WEAPON);
 *
 * ── Uso en sistemas externos ──────────────────────────────────────────────
 *   // El sistema de loot comprueba si puede soltar armas
 *   if (entity.getAttackSources().has(AttackSource.WEAPON)) { ... }
 *
 *   // La IA distingue amenaza mágica vs física
 *   if (entity.getAttackSources().hasAny(AttackSource.MAGIC, AttackSource.CURSE)) { ... }
 */
public class AttackSources {

    private final EnumSet<AttackSource> sources = EnumSet.noneOf(AttackSource.class);

    // ── Constructores ─────────────────────────────────────────────────────

    /** Crea un contenedor vacío. */
    public AttackSources() {}

    /** Crea un contenedor con las fuentes iniciales indicadas. */
    public AttackSources(AttackSource first, AttackSource... rest) {
        sources.add(first);
        Collections.addAll(sources, rest);
    }

    // ── Gestión ───────────────────────────────────────────────────────────

    /**
     * Añade una fuente de ataque. No hace nada si ya existe.
     *
     * @param source fuente a añadir.
     * @return this, para encadenamiento fluido.
     */
    public AttackSources add(AttackSource source) {
        sources.add(source);
        return this;
    }

    /**
     * Elimina una fuente de ataque. No hace nada si no existe.
     *
     * @param source fuente a eliminar.
     * @return this, para encadenamiento fluido.
     */
    public AttackSources remove(AttackSource source) {
        sources.remove(source);
        return this;
    }

    /** Elimina todas las fuentes activas. */
    public AttackSources clear() {
        sources.clear();
        return this;
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /** Devuelve true si la entidad tiene la fuente indicada. */
    public boolean has(AttackSource source) {
        return sources.contains(source);
    }

    /** Devuelve true si la entidad tiene al menos una de las fuentes indicadas. */
    public boolean hasAny(AttackSource first, AttackSource... rest) {
        if (sources.contains(first)) return true;
        for (AttackSource s : rest) {
            if (sources.contains(s)) return true;
        }
        return false;
    }

    /** Devuelve true si la entidad tiene TODAS las fuentes indicadas. */
    public boolean hasAll(AttackSource first, AttackSource... rest) {
        if (!sources.contains(first)) return false;
        for (AttackSource s : rest) {
            if (!sources.contains(s)) return false;
        }
        return true;
    }

    /** Devuelve true si la entidad no tiene ninguna fuente de ataque. */
    public boolean isEmpty() {
        return sources.isEmpty();
    }

    /** Vista inmutable del conjunto de fuentes activas. */
    public Set<AttackSource> getAll() {
        return Collections.unmodifiableSet(sources);
    }
}
