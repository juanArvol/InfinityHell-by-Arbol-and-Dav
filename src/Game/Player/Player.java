package Game.Player;

import Game.Engine.Entity.Attributes.EntityAttributes;
import Game.Engine.Entity.Combat.AttackSources;
import Game.Engine.Entity.EntityInfoProvider;
import Game.Engine.Entity.Flags.EntityFlags;
import Game.Engine.Entity.Stats.EntityStats;
import Game.Engine.Entity.Stats.RuntimeStats;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.MovingObjects;
import Game.Engine.RenderEngine.Sprites.SizeSyncMode;
import Game.Gameplay.Aimm.AimSelection;
import Game.Items.Savement.EquippedItems;
import Game.Items.Savement.Inventory;
import Game.Items.Types.Ammulets.AmuletInventory;
import Game.Items.Types.Bullets.Definition.Bullet;
import Sprites.Entity.Player.PlayerAssets;
import java.util.function.Consumer;

/**
 * Jugador — entidad que compone y expone; los módulos especializados ejecutan.
 *
 * ── HRFC — Player Reengineering ───────────────────────────────────────────
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 *
 *   Player compone y expone.
 *   Los módulos especializados ejecutan.
 *   Engine proporciona las abstracciones genéricas.
 *   PlayerAssembler conecta ambos mundos.
 *
 * ── LO QUE PLAYER NO HACE ────────────────────────────────────────────────
 *
 *   ✗ No construye concretamente ningún subsistema.
 *   ✗ No instancia armas (WeaponPistola, ModifiedWeapon).
 *   ✗ No configura EntityStats ni RuntimeStats.
 *   ✗ No añade Components.
 *   ✗ No implementa lógica de Aim.
 *   ✗ No duplica stats de EntityStats/RuntimeStats.
 *   ✗ No implementa las leyes físicas.
 *
 * ── LO QUE PLAYER HACE ────────────────────────────────────────────────────
 *
 *   ✓ Recibe todos sus módulos ya construidos (inyección de dependencias).
 *   ✓ Coordina el ciclo de actualización de sus módulos en update().
 *   ✓ Implementa EntityInfoProvider — contrato de entidad genérica del Engine.
 *   ✓ Expone una API pública pequeña y semántica.
 *   ✓ Aplica daño con política de invulnerabilidad (receiveDamage).
 *
 * ── CONSTRUCCIÓN ──────────────────────────────────────────────────────────
 *
 *   No usar el constructor directamente.
 *   Usar PlayerAssembler.assemble() que es el único punto de entrada.
 *
 *   // Loadout estándar:
 *   Player player = PlayerAssembler.assemble(spawn, world::add, eventBus);
 *
 *   // Loadout custom usando API declarativa:
 *   PlayerLoadout loadout = PlayerLoadout
 *       .initialWeapons(WeaponType.PISTOLA)
 *       .initialBullets(BulletType.NORMALBULLET)
 *       .initialAmulets()
 *       .build();
 *   Player player = PlayerAssembler.assemble(spawn, world::add, eventBus, loadout);
 *
 * ── JERARQUÍA ─────────────────────────────────────────────────────────────
 *
 *   GameObjects → AbstractEntity → MovingObjects → Player
 *   Player implements EntityInfoProvider
 *
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 *
 *                        Player
 *                          │
 *            ┌─────────────┼──────────────┐
 *            │             │              │
 *            ▼             ▼              ▼
 *      PlayerStats    PlayerCombat   PlayerController
 *            │             │              │
 *            │             ▼              ▼
 *            │       WeaponInventory  PlayerPhysics
 *            │
 *            ▼
 *      Entity Systems
 *            │
 *   ┌────────┼───────────┐
 *   ▼        ▼           ▼
 * EntityStats RuntimeStats HealthComponent
 */
public class Player extends MovingObjects implements EntityInfoProvider {

    // ── EntityInfoProvider — única fuente de verdad de stats ─────────────
    private final EntityStats      entityStats;
    private final RuntimeStats     runtimeStats;
    private final EntityFlags      entityFlags;
    private final EntityAttributes entityAttributes;
    private final AttackSources    attackSources;

    // ── Módulos específicos del Player ────────────────────────────────────
    private final PlayerController controller;
    private final PlayerCombat     combat;
    private final PlayerStats      playerStats;
    private final PlayerState      state;
    private final AmuletInventory  amulets;

    // ── Runtime del Player ────────────────────────────────────────────────
    private PlayerRuntime playerRuntime;

    // ── Inventario ────────────────────────────────────────────────────────
    private Inventory     inventory;
    private EquippedItems equippedItems;

    // ── Constructor — solo inyección, sin construcción concreta ──────────
    //
    // Este constructor es package-private. El único punto de entrada público
    // es PlayerAssembler.assemble(). No crear Player directamente.

    Player(Vector2D spawn,
           PlayerPhysics physics,
           EntityStats entityStats,
           RuntimeStats runtimeStats,
           EntityFlags entityFlags,
           EntityAttributes entityAttributes,
           AttackSources attackSources,
           PlayerState state,
           PlayerStats playerStats,
           AmuletInventory amulets,
           PlayerController controller,
           PlayerCombat combat) {

        super(spawn, PlayerAssets.handle, physics, SizeSyncMode.NONE);

        this.entityStats      = entityStats;
        this.runtimeStats     = runtimeStats;
        this.entityFlags      = entityFlags;
        this.entityAttributes = entityAttributes;
        this.attackSources    = attackSources;
        this.state            = state;
        this.playerStats      = playerStats;
        this.amulets          = amulets;
        this.controller       = controller;
        this.combat           = combat;
    }

    // ── Post-construcción — llamado por PlayerAssembler ───────────────────

    /**
     * Inyecta el inventario y los ítems equipados tras la construcción.
     * Solo PlayerAssembler llama este método.
     *
     * @param inventory     inventario del jugador
     * @param equippedItems ítems actualmente equipados
     */
    void initInventory(Inventory inventory, EquippedItems equippedItems) {
        this.inventory     = inventory;
        this.equippedItems = equippedItems;
    }

    /**
     * Inyecta el runtime del Player tras la construcción.
     * Solo PlayerAssembler llama este método.
     *
     * @param playerRuntime runtime del Player para gestión de inventario y equipamiento
     */
    void initRuntime(PlayerRuntime playerRuntime) {
        this.playerRuntime = playerRuntime;
    }

    // ── Update — coordinación del ciclo de juego ──────────────────────────

    /**
     * Actualiza el Player y todos sus subsistemas.
     *
     * ── HRFC — Unified DeltaTime Migration & Temporal Model Completion ────
     *
     * PROPAGACIÓN TEMPORAL:
     *   Player recibe deltaTime de WorldManager y lo propaga a:
     *     - PlayerCombat     → para cooldowns de armas
     *     - PlayerStats      → para timers de invulnerabilidad
     *     - Engine Components → via super.update(deltaTime)
     *
     * @param deltaTime tiempo del simulation step en segundos
     */
    @Override
    public void update(double deltaTime) {
        // 1. Sincronizar onGround de física → PlayerState
        //    (necesario para Controller y Renderer)
        if (physicsComponent != null) {
            state.setEnElSuelo(physicsComponent.getPhysics().getOnGround());
        }

        // 2. Aim — calcular dirección y escribirla en PlayerState
        AimSelection.apply(state);

        // 3. Controller — procesar input de movimiento
        controller.update(deltaTime);

        // 4. Runtime — coordinar inventario y equipamiento
        if (playerRuntime != null) {
            playerRuntime.update();
        }

        // 5. Combat — procesar input de disparo y cooldowns
        combat.update(deltaTime);

        // 6. Engine Components (HealthComponent, StatusEffectComponent, Renderer…)
        super.update(deltaTime);

        // 7. PlayerStats — actualizar timers de invulnerabilidad
        //    (después de super.update() para que el daño del frame ya se haya aplicado)
        playerStats.update(deltaTime);
    }

    // ── API de daño con invulnerabilidad ──────────────────────────────────

    /**
     * Aplica daño al Player respetando frames de invulnerabilidad post-golpe.
     *
     * Usar para daño directo (proyectil, contacto con enemigo).
     * Para daño de efecto de estado usar {@code damage(amount)} heredado,
     * que no aplica invulnerabilidad.
     *
     * @param amount cantidad de daño a aplicar.
     */
    public void receiveDamage(int amount) {
        if (playerStats.isInvulnerable()) return;
        damage(amount);                         // AbstractEntity → HealthComponent
        playerStats.triggerInvulnerability();
    }

    // ── EntityInfoProvider — integración con Engine Systems ──────────────

    /**
     * EntityStats base para integración con Engine Systems.
     * 
     * ── HRFC — Player Reengineering v2 ────────────────────────────────────
     * 
     * Para código específico del Player, usar getPlayerStats().getEntityStats()
     * en lugar de acceso directo. EntityInfoProvider se mantiene para
     * integración con sistemas genéricos del Engine (StatusEffectSystem, etc.)
     */
    @Override public EntityStats      getStats()         { return entityStats;      }
    
    /**
     * RuntimeStats efectivos para integración con Engine Systems.
     * Para código específico del Player, usar getPlayerStats().getRuntimeStats().
     */
    @Override public RuntimeStats     getRuntimeStats()  { return runtimeStats;     }
    
    /**
     * EntityFlags para integración con Engine Systems.
     * Para código específico del Player, usar getPlayerStats().getEntityFlags().
     */
    @Override public EntityFlags      getFlags()         { return entityFlags;      }
    
    /**
     * EntityAttributes para integración con Engine Systems.
     * Para código específico del Player, usar getPlayerStats().getAttributes().
     */
    @Override public EntityAttributes getAttributes()    { return entityAttributes; }
    
    /**
     * AttackSources para integración con Engine Systems.
     * Para código específico del Player, usar getPlayerStats().getAttackSources().
     */
    @Override public AttackSources    getAttackSources() { return attackSources;    }

    // ── API pública del Player ────────────────────────────────────────────

    /** Estado lógico del Player (movimiento, aim, combate). */
    public PlayerState getState()           { return state;         }

    /** Controlador de input. */
    public PlayerController getController() { return controller;    }

    /** Módulo de combate. */
    public PlayerCombat getCombat()         { return combat;        }

    /**
     * Fachada de dominio: acceso contextual a stats, salud e invulnerabilidad.
     * No usar para leer stats de gameplay — usar getStats() / getRuntimeStats().
     */
    public PlayerStats getPlayerStats()     { return playerStats;   }

    /** Runtime del Player — coordina inventario y equipamiento. */
    public PlayerRuntime getRuntime()       { return playerRuntime; }

    /** Inventario de ítems de la run. */
    public Inventory getInventory()         { return inventory;     }

    /** Ítems actualmente equipados. */
    public EquippedItems getEquippedItems() { return equippedItems; }

    /** Inventario de amuletos de la run. */
    public AmuletInventory getAmulets()     { return amulets;       }

    /** Posición actual en el mundo (shortcut de conveniencia). */
    public Vector2D getPosition()           { return getTransform().getPosition(); }

    // ── Factory — punto de entrada recomendado ────────────────────────────

    /**
     * Crea un Player con el loadout por defecto.
     * Delega en {@link PlayerAssembler#assemble(Vector2D, Consumer, Game.Engine.GameEventBus)}.
     *
     * @param spawn         posición inicial
     * @param bulletSpawner callback para añadir balas al mundo
     * @param eventBus      bus de eventos
     * @return Player completamente ensamblado
     */
    public static Player create(Vector2D spawn,
                                Consumer<Bullet> bulletSpawner,
                                Game.Engine.GameEventBus eventBus,
                                PlayerLoadout loadout) {
        return PlayerAssembler.assemble(spawn, bulletSpawner, eventBus, loadout);
    }
}
