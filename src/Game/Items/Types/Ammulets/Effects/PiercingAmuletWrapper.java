package Game.Items.Types.Ammulets.Effects;

import Game.Engine.AbstractEntity;
import Game.Engine.Events.GameEventBus;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.ProjectileEvents;
import Game.Items.Types.Weapons.Modifiers.BulletBehaviorWrapper;
import java.util.Collections;
import java.util.Set;
import java.util.IdentityHashMap;

/**
 * Wrapper de perforación — "Esquirla de Fase".
 *
 * Cada copia del amuleto añade +1 perforación. Con 3 amuletos la bala
 * perfora 3 entidades adicionales (wrappers anidados).
 *
 * ── HRFC — Projectile System Refactor ────────────────────────────────────
 *
 * PROBLEMA ANTERIOR:
 *   Usaba System.identityHashCode(entity) para deduplicar impactos.
 *   identityHashCode() puede tener colisiones (dos objetos distintos con
 *   el mismo hashCode). La probabilidad es baja por entidad individual,
 *   pero en un bullet-hell con miles de entidades simultáneas puede ocurrir.
 *
 * SOLUCIÓN:
 *   Usar un Set basado en IdentityHashMap — compara referencias por identidad
 *   (==), no por hashCode(). Esto garantiza unicidad real sin colisiones.
 *
 *   Collections.newSetFromMap(new IdentityHashMap<>()) produce un Set
 *   con semántica de identidad exacta, sin coste de boxing ni colisiones.
 */
public class PiercingAmuletWrapper extends BulletBehaviorWrapper {

    private final int maxPierces;
    private int pierceCount = 0;

    /**
     * Set de entidades ya impactadas, comparadas por identidad de referencia (==).
     * Evita multi-hit y colisiones de hashCode.
     */
    private final Set<AbstractEntity> hitEntities =
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
}
