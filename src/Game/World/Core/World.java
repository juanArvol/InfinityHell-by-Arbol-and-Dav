package Game.World.Core;

import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.Engine.GameObjects;
import Game.Player.Player;
import Game.World.WorldObjects.WorldObjectsContainer;

/**
 * Mundo del juego — estado puro, sin responsabilidades de render ni de cámara.
 *
 * ── HRFC-001: Camera extraída del dominio del mundo ──────────────────────
 *
 * La cámara ya NO vive en World. World es estado de dominio puro:
 * entidades, física, lógica. La cámara pertenece al Engine (GameCamera)
 * y su ciclo de vida lo gestiona el sistema de render.
 *
 * World expone únicamente:
 *   - getTrackedPosition(): la posición del objeto rastreado (para que
 *     el CameraController sepa hacia dónde moverse). Puede ser null si no
 *     hay ningún objeto rastreado.
 *
 * El comportamiento de seguimiento (lerp, clamp, etc.) ya no ocurre aquí.
 *
 * ── Conexión de WorldEnemyUpdater ────────────────────────────────────────
 *
 *   WorldObjectsContainer acepta un objectUpdater (Consumer<List<GameObjects>>)
 *   inyectable. World lo configura cuando el player es asignado como trackTarget:
 *
 *     objects.setObjectUpdater(list -> WorldEnemyUpdater.updateAll(list, player));
 *
 *   Cuando no hay player rastreado, el updater por defecto (obj.update()) se usa.
 *   WorldObjectsContainer no importa Player ni Enemy.
 */
public class World {

    private int width;
    private int height;
    private final WorldCoordinator coordinate;

    private final WorldObjectsContainer objects = new WorldObjectsContainer();

    /** Objeto cuya posición se expone al sistema de cámara del Engine. */
    private GameObjects trackTarget;

    public World(int width, int height, WorldCoordinator coordinate) {
        this.width      = width;
        this.height     = height;
        this.coordinate = coordinate;
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void update() {
        objects.update();
        // El seguimiento de cámara ya no ocurre aquí.
        // GameCamera + CameraController lo gestionan en el game loop.
    }

    // ── Gestión de objetos ────────────────────────────────────────────────────

    public void add(GameObjects obj) {
        objects.add(obj);
    }

    public void remove(GameObjects obj) {
        objects.remove(obj);
    }

    public WorldObjectsContainer getObjectsContainer() {
        return objects;
    }

    // ── Tracking de cámara ────────────────────────────────────────────────────

    /**
     * Registra el objeto a rastrear para el sistema de cámara del Engine.
     *
     * Si el objeto es un Player, también configura el objectUpdater del contenedor
     * para que los Enemy reciban EnemyContext correcto en cada update().
     *
     * El comportamiento de seguimiento de cámara (lerp, clamp) ocurre en
     * FollowCameraController, que consulta getTrackedPosition() cada tick.
     *
     * @param obj objeto a seguir (generalmente el player); puede ser null para
     *            liberar el tracking.
     */
    public void setTrackTarget(GameObjects obj) {
        this.trackTarget = obj;

        if (obj instanceof Player player) {
            objects.setObjectUpdater(list -> WorldEnemyUpdater.updateAll(list, player));
        } else if (obj == null) {
            objects.setObjectUpdater(null);
        }
    }

    /**
     * Posición actual del objeto rastreado en coordenadas de mundo.
     *
     * El CameraController la consulta cada tick para calcular hacia dónde
     * mover la cámara. Retorna null si no hay ningún objeto rastreado o si
     * el objeto rastreado no tiene transform válido.
     *
     * @return posición del target o null.
     */
    public Vector2D getTrackedPosition() {
        if (trackTarget == null) return null;
        var pos = trackTarget.getTransform().getPosition();
        return new Vector2D(pos.getX(), pos.getY());
    }

    /**
     * El objeto rastreado actualmente.
     * Útil para sistemas que necesitan más que la posición (WorldTransitionService).
     */
    public GameObjects getTrackTarget() {
        return trackTarget;
    }

    // ── Dimensiones ───────────────────────────────────────────────────────────

    public void resize(int newWidth, int newHeight) {
        this.width  = newWidth;
        this.height = newHeight;
    }

    public int getWidth()                   { return width;      }
    public int getHeight()                  { return height;     }
    public WorldCoordinator getCoordinate() { return coordinate; }
}
