package Game.Player;

import Game.Engine.GameEventBus;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Gameplay.Events.WeaponEvents;
import Game.Items.Types.Ammulets.AmuletRegistry;
import Game.Items.Types.Bullets.BulletComport.BulletStats;
import Game.Items.Types.Bullets.BulletFactory;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.Definition.BulletType;
import Game.Items.Types.Bullets.ProjectileBlueprint;
import Game.Items.Types.Weapons.ModifiedWeapon;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;
import Inputs.Listeners.MouseActionListener;
import Inputs.MouseInput;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Combate del jugador — ejecuta combate consumiendo desde inventario externo.
 *
 * ── HRFC — Player Reengineering v2 ────────────────────────────────────────
 *
 * ── RESPONSABILIDADES ─────────────────────────────────────────────────────
 *
 *   1. Leer input de combate (click, botón mantenido, recarga, cambio de arma/bala)
 *   2. Obtener arma y bala activas desde PlayerRuntime
 *   3. Gestionar recarga (coordinar con PlayerState)
 *   4. Ejecutar pipeline de disparo via ModifiedWeapon
 *   5. Pasar proyectiles al bulletSpawner inyectado
 *   6. Emitir eventos de combate
 *   7. Proveer ProjectilePreview para UI (CrossHairHUD)
 *
 * ── LO QUE NO ALMACENA ────────────────────────────────────────────────────
 *
 *   ✗ Lista de armas
 *   ✗ Lista de balas
 *   ✗ Posesiones
 *   ✗ Adquisiciones
 *   ✗ WeaponInventory
 *
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 *
 *   PlayerRuntime
 *          │
 *          │ consulta
 *          ▼
 *   PlayerCombat
 *          │
 *          ├── obtiene arma activa
 *          ├── obtiene bala activa
 *          │
 *          ▼
 *   ModifiedWeapon.handleInput(bala activa)
 *          │
 *          ▼
 *   ProjectileBlueprint → BulletFactory → Bullet
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 *   PlayerInventory → almacenamiento (qué posee)
 *   PlayerRuntime   → selección activa (qué está equipado)
 *   PlayerCombat    → ejecución (cómo se utiliza lo equipado)
 *
 * ── PROJECTILE PREVIEW ────────────────────────────────────────────────────
 *
 * CrossHairHUD necesita stats del proyectil sin disparar realmente.
 * PlayerCombat provee getProjectilePreview() que calcula el ProjectileBlueprint
 * usando el mismo pipeline que el disparo real, luego deriva BulletStats
 * via BulletFactory.statsFrom().
 *
 * ── DEPENDENCY INJECTION ─────────────────────────────────────────────────
 *
 *   playerRuntime    → proveedor de arma/bala activas
 *   positionSupplier → proveedor de posición del portador
 *   bulletSpawner    → callback que añade proyectiles al mundo
 *   eventBus         → bus de eventos para emitir eventos de combate
 */
public class PlayerCombat implements MouseActionListener {

    private final PlayerState        state;
    private final PlayerRuntime      playerRuntime;
    private       Supplier<Vector2D> positionSupplier;
    private final Consumer<Bullet>   bulletSpawner;
    private final GameEventBus       eventBus;

    /** Edge-click registrado este frame (disparo puntual en SemiAuto/Burst). */
    private boolean clickFired = false;

    /**
     * @param state            estado del jugador (congelado, apuntado, recargando)
     * @param playerRuntime    runtime del jugador para obtener arma/bala activas
     * @param positionSupplier proveedor de posición actual del portador (puede ser null inicialmente)
     * @param bulletSpawner    callback para añadir proyectiles al mundo
     * @param eventBus         bus de eventos para emitir eventos de arma
     */
    public PlayerCombat(
            PlayerState state,
            PlayerRuntime playerRuntime,
            Supplier<Vector2D> positionSupplier,
            Consumer<Bullet> bulletSpawner,
            GameEventBus eventBus
    ) {
        if (state == null) throw new IllegalArgumentException("state es requerido");
        if (playerRuntime == null) throw new IllegalArgumentException("playerRuntime es requerido");
        if (bulletSpawner == null) throw new IllegalArgumentException("bulletSpawner es requerido");
        if (eventBus == null) throw new IllegalArgumentException("eventBus es requerido");
        
        this.state            = state;
        this.playerRuntime    = playerRuntime;
        this.positionSupplier = positionSupplier;  // Puede ser null inicialmente
        this.bulletSpawner    = bulletSpawner;
        this.eventBus         = eventBus;
    }

    // ── Actualización del proveedor de posición ──────────────────────────

    /**
     * Actualiza el proveedor de posición del portador.
     *
     * ── HRFC — Player Reengineering v2 ────────────────────────────────────
     *
     * Elimina la necesidad del hack Vector2D[] positionRef. PlayerAssembler
     * ahora crea PlayerCombat con positionSupplier=null y luego lo inyecta
     * una vez que Player existe, evitando referencias circulares artificiales.
     *
     * @param positionSupplier nuevo proveedor de posición. No puede ser null.
     */
    public void setPositionSupplier(Supplier<Vector2D> positionSupplier) {
        if (positionSupplier == null)
            throw new IllegalArgumentException("positionSupplier no puede ser null");
        this.positionSupplier = positionSupplier;
    }

    // ── MouseActionListener ────────────────────────────────────────────────

    @Override
    public void onMouseAction(String action, float virtualX, float virtualY) {
        if ("leftClick".equals(action)) {
            clickFired = true;
        }
    }

    // ── Update ─────────────────────────────────────────────────────────────

    public void update() {
        if (state.isCongelado()) return;

        ModifiedWeapon currentWeapon = playerRuntime.getCurrentWeapon();
        if (currentWeapon == null) return;

        // ── Input de cambio de arma/bala ──────────────────────────────────
        handleWeaponSwitching();
        handleBulletSwitching();

        // ── Recarga manual ────────────────────────────────────────────────
        boolean reloadKeyPressed = Inputs.KeyBoard.getState("reload");
        if (reloadKeyPressed && !state.isReloading() && !currentWeapon.isFullyLoaded()) {
            // PlayerState es la fuente de verdad del estado lógico del Player
            state.setReloading(true);
            
            // El arma ejecuta su mecánica interna de recarga
            currentWeapon.reload();

            // Evento de inicio de recarga (suscriptores opcionales: UI, audio)
            if (eventBus.hasListeners(WeaponEvents.OnReloadStart.class)) {
                eventBus.post(new WeaponEvents.OnReloadStart(
                        currentWeapon, currentWeapon.getComport().getStats().getCooldown()));
            }
        }

        // ── Sincronizar estado de recarga ─────────────────────────────────
        // PlayerState controla el estado lógico, WeaponComport maneja la mecánica
        if (state.isReloading()) {
            // Verificar si el arma completó su mecánica de recarga
            boolean weaponStillReloading = currentWeapon.isReloading();
            if (!weaponStillReloading) {
                // La mecánica del arma se completó → actualizar estado del Player
                state.setReloading(false);
                if (eventBus.hasListeners(WeaponEvents.OnReloadComplete.class)) {
                    eventBus.post(new WeaponEvents.OnReloadComplete(currentWeapon));
                }
            }
        }

        // ── Cargador vacío ────────────────────────────────────────────────
        if (clickFired && currentWeapon.getCurrentAmmo() <= 0 && !currentWeapon.isReloading()) {
            if (eventBus.hasListeners(WeaponEvents.OnEmptyMagazine.class)) {
                eventBus.post(new WeaponEvents.OnEmptyMagazine(currentWeapon));
            }
        }

        // ── Disparo ───────────────────────────────────────────────────────
        boolean holding  = MouseInput.getButtonState("leftPressed");
        Vector2D pos     = (positionSupplier != null) ? positionSupplier.get() : new Vector2D(0, 0);
        Vector2D aim     = state.getAimDirection();
        BulletType currentBullet = playerRuntime.getCurrentBullet();

        List<Bullet> newBullets = handleShooting(currentWeapon, currentBullet, 
                                                 holding, clickFired, pos, aim, state.isDer());

        clickFired = false;

        // Pasar proyectiles al mundo via el spawner inyectado
        for (Bullet b : newBullets) {
            bulletSpawner.accept(b);
        }

        // Actualizar timers del arma (cooldown, recarga interna)
        currentWeapon.update();
    }

    // ── Disparo con bala runtime ──────────────────────────────────────────

    /**
     * Maneja el disparo usando la bala actualmente seleccionada.
     * Utiliza el nuevo sistema de balas runtime.
     */
    private List<Bullet> handleShooting(ModifiedWeapon weapon, BulletType bulletType,
                                       boolean holding, boolean clickFired,
                                       Vector2D pos, Vector2D aim, boolean facingRight) {
        if (bulletType == null) {
            // Sin bala seleccionada, no se puede disparar
            return List.of();
        }
        
        // Usar el nuevo handleInput que acepta BulletType como parámetro
        return weapon.handleInput(bulletType, holding, clickFired, 
                                 pos.getX(), pos.getY(), facingRight, aim);
    }

    // ── Input de cambio de equipamiento ───────────────────────────────────

    /**
     * Maneja el input de cambio de arma.
     */
    private void handleWeaponSwitching() {
        // Rueda del mouse o teclas para cambiar arma
        if (Inputs.KeyBoard.getState("nextWeapon")) {
            ModifiedWeapon previous = playerRuntime.getCurrentWeapon();
            playerRuntime.nextWeapon();
            ModifiedWeapon current = playerRuntime.getCurrentWeapon();
            emitWeaponSwitch(previous, current);
        }
        
        if (Inputs.KeyBoard.getState("prevWeapon")) {
            ModifiedWeapon previous = playerRuntime.getCurrentWeapon();
            playerRuntime.previousWeapon();
            ModifiedWeapon current = playerRuntime.getCurrentWeapon();
            emitWeaponSwitch(previous, current);
        }
    }

    /**
     * Maneja el input de cambio de bala.
     */
    private void handleBulletSwitching() {
        // Teclas para cambiar tipo de bala
        if (Inputs.KeyBoard.getState("nextBullet")) {
            playerRuntime.nextBullet();
        }
        
        if (Inputs.KeyBoard.getState("prevBullet")) {
            playerRuntime.previousBullet();
        }
    }

    // ── Cambio de arma (delegación a PlayerRuntime) ──────────────────────

    /**
     * Avanza al siguiente arma.
     * Delega en PlayerRuntime — PlayerCombat no gestiona la selección.
     */
    public void nextWeapon() { 
        ModifiedWeapon previous = playerRuntime.getCurrentWeapon();
        playerRuntime.nextWeapon(); 
        ModifiedWeapon current = playerRuntime.getCurrentWeapon();
        emitWeaponSwitch(previous, current);
    }
    
    /**
     * Retrocede al arma anterior.
     * Delega en PlayerRuntime — PlayerCombat no gestiona la selección.
     */
    public void previousWeapon() { 
        ModifiedWeapon previous = playerRuntime.getCurrentWeapon();
        playerRuntime.previousWeapon(); 
        ModifiedWeapon current = playerRuntime.getCurrentWeapon();
        emitWeaponSwitch(previous, current);
    }

    // ── Eventos ───────────────────────────────────────────────────────────

    /**
     * Emite evento de cambio de arma si hay suscriptores.
     */
    private void emitWeaponSwitch(ModifiedWeapon previous, ModifiedWeapon current) {
        if (previous != current && eventBus.hasListeners(WeaponEvents.OnWeaponSwitch.class)) {
            eventBus.post(new WeaponEvents.OnWeaponSwitch(previous, current));
        }
    }

    // ── ProjectilePreview — para UI (CrossHairHUD) ────────────────────────

    /**
     * Calcula las estadísticas del proyectil que se dispararía actualmente.
     * Usa el mismo pipeline que el disparo real, pero sin crear Bullet.
     *
     * ── HRFC — Player Inventory & Domain Ownership Consolidation ─────────
     *
     * Esta API reemplaza el uso directo de weapon.getBulletType().create()
     * y BulletFactory desde CrossHairHUD. El HUD no debe conocer los detalles
     * internos del pipeline de disparo.
     *
     * @return ProjectilePreview con stats calculados, o null si no hay arma/bala
     */
    public ProjectilePreview getProjectilePreview() {
        ModifiedWeapon currentWeapon = playerRuntime.getCurrentWeapon();
        BulletType currentBullet = playerRuntime.getCurrentBullet();

        if (currentWeapon == null || currentBullet == null) {
            return null;
        }

        // Replicar el pipeline de ModifiedWeapon.tryShoot() sin disparar
        WeaponStats effectiveStats = copyStats(currentWeapon.getStats());
        var behavior = currentBullet.create();
        
        // Aplicar amuletos del jugador (igual que en disparo real)
        behavior = AmuletRegistry.applyAll(
            playerRuntime.getInventory().amulets().getIds(), 
            effectiveStats, 
            behavior
        );

        double finalSpeed = effectiveStats.getBulletSpeedBase() 
                           * behavior.getDefaultData().speedFactor();
        double finalDamage = effectiveStats.getDamageBonusByWeapon() 
                            + behavior.getDefaultData().damage();

        // Crear ProjectileBlueprint (igual que en disparo real)
        ProjectileBlueprint blueprint = ProjectileBlueprint.from(
            behavior, finalSpeed, finalDamage
        );

        // Derivar BulletStats via BulletFactory.statsFrom()
        BulletStats stats = BulletFactory.statsFrom(blueprint);

        Vector2D spawnPosition = (positionSupplier != null) 
            ? positionSupplier.get().add(new Vector2D(20, 20))  // Offset del spawn
            : new Vector2D(20, 20);

        return new ProjectilePreview(
            stats.getSpeed(),
            stats.getDamage(), 
            stats.getLifeTime(),
            stats.hasGravity(),
            spawnPosition
        );
    }

    /**
     * Copia mutable de WeaponStats — helper compartido con ModifiedWeapon.
     */
    private static WeaponStats copyStats(WeaponStats src) {
        return new WeaponStats(
            src.getCooldown(),
            src.getBulletsPerShot(),
            src.getSpread(),
            src.getDamageBonusByWeapon(),
            src.getBulletSpeedBase()
        );
    }

    /**
     * Preview de proyectil para UI — encapsula stats calculados.
     *
     * ── HRFC — Player Inventory & Domain Ownership Consolidation ─────────
     *
     * Reemplaza el uso directo de weapon.getBulletType() + ProjectileBlueprint
     * + BulletFactory desde CrossHairHUD. El HUD consulta este record inmutable
     * en lugar de reconstruir manualmente el pipeline de disparo.
     *
     * @param speed        velocidad del proyectil (unidades/frame)
     * @param damage       daño del proyectil
     * @param lifeTime     frames de vida máximos
     * @param hasGravity   true si el proyectil tiene gravedad
     * @param spawnPosition posición de spawn del proyectil (mundo)
     */
    public record ProjectilePreview(
        double speed,
        double damage,
        int lifeTime,
        boolean hasGravity,
        Vector2D spawnPosition
    ) {}
}
