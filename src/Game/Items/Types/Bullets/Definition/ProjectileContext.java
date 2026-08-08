package Game.Items.Types.Bullets.Definition;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Bullets.ProjectileBlueprint;

/**
 * Contexto de interacción con el mundo para proyectiles y behaviors.
 *
 * ── HRFC — Projectile Construction & Transformation Pipeline ─────────────
 *
 * Algunos behaviors necesitan interactuar con el mundo durante su ciclo de
 * vida: spawnear proyectiles secundarios, aplicar efectos de área, buscar
 * entidades cercanas, notificar al mundo de una explosión, etc.
 *
 * Sin ProjectileContext, los behaviors tienen dos opciones malas:
 *   1. Depender de singletons arbitrarios (WorldManager.getInstance()...).
 *   2. Depender de GameEventBus.GLOBAL para todo (el bus se convierte en
 *      mutación implícita disfrazada de eventos).
 *
 * ProjectileContext proporciona una superficie mínima, explícita y orientada
 * a las necesidades reales de proyectiles.
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 *   ProjectileContext  = cómo un behavior solicita operaciones del mundo.
 *   ProjectileEvents   = qué eventos emite un proyectil durante su vida.
 *   GameEventBus       = infraestructura de comunicación entre sistemas.
 *
 * El contexto es inyectado, no global. Los behaviors lo reciben como parámetro.
 * Quien construye el contexto controla exactamente qué capacidades expone.
 *
 * ── OPERACIONES DISPONIBLES ───────────────────────────────────────────────
 *
 *   spawnProjectile(blueprint, position, direction)
 *       Spawnear un proyectil secundario (explosión, fragmentación, etc.)
 *
 *   spawnProjectile(blueprint, origin, target)
 *       Versión con posición objetivo, la dirección se calcula automáticamente.
 *
 *   findEntitiesInRadius(center, radius)
 *       Buscar entidades en un radio (área de efecto, explosiones).
 *
 * ── EXTENSIÓN ─────────────────────────────────────────────────────────────
 *
 * Si un behavior necesita más capacidades del mundo, añadir métodos aquí.
 * No añadir métodos sin necesidad real — mantener el contexto pequeño.
 *
 * ── IMPLEMENTACIÓN ────────────────────────────────────────────────────────
 *
 * ProjectileContext es una interfaz. La implementación concreta la provee
 * el sistema que gestiona el mundo (GameWorldBootstrap, WorldController, etc.)
 * cuando inicializa el ciclo de disparo.
 *
 * Una implementación vacía NullProjectileContext está disponible para
 * behaviors que no la usen o para tests.
 *
 * ── USO EN BEHAVIORS ─────────────────────────────────────────────────────
 *
 *   // En onExpire():
 *   @Override
 *   public void onExpire(Bullet bullet, ProjectileContext ctx) {
 *       // Fragmentar el proyectil al expirar
 *       Vector2D pos = bullet.getTransform().getPosition();
 *       for (int i = 0; i < 5; i++) {
 *           Vector2D dir = randomDirection();
 *           ctx.spawnProjectile(fragmentBlueprint, pos, dir);
 *       }
 *   }
 */
public interface ProjectileContext {

    // ── Spawn ─────────────────────────────────────────────────────────────

    /**
     * Spawnea un proyectil secundario en el mundo.
     *
     * La posición y dirección ya están calculadas por el caller.
     *
     * @param blueprint definición del proyectil a spawnear
     * @param position  posición de spawn en coordenadas del mundo
     * @param direction dirección normalizada de vuelo
     */
    void spawnProjectile(ProjectileBlueprint blueprint,
                         Vector2D position,
                         Vector2D direction);

    /**
     * Spawnea un proyectil apuntando a una posición objetivo.
     * La dirección se calcula internamente como normalize(target - origin).
     *
     * @param blueprint definición del proyectil
     * @param origin    posición de spawn
     * @param target    posición objetivo (puede ser null, usará dirección (1,0))
     */
    default void spawnProjectileToward(ProjectileBlueprint blueprint,
                                       Vector2D origin,
                                       Vector2D target) {
        Vector2D direction;
        if (target != null) {
            double dx = target.getX() - origin.getX();
            double dy = target.getY() - origin.getY();
            double len = Math.hypot(dx, dy);
            direction = (len > 1e-6)
                    ? new Vector2D(dx / len, dy / len)
                    : new Vector2D(1, 0);
        } else {
            direction = new Vector2D(1, 0);
        }
        spawnProjectile(blueprint, origin, direction);
    }

    // ── Búsqueda de entidades ─────────────────────────────────────────────

    /**
     * Retorna las entidades del mundo dentro de un radio dado.
     *
     * Usado para efectos de área (explosiones, daño en zona).
     * La lista retornada es una snapshot — no modificar.
     *
     * @param center posición central del área
     * @param radius radio de búsqueda en unidades del mundo
     * @return lista de AbstractEntity dentro del radio (puede estar vacía)
     */
    java.util.List<? extends Game.Engine.AbstractEntity> findEntitiesInRadius(
            Vector2D center, double radius);

    // ── Implementación nula ────────────────────────────────────────────────

    /**
     * Contexto vacío — todas las operaciones son no-ops.
     *
     * Usar cuando no hay un mundo activo (tests, behaviors que no usan el ctx)
     * o como placeholder hasta que el contexto real esté disponible.
     */
    ProjectileContext NULL = new ProjectileContext() {
        @Override
        public void spawnProjectile(ProjectileBlueprint blueprint,
                                    Vector2D position,
                                    Vector2D direction) {
            // no-op
        }

        @Override
        public java.util.List<? extends Game.Engine.AbstractEntity>
                findEntitiesInRadius(Vector2D center, double radius) {
            return java.util.List.of();
        }
    };
}
