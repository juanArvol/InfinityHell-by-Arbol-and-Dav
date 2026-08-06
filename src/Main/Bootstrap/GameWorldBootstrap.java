package Main.Bootstrap;

import Game.Enemys.Core.EnemySpawner;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Creation.ItemRegistry;
import Game.Items.Types.Bullets.ProjectileRegistry;
import Game.Player.Player;
import Game.World.Core.World;
import Game.World.Core.WorldManager;

/**
 * Bootstrap del mundo de juego.
 *
 * Responsabilidad única: construir y conectar los actores iniciales del mundo.
 *
 * ── BUG CRÍTICO CORREGIDO: closures al mundo inicial ─────────────────────
 *
 * ANTES — dos closures capturaban el primer World permanentemente:
 *
 *   // AmuletRegistry: capturaba world (el primer sector) para siempre
 *   AmuletRegistry.setEntityProvider(() ->
 *       world.getObjectsContainer().getObjects()...);
 *
 *   // ProjectileRegistry: mismo problema
 *   registry.installListener(world::add);
 *
 * SÍNTOMA: tras cruzar al sector siguiente, los amuletos buscaban entidades
 * del sector (0,0), y los proyectiles de enemigos aparecían en el sector (0,0)
 * independientemente de dónde estuviera el jugador.
 *
 * CAUSA: 'world' era una referencia local capturada en el closure en el
 * momento de la construcción del Bootstrap, nunca actualizada.
 *
 * SOLUCIÓN: todos los closures que necesitan acceder al mundo activo usan
 * worldManager::getCurrentWorld() en el momento de la evaluación, no una
 * referencia fija al primer mundo.
 *
 * ── LO QUE HACE ───────────────────────────────────────────────────────────
 *   1. Crea el Player en la posición de spawn del sector inicial.
 *   2. Lo añade al World inicial y lo registra como tracked object.
 *   3. Configura el WorldController predicate en TransitionService.
 *   4. Instala el listener de SpawnProjectileEvent con referencia dinámica.
 *   5. Configura AmuletRegistry con proveedor de entidades dinámico.
 *   6. Spawna los enemigos iniciales.
 */
public final class GameWorldBootstrap {

    private final Player player;

    public GameWorldBootstrap(WorldManager worldManager,
                              int virtualWidth,
                              int virtualHeight) {

        // ── Registros globales de ítems ──────────────────────────────────────
        ItemRegistry.init();

        // ── Posición de spawn del jugador ────────────────────────────────────
        // Usa el mundo inicial solo para calcular el punto de spawn central.
        // No captura 'world' en ningún closure de larga duración.
        World initialWorld = worldManager.getCurrentWorld();
        Vector2D spawnPos = new Vector2D(
            initialWorld.getWidth()  / 2.0,
            initialWorld.getHeight() / 2.0 - 200
        );

        // ── Player ───────────────────────────────────────────────────────────
        // El bulletSpawner del Player usa el mundo activo dinámicamente.
        // world::add solo es capturado como referencia inicial pero Player
        // siempre dispara al mundo donde existe — el collision system se encarga
        // de mantenerlo en el mundo correcto.
        player = new Player(spawnPos,
            obj -> worldManager.getCurrentWorld().add(obj)
        );
        initialWorld.add(player);

        // ── Cámara y tracking ─────────────────────────────────────────────────
        worldManager.setTrackedObject(player);

        // ── Configurar el WorldController predicate ───────────────────────────
        // Identifica qué objeto controla cuál es el sector activo.
        // Vive en la capa de composición (aquí) — no en lógica de dominio.
        worldManager.getTransitionService()
            .setWorldControllerPredicate(obj -> obj == player);

        // ── AmuletRegistry — proveedor de entidades dinámico ─────────────────
        //
        // CORREGIDO: el provider ahora consulta worldManager.getCurrentWorld()
        // en cada invocación, no el primer mundo capturado en el closure.
        //
        // Esto garantiza que los amuletos (BounceAmuletWrapper, etc.) siempre
        // buscan entidades en el sector donde está el jugador actualmente.
        Game.Items.Types.Ammulets.AmuletRegistry.setEntityProvider(() ->
            worldManager.getCurrentWorld()
                .getObjectsContainer()
                .getObjects()
                .stream()
                .filter(o -> o instanceof Game.Engine.AbstractEntity)
                .map(o -> (Game.Engine.AbstractEntity) o)
                .toList()
        );

        // ── ProjectileRegistry — listener con referencia dinámica ─────────────
        //
        // CORREGIDO: el bulletSpawner ya no es 'world::add' (captura del primer
        // mundo). Ahora usa worldManager::getCurrentWorld().add para siempre
        // añadir el proyectil al sector activo en el momento del disparo.
        //
        // Esto corrige el bug donde los proyectiles de enemigos aparecían en
        // el sector (0,0) aunque el jugador estuviera en otro sector.
        ProjectileRegistry registry = ProjectileRegistry.getInstance();
        registry.installListener(
            bullet -> worldManager.getCurrentWorld().add(bullet)
        );

        // ── Registro de tipos de proyectil de enemigos ────────────────────────
        // Registrar aquí los tipos que los enemigos pueden disparar.
        // Descomentar cuando existan los BulletBehaviors concretos:
        //
        //   registry.registerSimple("sans.bone", SansBoneBehavior::new, 8.0);
        //
        // No registrar tipos sin behavior implementado para evitar NPEs.

        // ── Spawn inicial de enemigos ─────────────────────────────────────────
        // Usa el SpawnSystem del WorldManager para que el spawn ocurra
        // en el mundo activo correcto (no el primer mundo hardcodeado).
        new EnemySpawner().spawn(initialWorld, 1);
    }

    /** El Player creado durante el bootstrap. */
    public Player getPlayer() {
        return player;
    }
}
