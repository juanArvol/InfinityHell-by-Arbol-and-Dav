package Game.Enemys.Core.Variables;

/**
 * Punto de extensión para variables exclusivas de cada familia de enemigos.
 *
 * ── HRFC-013 — Consolidación Definitiva del Dominio Entity ───────────────
 *
 * CAMBIO ARQUITECTÓNICO:
 *   EnemyVariables era un Map<String, Double> con claves String.
 *   Ese diseño permitía duplicar en EnemyVariables conceptos que ya
 *   pertenecen al dominio Entity (hp, speed, damage, defense...).
 *   El Map basado en Strings ha sido eliminado completamente.
 *
 * NUEVA RESPONSABILIDAD:
 *   EnemyVariables es ahora una clase base abstracta.
 *   Su único propósito es servir como contrato común para las variables
 *   que son exclusivas del comportamiento particular de un tipo de enemigo
 *   y que no tienen cabida en el dominio genérico Entity/*.
 *
 * ── Qué NO pertenece aquí ─────────────────────────────────────────────────
 *
 *   hp, maxHp, speed, damage, defense, attackRange, attackCooldown,
 *   resistances, visionRange, critChance, regen, flags, attributes...
 *
 *   Todos esos conceptos pertenecen exclusivamente a:
 *     Entity.Stats.EntityStats  → valores base
 *     Entity.Stats.RuntimeStats → valores efectivos con modificadores
 *     Entity.Flags.EntityFlags  → flags booleanos
 *
 * ── Qué SÍ pertenece aquí ────────────────────────────────────────────────
 *
 *   Variables cuya existencia depende completamente de una implementación
 *   concreta y que no tendría sentido añadir al Engine genérico.
 *
 *   Ejemplos:
 *
 *   SansVariables:
 *     teleportFrames, autoDodgeCooldown, invulnerabilityFrames
 *
 *   SpiderVariables:
 *     webLifetime, maxAttachedThreads, jumpCount
 *
 *   NecromancerVariables:
 *     maxSummons, summonInterval, graveRadius
 *
 *   DragonVariables:
 *     fireBreathDuration, wingFlapForce, landingDelay
 *
 * ── Jerarquía de implementaciones ────────────────────────────────────────
 *
 *   EnemyVariables (abstracta — este archivo)
 *       ▲
 *       ├── SansVariables      — variables exclusivas de Sans
 *       ├── ZombieVariables    — variables exclusivas del Zombie
 *       ├── FlyingVariables    — variables exclusivas del FlyingEnemy
 *       └── ...
 *
 * ── API ───────────────────────────────────────────────────────────────────
 *
 *   La API concreta la define cada implementación con campos y métodos
 *   completamente tipados. No existen Strings, Maps ni claves dinámicas.
 *
 *   Ejemplo correcto:
 *     enemy.getVariables().getTeleportFrames()
 *     enemy.getVariables().getAutoDodgeCooldown()
 *
 *   Ejemplo incorrecto (eliminado):
 *     enemy.getVariables().getDouble("teleportFrames")
 *     enemy.getVariables().set("hp", 100)
 *
 * ── Relación con Entity/* ─────────────────────────────────────────────────
 *
 *   Entity/* responde: ¿cuánta vida tiene?, ¿cuál es su velocidad efectiva?
 *   EnemyVariables responde: ¿cuántos frames dura el teleporte de Sans?
 *
 *   Ambos modelos son complementarios y no se solapan.
 *   Entity nunca conoce EnemyVariables.
 *   EnemyVariables puede leer de Entity para implementar comportamientos.
 *
 * ── Uso desde Enemy ───────────────────────────────────────────────────────
 *
 *   Enemy expone getVariables() que retorna EnemyVariables.
 *   Los sistemas de IA y comportamiento hacen cast al tipo concreto:
 *
 *     SansVariables vars = (SansVariables) enemy.getVariables();
 *     int frames = vars.getTeleportFrames();
 *
 *   Los Assemblers inyectan la implementación correcta durante construcción.
 */
public abstract class EnemyVariables {
    // Clase base intencionalemnte vacía.
    // Las implementaciones concretas declaran únicamente los campos
    // y métodos que describen su mecánica particular.
}
