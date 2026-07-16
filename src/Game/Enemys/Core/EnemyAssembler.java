package Game.Enemys.Core;

import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;

/**
 * Contrato base del ensamblador de Enemy.
 *
 * ── HRFC-005 — Responsabilidad única ────────────────────────────────────
 * Un Assembler construye completamente un Enemy usando el framework Core.
 * Su única responsabilidad es ensamblarlo correctamente.
 * El Assembler NO participa en el ciclo de vida del Enemy después de crearlo.
 *
 * ── Qué hace un Assembler ────────────────────────────────────────────────
 *   1. Instancia Enemy con su EnemyDefinition.
 *   2. Reemplaza la física del engine con la configurada en la definición.
 *   3. Ajusta el collider según las dimensiones de la definición.
 *   4. Configura los controladores: IA, movimiento, ataques, fases.
 *   5. Añade EnemyComponents opcionales (aura, regen, etc.).
 *   6. Establece las variables (velocidad, daño, rangos).
 *   7. Añade los AnimationController y componentes visuales del engine.
 *   8. Si el enemy tiene fases, llama phaseController.start(enemy).
 *   9. Retorna el Enemy completamente configurado.
 *
 * ── Qué NO hace un Assembler ─────────────────────────────────────────────
 *   - No llama update() ni draw().
 *   - No añade el enemy al mundo (eso lo hace EnemyFactory o EnemySpawner).
 *   - No guarda referencias al enemy después de retornarlo.
 *
 * ── Por qué una clase abstracta en lugar de interfaz ─────────────────────
 * EnemyAssembler proporciona assemble() como template: crea el Enemy con la
 * definición base y luego llama configure() para que la subclase lo
 * especialice. Esto garantiza que todos los assemblers pasan por el mismo
 * proceso de inicialización del engine (física, collider) sin duplicar código.
 *
 * ── Ejemplo de implementación ────────────────────────────────────────────
 *
 *   public class ZombieAssembler extends EnemyAssembler {
 *
 *       @Override
 *       protected EnemyDefinition definition() {
 *           return EnemyDefinition.builder()
 *               .sprite(EnemyAssets.normalHandle)
 *               .health(100)
 *               .physics(EnemyPhysicsConfig.groundStandard())
 *               .collider(24, 30)
 *               .build();
 *       }
 *
 *       @Override
 *       protected void configure(Enemy enemy) {
 *           // IA
 *           enemy.getAIController().setBehavior(new AggressiveBehavior());
 *           // Movimiento
 *           enemy.getMovementController().setStrategy(new GroundMovement());
 *           // Variables
 *           enemy.getVariables()
 *               .set(EnemyVariables.Keys.SPEED, 1.0)
 *               .set(EnemyVariables.Keys.DAMAGE, 10);
 *           // Visual
 *           enemy.addComponent(new AnimationController(
 *               EnemyAssets.normalHandle, "idle"
 *           ));
 *       }
 *   }
 */
public abstract class EnemyAssembler {

    /**
     * Construye y retorna un Enemy completamente configurado en la posición dada.
     *
     * El proceso es siempre el mismo:
     *   1. Obtiene la definición del enemigo.
     *   2. Crea el Enemy con sprite y HP.
     *   3. Reemplaza la física del engine con la de la definición.
     *   4. Ajusta el collider.
     *   5. Llama configure() para que la subclase especialice los controladores.
     *
     * @param position posición inicial en el mundo.
     * @return Enemy completamente configurado, listo para añadir al mundo.
     */
    public final Enemy assemble(Vector2D position) {
        EnemyDefinition def = definition();

        // La física correcta se pasa directamente al constructor de Enemy,
        // que la reenvía a MovingObjects → Physics2DComponent (que es final).
        Enemy enemy = new Enemy(position, def.sprite, def.maxHealth, def.physics);

        // Ajustar collider con las dimensiones de la definición
        ColliderComponent col = enemy.getComponent(ColliderComponent.class);
        if (col != null) {
            col.setSize(def.colliderW, def.colliderH);
        }

        // Delegar en la subclase la configuración específica del enemy
        configure(enemy);

        return enemy;
    }

    // ── Template methods ──────────────────────────────────────────────────

    /**
     * Retorna la definición estática del enemy: sprite, HP, física, collider.
     */
    protected abstract EnemyDefinition definition();

    /**
     * Configura el Enemy recién creado: controladores, variables, componentes.
     * La subclase no necesita preocuparse por la física ni el collider — ya
     * están configurados cuando este método es llamado.
     *
     * @param enemy el Enemy a configurar.
     */
    protected abstract void configure(Enemy enemy);
}
