package Game.Items.Types.Projectiles;

import Game.Items.Types.Ammulets.AmuletDefinition;
import Game.Items.Types.Ammulets.AmuletRegistry;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.Definition.BulletType;
import Game.Items.Types.Bullets.ProjectileBlueprint;
import Game.Items.Types.Weapons.WeaponType.FireMode.FireModeResolution;
import Game.Items.Types.Weapons.WeaponType.FireMode.FireModeResult;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;
import java.util.List;

/**
 * Resolución unificada de proyectiles — única fuente de verdad para el pipeline de disparo.
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
 * ── HRFC — Phantom Bullet / Unified Projectile Resolution ───────────────
 *
 * NUEVA ARQUITECTURA para Phantom Bullet:
 * 
 * Tanto el disparo real como el preview de trayectoria deben usar exactamente
 * la misma resolución, incluyendo los multiplicadores de FireMode. La diferencia
 * entre ambos debe existir únicamente en el uso posterior del resultado:
 * 
 *   - Disparo real → materializa el proyectil y ejecuta gameplay
 *   - Phantom Bullet → representa visualmente su trayectoria sin ejecutar gameplay
 * 
 * Métodos de resolución:
 * 
 *   resolveWithFireMode()     - Acepta FireModeResult directamente (nueva API unificada)
 *   resolveComplete()         - Acepta multiplicadores por separado (compatibilidad)
 *   resolve()                 - Versiones de conveniencia
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
 * WeaponStats + BulletType + Amuletos
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
            List<AmuletDefinition> amulets,
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
        behavior = AmuletRegistry.applyAllFromDefinitions(amulets, effectiveStats, behavior);

        // 4. Cálculos finales con multiplicadores
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
     * auto speed penalty, etc.) sin duplicar la lógica base.
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
            List<AmuletDefinition> amulets,
            double damageMult,
            double speedMult) {

        return resolveComplete(baseStats, bulletType, amulets, damageMult, speedMult).blueprint();
    }



    // ── HRFC — Phantom Bullet / Unified Projectile Resolution ────────────

    /**
     * Resolución unificada usando FireModeResult — API principal para Phantom Bullet.
     *
     * ── ÚNICA FUENTE DE VERDAD PARA FIREMODE ──────────────────────────────
     *
     * Este método garantiza que tanto el disparo real como el preview de trayectoria
     * usen exactamente los mismos multiplicadores de FireMode. El FireModeResult
     * debe obtenerse de la misma llamada a fireMode.handleInput() que se usaría
     * en el disparo real.
     *
     * Uso en disparo real (ModifiedWeapon.tryShoot):
     *   FireModeResult result = comport.getFireMode().handleInput(held, pressed, comport);
     *   ProjectileBlueprint bp = ProjectileResolver.resolveWithFireMode(..., result);
     *
     * Uso en preview (PlayerCombat.getProjectilePreview):
     *   FireModeResult result = weapon.getComport().getFireMode().handleInput(held, pressed, comport);
     *   ProjectileBlueprint bp = ProjectileResolver.resolveWithFireMode(..., result);
     *
     * @param baseStats     estadísticas base del arma (inmutable)
     * @param bulletType    tipo de bala a disparar
     * @param amulets       amuletos del jugador (pueden estar vacíos)
     * @param fireModeResult resultado de FireMode.handleInput() con multiplicadores exactos
     * @return ProjectileBlueprint completamente resuelto con FireMode aplicado
     * @throws IllegalArgumentException si algún parámetro es null
     */
    public static ProjectileBlueprint resolveWithFireMode(
            WeaponStats baseStats,
            BulletType bulletType,
            List<AmuletDefinition> amulets,
            FireModeResult fireModeResult) {

        if (fireModeResult == null) {
            throw new IllegalArgumentException("fireModeResult no puede ser null");
        }

        return resolve(baseStats, bulletType, amulets,
                fireModeResult.getDamageMultiplier(),
                fireModeResult.getSpeedMultiplier());
    }

    /**
     * Resolución completa usando FireModeResult — retorna blueprint y stats modificadas.
     *
     * ── VERSIÓN COMPLETA PARA DISPARO REAL ────────────────────────────────
     *
     * Idéntica a resolveWithFireMode() pero retorna tanto el ProjectileBlueprint
     * como las WeaponStats modificadas por amuletos. Útil para ModifiedWeapon.tryShoot()
     * que necesita las stats para spread y bulletsPerShot.
     *
     * @param baseStats     estadísticas base del arma (inmutable)
     * @param bulletType    tipo de bala a disparar
     * @param amulets       amuletos del jugador (pueden estar vacíos)
     * @param fireModeResult resultado de FireMode.handleInput() con multiplicadores exactos
     * @return ResolvedProjectile con blueprint y stats modificadas
     * @throws IllegalArgumentException si algún parámetro es null
     */
    public static ResolvedProjectile resolveCompleteWithFireMode(
            WeaponStats baseStats,
            BulletType bulletType,
            List<AmuletDefinition> amulets,
            FireModeResult fireModeResult) {

        if (fireModeResult == null) {
            throw new IllegalArgumentException("fireModeResult no puede ser null");
        }

        return resolveComplete(baseStats, bulletType, amulets,
                fireModeResult.getDamageMultiplier(),
                fireModeResult.getSpeedMultiplier());
    }

    // ── HRFC — Separación de consulta de FireMode de su ejecución ─────────

    /**
     * Resolución usando FireModeResolution — API para consultas idempotentes.
     *
     * ── SEPARACIÓN QUERY/EXECUTION ────────────────────────────────────────
     *
     * Este método permite a ProjectilePreview obtener la resolución de proyectil
     * usando multiplicadores de FireMode sin ejecutar handleInput() ni mutar estado.
     * 
     * DIFERENCIA CON resolveWithFireMode():
     * 
     *   resolveWithFireMode():
     *     - Acepta FireModeResult (de handleInput())
     *     - Para disparo real donde se procesa input
     *     - Puede mutar estado del FireMode
     * 
     *   resolveWithFireModeQuery():
     *     - Acepta FireModeResolution (de queryResolution())
     *     - Para preview donde solo se consulta estado
     *     - Garantiza que no se muta estado del FireMode
     * 
     * USO EN PROJECTILE PREVIEW:
     *   FireModeResolution resolution = weapon.getComport().getFireMode()
     *                                         .queryResolution(held, comport);
     *   ProjectileBlueprint bp = ProjectileResolver.resolveWithFireModeQuery(
     *       stats, bulletType, amulets, resolution);
     *
     * @param baseStats     estadísticas base del arma (inmutable)
     * @param bulletType    tipo de bala a disparar
     * @param amulets       amuletos del jugador (pueden estar vacíos)
     * @param resolution    resolución de FireMode obtenida via queryResolution()
     * @return ProjectileBlueprint completamente resuelto sin side-effects
     * @throws IllegalArgumentException si algún parámetro es null
     */
    public static ProjectileBlueprint resolveWithFireModeQuery(
            WeaponStats baseStats,
            BulletType bulletType,
            List<AmuletDefinition> amulets,
            FireModeResolution resolution) {

        if (resolution == null) {
            throw new IllegalArgumentException("resolution no puede ser null");
        }

        return resolve(baseStats, bulletType, amulets,
                resolution.damageMultiplier(),
                resolution.speedMultiplier());
    }

    /**
     * Resolución completa usando FireModeResolution — retorna blueprint y stats.
     *
     * ── VERSIÓN COMPLETA PARA CONSULTAS ───────────────────────────────────
     *
     * Idéntica a resolveWithFireModeQuery() pero retorna tanto el ProjectileBlueprint
     * como las WeaponStats modificadas por amuletos. Útil cuando el preview necesita
     * tanto el blueprint como las stats del arma.
     *
     * @param baseStats     estadísticas base del arma (inmutable)
     * @param bulletType    tipo de bala a disparar
     * @param amulets       amuletos del jugador (pueden estar vacíos)
     * @param resolution    resolución de FireMode obtenida via queryResolution()
     * @return ResolvedProjectile con blueprint y stats modificadas
     * @throws IllegalArgumentException si algún parámetro es null
     */
    public static ResolvedProjectile resolveCompleteWithFireModeQuery(
            WeaponStats baseStats,
            BulletType bulletType,
            List<AmuletDefinition> amulets,
            FireModeResolution resolution) {

        if (resolution == null) {
            throw new IllegalArgumentException("resolution no puede ser null");
        }

        return resolveComplete(baseStats, bulletType, amulets,
                resolution.damageMultiplier(),
                resolution.speedMultiplier());
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