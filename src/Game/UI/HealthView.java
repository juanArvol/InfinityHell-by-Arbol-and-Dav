package Game.UI;

/**
 * Vista de solo lectura de la salud de una entidad.
 *
 * ── PROPÓSITO ─────────────────────────────────────────────────────────────
 * Desacopla los HUDs de vida (LifeHUD, EnemyLifeHUD, BossLifeHUD) de la
 * implementación concreta del sistema de salud.
 *
 * Sin esta interfaz hay dos opciones malas:
 *
 *   Opción A — LifeHUD → HealthComponent directamente
 *     Funciona para el Player, pero rompe la simetría para otros tipos:
 *     un EnemyLifeHUD también recibiría HealthComponent de un Enemy, lo cual
 *     asume que todos los tipos tienen HealthComponent como componente.
 *     Si un Enemy con un sistema de salud diferente (por diseño o por
 *     simplicidad) necesita mostrar su vida en la UI, no hay contrato unificado.
 *
 *   Opción B — LifeHUD → PlayerStats → HealthComponent
 *     Introduce fachada en PlayerStats, lo cual está bien para el Player.
 *     Pero EnemyLifeHUD no tiene PlayerStats — la fachada no es reutilizable.
 *     Habría que duplicar getLife()/getLifeMax() en EnemyStats, BossStats...
 *
 * ── SOLUCIÓN ──────────────────────────────────────────────────────────────
 * HealthView es el contrato de lectura de salud. Cualquier tipo puede
 * implementarlo, independientemente de su sistema de salud interno:
 *
 *   PlayerStats  implementa HealthView → delega a HealthComponent del Player.
 *   Enemy        implementa HealthView → delega a su propio campo de vida.
 *   Boss         implementa HealthView → puede exponer vida de fase o vida total.
 *
 * LifeHUD recibe HealthView — no sabe nada de HealthComponent ni de PlayerStats.
 * Es agnóstico a quién le provee los datos.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   // Player
 *   uiManager.add(new LifeHUD(player.getPlayerStats(), vw, vh));
 *                                ↑ PlayerStats implementa HealthView
 *
 *   // Enemy (futuro)
 *   uiManager.add(new EnemyLifeHUD(enemy, vw, vh));
 *                                  ↑ Enemy implementa HealthView
 *
 *   // Boss (futuro)
 *   uiManager.add(new BossLifeHUD(boss.getHealthView(), vw, vh));
 *
 * ── POR QUÉ NO getPercent() + getMaxHp() directos ────────────────────────
 * getLife() y getLifeMax() son más expresivos para UI: el diseñador puede
 * mostrar "HP: 75 / 200" sin recomputar. getPercent() queda como derivado.
 */
public interface HealthView {

    /** HP actual. */
    int getLife();

    /** HP máxima. */
    int getLifeMax();

    /**
     * Porcentaje de vida en [0.0, 1.0].
     * Implementación por defecto — no es necesario sobreescribir.
     */
    default double getLifePercent() {
        int max = getLifeMax();
        return (max > 0) ? (double) getLife() / max : 0.0;
    }
}
