package Game.Items.Types.Ammulets.Effects;

import Game.Engine.AbstractEntity;
import Game.Engine.Events.GameEventBus;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.BulletComport.BulletBehaviorWrapper;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.Definition.ProjectileEvents;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Wrapper de perforación — "Esquirla de Fase".
 *
 * Cada copia del amuleto añade +1 perforación. Con 3 amuletos la bala
 * perfora 3 entidades adicionales (wrappers anidados).
 *
 * ── HRFC — Projectile System Refactor ────────────────────────────────────
 *
 * PROBLEMA ANTERIOR (identityHashCode):
 *   Usaba System.identityHashCode(entity) para deduplicar impactos.
 *   identityHashCode() puede tener colisiones (dos objetos distintos con
 *   el mismo hashCode). La probabilidad es baja por entidad individual,
 *   pero en un bullet-hell con miles de entidades simultáneas puede ocurrir.
 *
 * SOLUCIÓN (IdentityHashMap):
 *   Usar un Set basado en IdentityHashMap — compara referencias por identidad
 *   (==), no por hashCode(). Esto garantiza unicidad real sin colisiones.
 *
 * ── HRFC — Lifecycle de hitEntities (Issue 10) ──────────────────────────
 *
 * PROBLEMA ANTERIOR (retención de referencias):
 *   hitEntities retenía referencias a AbstractEntity hasta que el GC
 *   colectara el wrapper. Si el proyectil moría (por colisión normal o
 *   expiración), el Set permanecía poblado sin limpiarse explícitamente.
 *
 *   resetBehaviorState() limpiaba el Set, pero nunca se invocaba en la
 *   práctica: el pool no puede reutilizar PiercingAmuletWrapper porque
 *   isBehaviorStateless() == false, así que resetBehaviorState() era
 *   código muerto para este wrapper.
 *
 * SOLUCIÓN (onWrapperRelease):
 *   onWrapperRelease() se invoca SIEMPRE al final del lifecycle del
 *   proyectil, independientemente de la causa de muerte. Es el único
 *   hook con esa garantía (ver BulletBehavior.onRelease() Javadoc).
 *
 *   Allí se limpia hitEntities, liberando las referencias a entidades
 *   antes de que el wrapper sea elegible para GC o reutilización.
 *
 *   resetBehaviorState() se mantiene como mecanismo de reset del estado
 *   interno (por si en el futuro el pool cambia su política de reutilización
 *   para behaviors stateful), pero ya no es el único punto de cleanup.
 *
 * ── SEMÁNTICA CORRECTA DE hitEntities ────────────────────────────────────
 *
 *   hitEntities DEBE estar poblado mientras el proyectil está vivo:
 *   sin él, el mismo enemigo podría ser impactado varias veces en el
 *   mismo frame (si onCollision se llama múltiples veces por overlapping).
 *
 *   hitEntities DEBE estar vacío cuando el proyectil muere: no hay
 *   razón para retener esas referencias después del lifecycle.
 *
 *   onWrapperRelease() es el punto donde se cumple la segunda condición.
 */
public class PiercingAmuletWrapper extends BulletBehaviorWrapper {

    private final int maxPierces;
    private int pierceCount = 0;

    /**
     * Set de entidades ya impactadas, comparadas por identidad de referencia (==).
     * Evita multi-hit y colisiones de hashCode.
     *
     * Se limpia en onWrapperRelease() al final del lifecycle del proyectil.
     */
    private Set<AbstractEntity> hitEntities =
            Collections.newSetFromMap(new IdentityHashMap<>());

    public PiercingAmuletWrapper(BulletBehavior inner, int maxPierces) {
        super(inner);
        this.maxPierces = maxPierces;
    }

    @Override
    protected void onHitEntity(Bullet bullet, AbstractEntity entity) {
        // hitEntities usa identidad de referencia — sin colisiones de hashCode
        if (!hitEntities.add(entity)) return; // ya impactó a esta entidad

        pierceCount++;

        if (pierceCount <= maxPierces) {
            // El proyectil perfora — sigue vivo
            bullet.getBulletLife().revive();

            if (GameEventBus.GLOBAL.hasListeners(ProjectileEvents.OnProjectilePierce.class)) {
                GameEventBus.GLOBAL.post(new ProjectileEvents.OnProjectilePierce(bullet, entity));
            }
        }
        // Si superó maxPierces, el inner ya mató el proyectil con kill()
    }

    // ── Lifecycle garantizado ─────────────────────────────────────────────

    /**
     * Libera las referencias a entidades impactadas al final del lifecycle.
     *
     * Se invoca SIEMPRE, independientemente de la causa de muerte del proyectil
     * (expiración, colisión, kill() manual). Garantizado por emitDestroy()
     * en Bullet, protegido por el flag destroyEventFired.
     *
     * Limpia hitEntities y pierceCount. Después de este método, el wrapper
     * no retiene ninguna referencia a entidades del mundo.
     */
    @Override
    protected void onWrapperRelease(Bullet bullet) {
        hitEntities.clear();
        pierceCount = 0;
    }

    // ── Contrato de estado para pool ──────────────────────────────────────

    /**
     * PiercingAmuletWrapper es STATEFUL: pierceCount y hitEntities cambian
     * por cada proyectil. El pool no puede reutilizar instancias sin resetear.
     *
     * Nota: dado que isBehaviorStateless() == false, el pool actualmente
     * no reutiliza instancias de este wrapper — cada proyectil crea una nueva.
     * El cleanup de onWrapperRelease() es correcto en ambos casos:
     *   - Sin pool: libera referencias antes de que el wrapper sea GC'd.
     *   - Con pool (si en el futuro cambia la política): prepara para reutilización.
     */
    @Override
    public boolean isBehaviorStateless() {
        return false;
    }

    /**
     * Resetea el estado interno del wrapper.
     *
     * Este método existe para compatibilidad con el protocolo de pool
     * (BulletBehavior.resetBehaviorState()). En la práctica, onWrapperRelease()
     * ya limpió hitEntities y pierceCount antes de llegar aquí.
     * No causa daño llamarlo de nuevo — limpiar un Set ya vacío y resetear 0 es no-op.
     */
    @Override
    public void resetBehaviorState() {
        pierceCount = 0;
        hitEntities = Collections.newSetFromMap(new IdentityHashMap<>());
        inner.resetBehaviorState();
    }
}
