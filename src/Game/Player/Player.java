package Game.Player;

import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Components.HealthComponent;
import Game.Engine.Components.StatusEffectComponent;
import Game.Engine.Components.Visuals.AnimationController;
import Game.Engine.Components.Visuals.HitBoxComponent;
import Game.Engine.Components.Visuals.SizeSyncMode;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.Engine.MovingObjects;
import Game.Items.Savement.EquippedItems;
import Game.Items.Savement.Inventory;
import Game.Items.Types.Bullets.Bullet;
import Sprites.Player.PlayerAssets;
import java.awt.Color;
import java.util.function.Consumer;

/**
 * Jugador.
 *
 * ── REFACTOR: SALUD MIGRADA A HealthComponent ─────────────────────────────
 *
 * PROBLEMA ORIGINAL:
 *   Player usaba PlayerStats para la salud (life, lifeMax, receiveDamage,
 *   isDead). PlayerStats duplicaba exactamente la responsabilidad de
 *   HealthComponent. Había dos sistemas de salud para el mismo objeto.
 *
 * SOLUCIÓN:
 *   Player ahora añade HealthComponent como componente en su constructor.
 *   PlayerStats queda solo con los atributos complementarios (speedMultiplier,
 *   damageMultiplier, invulnerabilidad post-golpe).
 *
 *   La salud es accesible a través de los shortcuts de Entity (heredados
 *   via MovingObjects → Entity):
 *     player.damage(10)           → delega a HealthComponent
 *     player.heal(5)              → delega a HealthComponent
 *     player.isDead()             → delega a HealthComponent
 *     player.getHealthPercent()   → delega a HealthComponent
 *     player.getHealth()          → retorna HealthComponent directamente
 *
 *   Para daño con invulnerabilidad (específico del Player):
 *     player.receiveDamage(10)    → verifica PlayerStats.isInvulnerable()
 *                                   luego llama damage() y triggerInvulnerability()
 *
 * ── FLUJO DE DAÑO ────────────────────────────────────────────────────────
 *
 * Sistema externo que quiere dañar al Player:
 *
 *   // Con invulnerabilidad (daño de contacto, proyectiles normales):
 *   player.receiveDamage(amount);
 *
 *   // Sin invulnerabilidad (daño de efecto de estado, caída):
 *   player.damage(amount);  // shortcut de Entity → HealthComponent directo
 *
 * ── JERARQUÍA EN EFECTO ───────────────────────────────────────────────────
 *
 *   GameObjects → Entity → MovingObjects → Player
 *
 *   Player hereda de Entity (vía MovingObjects) los shortcuts:
 *   damage(), heal(), isDead(), getHealthPercent(), addEffect(), hasEffect()
 */
public class Player extends MovingObjects {

    // Configuración de salud del jugador
    // BASE_HP     = vida con la que el jugador comienza la partida
    // BASE_HP_MAX = vida máxima que puede tener (techo de curación)
    private static final int    BASE_HP      = 100;
    private static final int    BASE_HP_MAX  = 200;

    /** Gravedad aplicada a la física del jugador (píxeles virtuales/frame²). */
    private static final double BASE_GRAVITY = 0.78;

    private final PlayerController controller;
    private final PlayerCombat     combat;
    private final PlayerStats      stats;
    private final PlayerState      state;

    private final Inventory     inventory;
    private final EquippedItems equippedItems;

    /**
     * ── HRFC-002 ──────────────────────────────────────────────────────────
     * El constructor ya NO recibe BufferedImage. El Player obtiene su handle
     * de PlayerAssets directamente. GameWorldBootstrap no necesita conocer
     * la imagen del jugador — solo su posición de spawn.
     *
     * @param spawn         posición inicial
     * @param bulletSpawner callback para añadir balas al mundo (ej: world::add)
     */
    public Player(Vector2D spawn, Consumer<Bullet> bulletSpawner) {
        // ── HRFC-004: constructor con SpriteHandle — sin legacySprite ────────
        // MovingObjects crea SpriteRenderer(handle) directamente.
        // No se pasa BufferedImage: elimina el frame incorrecto que podía
        // mostrarse durante el primer tick antes de AnimationController.start().
        super(spawn,
              PlayerAssets.handle,
              new PlayerPhysics(BASE_GRAVITY),
              SizeSyncMode.NONE);

        // ── Componentes de gameplay (Entity) ─────────────────────────────
        // HealthComponent gestiona la salud — PlayerStats ya no lo hace.
        addComponent(new HealthComponent(BASE_HP_MAX) {
            @Override
            protected void onDeath() {
                // Punto de extensión: notificar muerte del Player al bus de eventos,
                // activar pantalla de game over, etc. Por ahora vacío.
            }
        });
        // StatusEffectComponent: Player puede recibir efectos (veneno de trampa, etc.)
        addComponent(new StatusEffectComponent());

        // ── Estado y lógica específica del Player ─────────────────────────
        state = new PlayerState();
        stats = new PlayerStats();

        // Vincular HealthComponent a PlayerStats para que actúe como fachada
        // de solo lectura hacia la UI (cadena: LifeHUD → PlayerStats → HealthComponent).
        stats.bindHealth(getHealth());

        PlayerPhysics physics = (PlayerPhysics) getPhysics();
        controller = new PlayerController(physics, state);

        combat = new PlayerCombat(
            state,
            () -> getTransform().getPosition(),
            bulletSpawner
        );

        // Loadout inicial — responsabilidad de Player, no de PlayerCombat
        combat.setInitialWeapon(
            new Game.Items.Types.Weapons.WeaponSelected(
                new Game.Items.Types.Weapons.WeaponType.WeaponClass.WeaponEscopeta(),
                Game.Items.Types.Bullets.BulletType.SPRINGBULLET
            )
        );

        // ── Colisión y visual ─────────────────────────────────────────────
        ColliderComponent collider = getComponent(ColliderComponent.class);
        if (collider != null) {
            collider.setProfile(CollisionProfile.PLAYER);
            collider.setSize(15, 24);
            collider.setOffset(4, 0);
        }

        addComponent(new HitBoxComponent(Color.RED));
        // AnimationController ANTES que PlayerRenderer: PlayerRenderer.start()
        // busca AnimationController en el mismo objeto.
        addComponent(new AnimationController(PlayerAssets.handle));
        addComponent(new PlayerRenderer(state));

        // ── Inventario y equipamiento ─────────────────────────────────────
        // getMaxInventorySlots() es la única fuente de verdad para el tamaño del inventario.
        inventory     = new Inventory(stats.getMaxInventorySlots());
        equippedItems = new EquippedItems();

        // Establecer HP inicial (HealthComponent arranca en max; queremos BASE_HP).
        // initCurrentHP() fija el valor directamente sin disparar onDamage ni onDeath.
        if (BASE_HP < BASE_HP_MAX) {
            getHealth().initCurrentHP(BASE_HP);
        }
    }

    @Override
    public void update() {
        // Sincronizar el flag de estado con el valor autoritativo de la física.
        // onGround lo establece CollisionsSystem (FASE 0) en el frame anterior;
        // aquí solo lo copiamos a PlayerState para que el controlador y las
        // animaciones puedan consultarlo sin acceder directamente a la física.
        if (physicsComponent != null) {
            state.setEnElSuelo(physicsComponent.getPhysics().getOnGround());
        }

        controller.update();
        combat.update();

        // applyGravity() ya NO se llama aquí.
        // CollisionsSystem la aplica en FASE 0.5, DESPUÉS de actualizar onGround
        // en FASE 0. Esto elimina el bug donde la gravedad se acumulaba usando
        // el valor de onGround del frame anterior. Ver CollisionsSystem.java.

        super.update(); // actualiza todos los Component (HealthComponent, StatusEffectComponent, etc.)

        // PlayerStats no es un Component (no encaja en el ciclo de vida del engine:
        // no necesita start(), no se añade/quita en runtime, gestiona lógica de gameplay
        // pura). Se actualiza manualmente aquí, después de los Component, para que los
        // frames de invulnerabilidad decremente DESPUÉS de que el daño del frame actual
        // ya fue procesado por HealthComponent.
        stats.update();
    }

    // ── API de daño con invulnerabilidad ──────────────────────────────────

    /**
     * Aplica daño al Player respetando los frames de invulnerabilidad post-golpe.
     *
     * Usar para daño directo (colisión con enemigo, proyectil).
     * Para daño de efecto de estado (veneno, caída), usar damage() de Entity,
     * que va directo al HealthComponent sin verificar invulnerabilidad.
     */
    public void receiveDamage(int amount) {
        if (stats.isInvulnerable()) return;
        damage(amount);                      // shortcut de Entity → HealthComponent
        stats.triggerInvulnerability();
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public Vector2D         getPosition()    { return getTransform().getPosition(); }
    public PlayerState      getState()       { return state; }
    public PlayerController getController()  { return controller; }
    public PlayerCombat     getCombat()      { return combat; }
    public PlayerStats      getStats()       { return stats; }
    public Inventory        getInventory()   { return inventory; }
    public EquippedItems    getEquippedItems() { return equippedItems; }

    // ── Colisiones ────────────────────────────────────────────────────────
    // onCollisionWith(GameObjects) heredado de GameObjects — default vacío correcto.
    // Player no reacciona a colisiones directas; sus efectos los gestiona
    // la física (CollisionsSystem) o sistemas externos (enemy.onCollisionWith).
}
