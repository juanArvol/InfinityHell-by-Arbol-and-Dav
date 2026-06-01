package Game.Player;

import java.util.List;
import java.util.function.Consumer;

import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Weapons.WeaponInventory;
import Game.Items.Types.Weapons.WeaponSelected;
import Inputs.MouseInput;
import Inputs.Listeners.MouseActionListener;

/**
 * Combate del jugador.
 *
 * ── REFACTOR 1: ELIMINAR WorldManager (Dependency Injection) ─────────────
 *
 * PROBLEMA ORIGINAL:
 *   PlayerCombat accedía al World mediante el singleton WorldManager:
 *
 *     World world = WorldManager.getInstance().getCurrentWorld();
 *     world.add(bullet);
 *
 *   Esto creaba tres problemas:
 *   (a) Acoplamiento duro: PlayerCombat conoce WorldManager, World y todo
 *       lo que WorldManager implica. Cambiar el ciclo de vida del World
 *       afecta a PlayerCombat.
 *   (b) Testabilidad: imposible testear disparo sin un World real.
 *   (c) Responsabilidad mezclada: PlayerCombat tiene que saber CÓMO añadir
 *       cosas al mundo, no solo disparar.
 *
 * SOLUCIÓN:
 *   Sustituir el acceso al singleton por un Consumer<Bullet> inyectado en
 *   el constructor. El que construye PlayerCombat (Player o el bootstrap)
 *   decide cómo se añaden las balas al mundo:
 *
 *     new PlayerCombat(player, state, bullet -> world.add(bullet));
 *
 *   PlayerCombat no conoce World ni WorldManager. Solo sabe "cuando disparo,
 *   llamo a este callback con la bala resultante".
 *
 * BENEFICIO:
 *   - Sin imports de World ni WorldManager en este archivo.
 *   - Fácil de testear: pasar un Consumer que acumule en una lista.
 *   - Bajo acoplamiento: si World cambia su API, PlayerCombat no cambia.
 *
 * ── REFACTOR 2: EXTRAER CONFIGURACIÓN DE LOADOUT ─────────────────────────
 *
 * PROBLEMA ORIGINAL:
 *   El constructor de PlayerCombat construía y configuraba el arma inicial:
 *
 *     inventory.addWeapon(new WeaponSelected(new WeaponEscopeta(), SPRINGBULLET));
 *
 *   Mezcla responsabilidades: combate + configuración inicial del loadout.
 *   Si queremos cargar loadouts desde datos (JSON, save), no hay punto de
 *   entrada limpio.
 *
 * SOLUCIÓN:
 *   Extraer la configuración inicial a un método setInitialWeapon() separado,
 *   que Player (o un sistema de loadout externo) llama después de construir
 *   PlayerCombat. El constructor queda limpio y sin conocimiento de tipos
 *   de arma concretos.
 *
 * BENEFICIO:
 *   - PlayerCombat es agnóstico al loadout específico.
 *   - Facilita futuros sistemas: loadout desde save, loadout aleatorio,
 *     selección por clase de personaje.
 *
 * ── REFACTOR 3: DESACOPLAR DE Player COMPLETO ────────────────────────────
 *
 * PROBLEMA ORIGINAL:
 *   PlayerCombat recibía el Player completo y accedía a player.getPosition().
 *   Solo necesitaba la posición para calcular el origen del disparo.
 *
 * SOLUCIÓN:
 *   Inyectar un Supplier<Vector2D> para la posición en lugar del Player completo.
 *   Esto rompe la dependencia circular PlayerCombat ↔ Player.
 *
 * BENEFICIO:
 *   - PlayerCombat no depende de Player. Puede reutilizarse para otros
 *     combatientes sin arrastrar Player.
 */
public class PlayerCombat implements MouseActionListener {

    private final PlayerState         state;
    private final WeaponInventory     inventory;
    private final java.util.function.Supplier<Vector2D> positionSupplier;
    private final Consumer<Bullet>    bulletSpawner;

    /** Edge click recibido este frame (disparo puntual). */
    private boolean clickFired = false;

    /**
     * @param state           estado del jugador (dirección, recarga, aim)
     * @param positionSupplier proveedor de posición actual del jugador
     * @param bulletSpawner   callback para añadir balas al mundo
     */
    public PlayerCombat(
            PlayerState state,
            java.util.function.Supplier<Vector2D> positionSupplier,
            Consumer<Bullet> bulletSpawner
    ) {
        this.state            = state;
        this.positionSupplier = positionSupplier;
        this.bulletSpawner    = bulletSpawner;
        this.inventory        = new WeaponInventory();
    }

    /**
     * Configura el arma inicial (loadout).
     * Separado del constructor para que la responsabilidad de qué armas
     * tiene el jugador al inicio quede fuera de PlayerCombat.
     *
     * Llamar desde Player o desde un sistema de loadout externo.
     */
    public void setInitialWeapon(WeaponSelected weapon) {
        inventory.addWeapon(weapon);
    }

    public WeaponInventory getInventory() {
        return inventory;
    }

    // ── MouseActionListener ───────────────────────────────────────────────

    @Override
    public void onMouseAction(String action, float virtualX, float virtualY) {
        if ("leftClick".equals(action)) {
            clickFired = true;
        }
    }

    // ── Update ────────────────────────────────────────────────────────────

    public void update() {
        WeaponSelected currentWeapon = inventory.getCurrentWeapon();
        if (currentWeapon == null) return;

        // ── Gestión de recarga ────────────────────────────────────────────
        // Activar recarga si el jugador presiona la tecla de recarga y el arma
        // tiene munición que recargar.
        boolean reloadKeyPressed = Inputs.KeyBoard.getState("reload");
        if (reloadKeyPressed && !state.isReloading() && !currentWeapon.isFullyLoaded()) {
            state.setReloading(true);
        }

        if (state.isReloading()) {
            currentWeapon.reload();
            // Desactivar el flag cuando el arma haya terminado de recargar.
            // weapon.isReloading() es false una vez que la recarga se completa.
            if (!currentWeapon.isReloading()) {
                state.setReloading(false);
            }
        }

        boolean holding = MouseInput.getButtonState("leftPressed");
        Vector2D pos    = positionSupplier.get();
        Vector2D aim    = state.getAimDirection();

        List<Bullet> newBullets = currentWeapon.handleInput(
            holding,
            clickFired,
            pos.getX(),
            pos.getY(),
            state.isDer(),
            aim
        );

        clickFired = false;

        // Delegar al spawner inyectado — sin conocer World ni WorldManager
        for (Bullet b : newBullets) {
            bulletSpawner.accept(b);
        }

        currentWeapon.update();
    }

    public void nextWeapon()     { inventory.nextWeapon();     }
    public void previousWeapon() { inventory.previousWeapon(); }
}
