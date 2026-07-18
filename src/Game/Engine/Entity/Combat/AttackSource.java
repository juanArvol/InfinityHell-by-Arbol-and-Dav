package Game.Engine.Entity.Combat;

/**
 * Fuente de ataque de cualquier entidad viva.
 *
 * ── HRFC-007 — Generalización al Living Entity Core ──────────────────────
 * Movido desde Game.Enemys.Core.Combat a Game.Living.Combat.
 * AttackSource describe DE DÓNDE PROVIENE la capacidad de una entidad para
 * generar ataques. No describe armas, patrones ni cómo se ejecutan los ataques.
 *
 * ── Reutilización por tipo de entidad ────────────────────────────────────
 *
 *   Player     → MAGIC, WEAPON
 *   Enemy      → NATURAL, MAGIC, CURSE (según diseño)
 *   Boss       → MAGIC, NATURAL
 *   NPC        → WEAPON, TECHNOLOGY
 *   Summon     → DIVINE, NATURAL
 *   Turret     → TECHNOLOGY
 *   Companion  → WEAPON, MAGIC
 *
 * ── Por qué no es WeaponType ─────────────────────────────────────────────
 * WeaponType implica posesión de un arma concreta. Una entidad puede atacar
 * sin poseer ningún arma (Natural, Magic, Mutation, Environmental).
 * AttackSource es una capacidad ontológica, no un objeto.
 *
 * ── Extensión ─────────────────────────────────────────────────────────────
 * Añadir una nueva fuente = añadir una entrada aquí.
 * Ningún otro archivo del Living Core necesita modificarse.
 */
public enum AttackSource {

    /**
     * Ataques físicos del cuerpo: mordiscos, arañazos, golpes, embestidas.
     * Toda entidad orgánica que ataca cuerpo a cuerpo sin herramientas.
     */
    NATURAL,

    /**
     * Ataques originados en habilidades mágicas, hechizos o poderes sobrenaturales.
     * No requiere posesión de un arma física.
     */
    MAGIC,

    /**
     * Ataques ejecutados con un arma portada (espada, arco, pistola, etc.).
     * El arma concreta pertenece al módulo Weapon — esta fuente solo indica
     * que la entidad PUEDE portar y usar armas.
     */
    WEAPON,

    /**
     * Ataques de origen tecnológico: disparos mecánicos, láseres, torretas,
     * explosivos de fabricación artificial.
     */
    TECHNOLOGY,

    /**
     * Invocación de otras entidades como proyectiles o combatientes aliados.
     */
    SUMMONING,

    /**
     * Maldiciones, hexes y ataques que alteran el alma o la esencia del objetivo.
     */
    CURSE,

    /**
     * Poderes de origen divino o sagrado.
     */
    DIVINE,

    /**
     * Ataques derivados de mutación biológica: venenos, ácidos, espinas.
     */
    MUTATION,

    /**
     * Ataques del entorno controlados por la entidad: trampas, detonaciones,
     * corrientes eléctricas del terreno.
     */
    ENVIRONMENTAL
}
