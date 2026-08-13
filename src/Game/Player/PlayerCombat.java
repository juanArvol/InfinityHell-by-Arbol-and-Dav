package Game.Player;

import Game.Engine.GameEventBus;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Gameplay.Events.WeaponEvents;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.Definition.BulletType;
import Game.Items.Types.Weapons.ModifiedWeapon;
import Inputs.Listeners.MouseActionListener;
import Inputs.MouseAction;
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
 * usando exactamente el mismo pipeline que el disparo real, incluyendo los
 * multiplicadores de FireMode, luego deriva BulletStats via BulletFactory.statsFrom().
 *
 * ── HRFC — Phantom Bullet / Unified Projectile Resolution ─────────────────
 *
 * GARANTÍA DE CONSISTENCIA:
 * El preview de trayectoria usa exactamente la misma resolución que el disparo real:
 * - Mismo FireMode.handleInput() con estado actual de input
 * - Mismo ProjectileResolver.resolveWithFireMode() 
 * - Mismos multiplicadores de daño y velocidad
 * - Misma aplicación de amuletos y efectos
 * 
 * La única diferencia: disparo real materializa proyectiles, preview solo calcula stats.
 *
 * ── DEPENDENCY INJECTION ─────────────────────────────────────────────────
 *
 *   playerRuntime    → proveedor de arma/bala activas
 *   positionSupplier → proveedor de posición del portador
 *   bulletSpawner    → callback que añade proyectiles al mundo
 *   eventBus         → bus de eventos para emitir eventos de combate
 */
public class PlayerCombat implements MouseActionListener {

    // ── Equipment Cycling Mode ────────────────────────────────────────────
    
    /**
     * Modo de cycling del equipamiento.
     * Determina qué categoría de equipamiento se modifica al hacer scroll.
     */
    private enum EquipmentCycleMode {
        WEAPON,
        BULLET
    }
    
    /** Modo actual de cycling — por defecto WEAPON según HRFC. */
    private EquipmentCycleMode cycleMode = EquipmentCycleMode.WEAPON;

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
    public void onMouseAction(MouseAction action, float virtualX, float virtualY) {
        if (action == MouseAction.LEFT_CLICK) {
            clickFired = true;
            return;
        }

        if (action == MouseAction.MIDDLE_CLICK) {
            toggleEquipmentCycleMode();
        }
    }

    // ── Equipment Cycling Control ─────────────────────────────────────────

    /**
     * Alterna el modo de cycling entre WEAPON y BULLET.
     * Invocado por click del botón central del mouse (BUTTON2).
     */
    private void toggleEquipmentCycleMode() {
        cycleMode = (cycleMode == EquipmentCycleMode.WEAPON)
                ? EquipmentCycleMode.BULLET
                : EquipmentCycleMode.WEAPON;
    }

    @Override
    public void onScroll(int delta) {
        if (delta == 0) {
            return;
        }

        if (cycleMode == EquipmentCycleMode.WEAPON) {
            cycleWeapon(delta);
        } else {
            cycleBullet(delta);
        }
    }

    /**
     * Cambia el arma según el delta del scroll.
     * delta < 0 → rueda hacia arriba → arma anterior
     * delta > 0 → rueda hacia abajo → arma siguiente
     */
    private void cycleWeapon(int delta) {
        if (delta < 0) {
            playerRuntime.previousWeapon();
        } else {
            playerRuntime.nextWeapon();
        }
    }

    /**
     * Cambia la bala según el delta del scroll.
     * delta < 0 → rueda hacia arriba → bala anterior
     * delta > 0 → rueda hacia abajo → bala siguiente
     */
    private void cycleBullet(int delta) {
        if (delta < 0) {
            playerRuntime.previousBullet();
        } else {
            playerRuntime.nextBullet();
        }
    }

    // ── Update ─────────────────────────────────────────────────────────────

    public void update() {
        if (state.isCongelado()) return;

        ModifiedWeapon currentWeapon = playerRuntime.getCurrentWeapon();
        if (currentWeapon == null) return;

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
                        currentWeapon, currentWeapon.getCooldown()));
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

    // ── Cambio de arma (delegación a PlayerRuntime) ──────────────────────

    /**
     * Avanza al siguiente arma.
     * Delega en PlayerRuntime — PlayerCombat no gestiona la selección.
     */
    public void nextWeapon() { 
        playerRuntime.nextWeapon(); 
    }
    
    /**
     * Retrocede al arma anterior.
     * Delega en PlayerRuntime — PlayerCombat no gestiona la selección.
     */
    public void previousWeapon() { 
        playerRuntime.previousWeapon(); 
    }

    // ── ProjectilePreview — para UI (CrossHairHUD) ────────────────────────

    /**
     * Calcula el preview del próximo disparo para mostrar en UI.
     *
     * ── HRFC — Mini-HRFC: Desacoplar PlayerCombat de la resolución interna del arma ──────────────
     *
     * ANTES: PlayerCombat accedía directamente a los internals del arma:
     *   - currentWeapon.getComport().getFireMode().queryResolution()
     *   - ProjectileResolver.resolveWithFireModeQuery() 
     *   - BulletFactory.statsFrom()
     *
     * AHORA: PlayerCombat delega al dominio del arma:
     *   - currentWeapon.getProjectilePreview()
     *
     * PlayerCombat se limita a su responsabilidad de orquestación:
     *   1. Obtener arma y bala actuales del PlayerRuntime
     *   2. Obtener estado de input actual
     *   3. Calcular posición de spawn
     *   4. Delegar resolución al dominio del arma
     *
     * La resolución interna del disparo permanece encapsulada en el dominio
     * responsable (ModifiedWeapon), que conoce sobre FireMode, ProjectileResolver,
     * multiplicadores, etc.
     *
     * @return ProjectilePreview con stats calculados incluyendo FireMode, o null si no hay arma/bala
     */
    public ModifiedWeapon.ProjectilePreview getProjectilePreview() {
        ModifiedWeapon currentWeapon = playerRuntime.getCurrentWeapon();
        BulletType currentBullet = playerRuntime.getCurrentBullet();

        if (currentWeapon == null || currentBullet == null) {
            return null;
        }

        // ── Obtener estado actual de input ────────────────────────────────
        boolean holding = MouseInput.getButtonState("leftPressed");
        
        // ── Calcular posición de spawn ────────────────────────────────────
        Vector2D spawnPosition = (positionSupplier != null) 
            ? positionSupplier.get().add(new Vector2D(20, 20))  // Offset del spawn
            : new Vector2D(20, 20);

        // ── Delegar al dominio del arma ───────────────────────────────────
        // El arma encapsula toda la resolución interna: FireMode, ProjectileResolver, BulletFactory
        return currentWeapon.getProjectilePreview(currentBullet, holding, spawnPosition);
    }

}
