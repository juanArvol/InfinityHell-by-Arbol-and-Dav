package Game.Items.Types.Ammulets.Effects;

import Game.Engine.AbstractEntity;
import Game.Engine.Events.GameEventBus;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.BulletComport.BulletBehaviorWrapper;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.Definition.ProjectileEvents;
import java.util.List;
import java.util.function.Supplier;

/**
 * Wrapper de rebote entre entidades — "Piedra del Eco".
 *
 * Al impactar una entidad, busca la entidad AbstractEntity más cercana dentro
 * de un radio de búsqueda y redirige el proyectil hacia ella.
 * Cada copia del amuleto añade +1 salto.
 *
 * ── DISEÑO ────────────────────────────────────────────────────────────────
 *
 * El wrapper necesita la lista de entidades del mundo para buscar el siguiente
 * objetivo. Sigue el mismo patrón de inyección que ExplosiveModifier:
 * recibe un Supplier<List<AbstractEntity>> en construcción. Esto evita el
 * acoplamiento a WorldManager y hace el wrapper testeable.
 *
 * ── CÓMO SE CREA ──────────────────────────────────────────────────────────
 *
 *   // En AmuletRegistry (con acceso al world):
 *   register(new AmuletDefinition("echo_stone", ..., new AmuletEffect() {
 *       public BulletBehavior wrapBehavior(BulletBehavior base) {
 *           return new BounceAmuletWrapper(base, 1, entityProvider);
 *       }
 *   }));
 *
 *   // entityProvider se construye cuando el jugador está en un mundo:
 *   Supplier<List<AbstractEntity>> entityProvider = () ->
 *       world.getObjectsContainer().getObjects().stream()
 *           .filter(o -> o instanceof AbstractEntity)
 *           .map(o -> (AbstractEntity) o)
 *           .toList();
 *
 * ── MODO SIN PROVEEDOR ────────────────────────────────────────────────────
 *
 * Si no se provee un entityProvider (o se pasa null), el amuleto aplica el
 * daño normal pero no rebota — es un degradado elegante en lugar de un crash.
 * AmuletRegistry conecta el proveedor cuando inicializa el mundo.
 */
public class BounceAmuletWrapper extends BulletBehaviorWrapper {

    /** Radio de búsqueda del siguiente objetivo en unidades del mundo. */
    private static final double SEARCH_RADIUS = 200.0;

    private final int maxBounces;
    private int bounceCount = 0;

    /**
     * Proveedor de entidades candidatas para el rebote.
     * Inyectado para desacoplar el wrapper del World.
     * Null = sin rebote (degradado graceful).
     */
    private final Supplier<List<? extends AbstractEntity>> entityProvider;

    // ── Constructores ─────────────────────────────────────────────────────

    /**
     * Constructor con proveedor de entidades.
     *
     * @param inner          behavior base a decorar
     * @param maxBounces     número máximo de rebotes por proyectil
     * @param entityProvider proveedor de entidades candidatas (puede retornar lista vacía)
     */
    public BounceAmuletWrapper(BulletBehavior inner,
                               int maxBounces,
                               Supplier<List<? extends AbstractEntity>> entityProvider) {
        super(inner);
        this.maxBounces     = maxBounces;
        this.entityProvider = (entityProvider != null) ? entityProvider : List::of;
    }

    /**
     * Constructor sin proveedor — el rebote está desactivado.
     * Útil como placeholder cuando el entityProvider aún no está disponible.
     */
    public BounceAmuletWrapper(BulletBehavior inner, int maxBounces) {
        this(inner, maxBounces, null);
    }

    // ── Contrato de estado ────────────────────────────────────────────────

    /**
     * BounceAmuletWrapper es STATEFUL: bounceCount cambia por cada proyectil.
     * El pool no puede reutilizar instancias de este wrapper sin resetear su estado.
     */
    @Override
    public boolean isBehaviorStateless() {
        return false;
    }

    /**
     * Resetea el contador de rebotes al estado inicial.
     * Llamado por el pool antes de reutilizar la instancia.
     */
    @Override
    public void resetBehaviorState() {
        bounceCount = 0;
        inner.resetBehaviorState(); // propagar al inner
    }

    // ── Comportamiento ────────────────────────────────────────────────────

    @Override
    protected void onHitEntity(Bullet bullet, AbstractEntity hitEntity) {
        if (bounceCount >= maxBounces) return;

        // Buscar el objetivo más cercano diferente a la entidad actual
        AbstractEntity nextTarget = findNearestOther(
                bullet.getTransform().getPosition(), hitEntity);

        if (nextTarget == null) return; // sin objetivo — el proyectil muere normalmente

        bounceCount++;

        // Calcular la dirección hacia el nuevo objetivo
        Vector2D bulletPos = bullet.getTransform().getPosition();
        Vector2D targetPos = nextTarget.getTransform().getPosition();

        double dx   = targetPos.getX() - bulletPos.getX();
        double dy   = targetPos.getY() - bulletPos.getY();
        double dist = Math.hypot(dx, dy);

        if (dist < 1e-6) return;

        // Conservar la velocidad actual del proyectil (solo cambiar dirección)
        double currentSpeed = Math.hypot(
                bullet.getPhysics().getXspeed(),
                bullet.getPhysics().getYspeed()
        );
        if (currentSpeed < 1.0) currentSpeed = 8.0; // velocidad mínima de rebote

        bullet.getPhysics().setXspeed((dx / dist) * currentSpeed);
        bullet.getPhysics().setYspeed((dy / dist) * currentSpeed);

        // Reactivar el proyectil para que llegue al nuevo objetivo
        bullet.getBulletLife().revive();

        // Evento de rebote
        if (GameEventBus.GLOBAL.hasListeners(ProjectileEvents.OnProjectileBounce.class)) {
            GameEventBus.GLOBAL.post(new ProjectileEvents.OnProjectileBounce(bullet, hitEntity));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Busca la AbstractEntity más cercana al origen dentro del radio,
     * excluyendo la entidad que ya fue impactada.
     */
    private AbstractEntity findNearestOther(Vector2D origin, AbstractEntity exclude) {
        List<? extends AbstractEntity> candidates = entityProvider.get();
        if (candidates == null || candidates.isEmpty()) return null;

        AbstractEntity nearest  = null;
        double         minDist  = SEARCH_RADIUS * SEARCH_RADIUS; // comparar con distSq

        for (AbstractEntity candidate : candidates) {
            if (candidate == exclude) continue;
            if (candidate.isDead()) continue; // no redirigir a entidades muertas

            Vector2D pos = candidate.getTransform().getPosition();
            double dx    = pos.getX() - origin.getX();
            double dy    = pos.getY() - origin.getY();
            double distSq = dx * dx + dy * dy;

            if (distSq < minDist) {
                minDist = distSq;
                nearest = candidate;
            }
        }
        return nearest;
    }
}
