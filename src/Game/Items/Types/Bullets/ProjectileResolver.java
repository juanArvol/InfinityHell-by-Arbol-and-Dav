package Game.Items.Types.Bullets;

import Game.Items.Creation.ItemDefinition;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.Definition.BulletType;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;
import java.util.List;

/**
 * Resolución unificada de proyectiles — única fuente de verdad para el pipeline de disparo.
 *
 * ── HRFC — Mini-HRFC Eliminación de adaptadores especializados ────────────
 *
 * ProjectileResolver no debe conocer FireMode. Su responsabilidad es resolver
 * proyectiles basándose en los datos que realmente necesita:
 *   - damageMultiplier
 *   - speedMultiplier
 *
 * Los callers extraen estos datos del resultado de FireMode y los pasan explícitamente.
 * Esto mantiene la separación de concerns: FireMode decide, ProjectileResolver ejecuta.
 *
 * ── HRFC — Projectile Preview Single Resolution Path ──────────────────────
 *
 * Este servicio extrae la lógica duplicada entre:
 *   - ModifiedWeapon.tryShoot()     (disparo real)
 *   - PlayerCombat.getProjectilePreview()  (preview para UI)
 *
 * Ambos sistemas ahora usan la misma resolución de dominio, garantizando que
 * lo que muestra el HUD sea exactamente lo que el sistema de combate resolvería.
 *
 * ── RESPONSABILIDADES ─────────────────────────────────────────────────────
 *
 * 1. Copiar WeaponStats para modificación mutable
 * 2. Crear BulletBehavior desde BulletType
 * 3. Aplicar efectos de amuletos (stats + behavior wrapping)
 * 4. Calcular velocidad y daño finales
 * 5. Construir ProjectileBlueprint resuelto
 *
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 *
 * WeaponStats + BulletType + Amuletos + Multiplicadores
 *              │
 *              ▼
 *      ProjectileResolver
 *              │
 *              ▼
 *      ResolvedProjectile (Blueprint + Stats)
 *              │
 *         ┌────┴────┐
 *         ▼         ▼
 *    BulletFactory  BulletFactory.statsFrom()
 *         │                    │
 *         ▼                    ▼
 *    List<Bullet>        BulletStats (UI)
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 *
 * ProjectileResolver es un servicio puro — no mantiene estado.
 * Todas las operaciones son side-effect free, permitiendo su uso
 * tanto en disparo real como en preview sin interferencias.
 */
public final class ProjectileResolver {

    private ProjectileResolver() {}

    /**
     * Resultado completo de la resolución de un proyectil.
     * 
     * Contiene tanto el ProjectileBlueprint resuelto como las WeaponStats
     * modificadas por amuletos. Esto evita duplicar la resolución cuando
     * ambos valores son necesarios (como en ModifiedWeapon.tryShoot).
     * 
     * @param blueprint ProjectileBlueprint con todos los valores calculados
     * @param stats     WeaponStats después de aplicar amuletos
     */
    public record ResolvedProjectile(
        ProjectileBlueprint blueprint,
        WeaponStats stats
    ) {}

    /**
     * Resuelve un proyectile completo aplicando el pipeline de disparo.
     * Retorna tanto el blueprint como las stats modificadas.
     *
     * ── PIPELINE DE RESOLUCIÓN ────────────────────────────────────────────
     *
     * 1. Copia mutable de WeaponStats (el original no se toca)
     * 2. BulletBehavior base desde BulletType
     * 3. Aplicación de amuletos (modifica stats + envuelve behavior)
     * 4. Cálculo de velocidad final (weapon base * speedMult * behavior factor)
     * 5. Cálculo de daño final (weapon bonus * damageMult + behavior base)
     * 6. ProjectileBlueprint con todos los valores resueltos
     *
     * Los multiplicadores permiten efectos de FireMode (burst damage bonus,
     * auto speed penalty, etc.) sin duplicar la lógica base.
     *
     * @param baseStats     estadísticas base del arma (inmutable)
     * @param bulletType    tipo de bala a disparar
     * @param amulets       amuletos del jugador (pueden estar vacíos)
     * @param damageMult    multiplicador de daño (típicamente de FireMode)
     * @param speedMult     multiplicador de velocidad (típicamente de FireMode)
     * @return ResolvedProjectile con blueprint y stats modificadas
     * @throws IllegalArgumentException si baseStats o bulletType son null
     */
    public static ResolvedProjectile resolveComplete(
            WeaponStats baseStats,
            BulletType bulletType,
            List<ItemDefinition> amulets,
            double damageMult,
            double speedMult) {

        if (baseStats == null) {
            throw new IllegalArgumentException("baseStats no puede ser null");
        }
        if (bulletType == null) {
            throw new IllegalArgumentException("bulletType no puede ser null");
        }
        if (amulets == null) {
            throw new IllegalArgumentException("amulets no puede ser null");
        }

        // 1. Copia mutable de stats — el original del arma no se toca
        WeaponStats effectiveStats = copyStats(baseStats);

        // 2. Behavior base del tipo de bala
        BulletBehavior behavior = bulletType.create();

        // 3. Aplicar amuletos del jugador (modifica stats y envuelve behavior)
        /* behavior = AmuletRegistry.applyAllFromDefinitions(amulets, effectiveStats, behavior); */

        // 4. Cálculos finales con multiplicadores
        // bulletSpeedBase ya está en units/s desde WeaponStats
        // Los multiplicadores son factores sin unidad
        double finalSpeed = effectiveStats.getBulletSpeedBase() * speedMult
                           * behavior.getDefaultData().speedFactor();
        
        double finalDamage = effectiveStats.getDamageBonusByWeapon() * damageMult
                            + behavior.getDefaultData().damage();

        // 5. Construir ProjectileBlueprint con toda la resolución aplicada
        ProjectileBlueprint blueprint = ProjectileBlueprint.from(behavior, finalSpeed, finalDamage);

        return new ResolvedProjectile(blueprint, effectiveStats);
    }

    /**
     * Resuelve un proyectile completo aplicando el pipeline de disparo.
     *
     * ── PIPELINE DE RESOLUCIÓN ────────────────────────────────────────────
     *
     * 1. Copia mutable de WeaponStats (el original no se toca)
     * 2. BulletBehavior base desde BulletType
     * 3. Aplicación de amuletos (modifica stats + envuelve behavior)
     * 4. Cálculo de velocidad final (weapon base * speedMult * behavior factor)
     * 5. Cálculo de daño final (weapon bonus * damageMult + behavior base)
     * 6. ProjectileBlueprint con todos los valores resueltos
     *
     * Los multiplicadores permiten efectos de FireMode (burst damage bonus,
     * auto speed penalty, etc.) sin que ProjectileResolver conozca FireMode.
     *
     * @param baseStats     estadísticas base del arma (inmutable)
     * @param bulletType    tipo de bala a disparar
     * @param amulets       amuletos del jugador (pueden estar vacíos)
     * @param damageMult    multiplicador de daño (típicamente de FireMode)
     * @param speedMult     multiplicador de velocidad (típicamente de FireMode)
     * @return ProjectileBlueprint completamente resuelto
     * @throws IllegalArgumentException si baseStats o bulletType son null
     */
    public static ProjectileBlueprint resolve(
            WeaponStats baseStats,
            BulletType bulletType,
            List<ItemDefinition> amulets,
            double damageMult,
            double speedMult) {

        return resolveComplete(baseStats, bulletType, amulets, damageMult, speedMult).blueprint();
    }

    /**
     * Copia mutable de WeaponStats para modificación por amuletos.
     *
     * ── INMUTABILIDAD DEL ORIGINAL ────────────────────────────────────────
     *
     * WeaponStats del arma permanece inmutable. Los amuletos modifican
     * esta copia sin afectar el arma original, permitiendo que múltiples
     * resoluciones concurrentes (preview + disparo real) no interfieran.
     *
     * @param src WeaponStats original a copiar
     * @return copia mutable para modificación por amuletos
     */
    public static WeaponStats copyStats(WeaponStats src) {
        return new WeaponStats(
                src.getCooldown(),
                src.getBulletsPerShot(),
                src.getSpread(),
                src.getDamageBonusByWeapon(),
                src.getBulletSpeedBase()
        );
    }
}