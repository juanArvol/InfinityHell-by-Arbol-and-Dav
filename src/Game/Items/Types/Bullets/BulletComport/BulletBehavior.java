package Game.Items.Types.Bullets.BulletComport;

import Game.Engine.GameObjects;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.Definition.ProjectileContext;
import Game.Items.Types.Bullets.Definition.ProjectileData;
import Game.Items.Types.Bullets.Movement.LinearMovement;
import Game.Items.Types.Bullets.ProjectileMovement;

/**
 * Comportamiento de un proyectil — qué hace al impactar, cada frame y al expirar.
 *
 * ── HRFC — Projectile Construction & Transformation Pipeline ─────────────
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 *   BulletBehavior     → QUÉ HACE al impactar, en cada frame y al expirar.
 *   ProjectileData     → QUÉ VALORES tiene el proyectil (datos declarativos).
 *   ProjectileMovement → CÓMO SE MUEVE cada frame (estrategia de movimiento).
 *   ProjectileBlueprint→ DEFINICIÓN FINAL resuelta antes de instanciar Bullet.
 *
 * ── CICLO DE VIDA COMPLETO DEL BEHAVIOR ──────────────────────────────────
 *
 *   onAttached(bullet)           — bullet adquirido del pool o recién creado.
 *                                  Adquirir recursos, registrar listeners,
 *                                  capturar posición de spawn.
 *
 *   onUpdate(bullet)             — cada frame mientras el bullet está vivo.
 *
 *   onCollision(bullet, other)   — al impactar un objeto del mundo.
 *
 *   onExpire(bullet, ctx)        — al agotar lifeTime SIN impactar nada.
 *                                  spawnear fragmentos, efectos de área, etc.
 *
 *   onRelease(bullet)            — SIEMPRE, justo antes de que el bullet sea
 *                                  reciclado al pool o destruido definitivamente.
 *                                  Liberar recursos: cancelar listeners,
 *                                  limpiar colecciones, liberar referencias.
 *                                  GARANTÍA: se invoca exactamente una vez por
 *                                  ciclo de vida, independientemente de la causa
 *                                  de muerte (expiración, colisión, kill()).
 *
 *   onDetached(bullet)           — SOLO cuando el behavior es REEMPLAZADO en
 *                                  un bullet VIVO (via ProjectileView.changeBehavior).
 *                                  NO se llama en muerte normal. NO se llama antes
 *                                  del pool release — eso es responsabilidad de
 *                                  onRelease().
 *
 * ── SECUENCIA PARA BULLETS CON POOL ──────────────────────────────────────
 *
 *   acquire() → onAttached → onUpdate* → onExpire/onCollision → onRelease
 *             → [pool.release()] → [pool.acquire()] → onAttached → ...
 *
 * ── SECUENCIA PARA BULLETS SIN POOL ──────────────────────────────────────
 *
 *   new Bullet() → onAttached → onUpdate* → onExpire/onCollision → onRelease
 *               → [GC]
 *
 * ── COMPORTAMIENTO DE onRelease vs onDetached ─────────────────────────────
 *
 *   onRelease  → cleanup final. Liberar todo. Siempre garantizado.
 *   onDetached → notificación de reemplazo en vida. Para transición de control.
 *
 *   Un behavior que registra un listener en onAttached DEBE cancelarlo en
 *   onRelease. Si usa onDetached para eso, falla en muerte normal (sin reemplazo).
 *
 * ── STATELESS vs STATEFUL ────────────────────────────────────────────────
 *
 *   isBehaviorStateless() == true  → el pool puede reutilizar sin resetear.
 *   isBehaviorStateless() == false → el pool llama resetBehaviorState() antes.
 *
 *   resetBehaviorState() debe dejar el behavior en el mismo estado que una
 *   instancia recién creada — no debe necesitar onRelease() para eso.
 *   onRelease() es para limpieza de recursos EXTERNOS (listeners, refs).
 *   resetBehaviorState() es para limpieza de estado INTERNO (contadores).
 */
public abstract class BulletBehavior {

    // ── Datos y movimiento por defecto ────────────────────────────────────

    /**
     * Datos de configuración por defecto de este tipo de proyectil.
     *
     * ProjectileBlueprint.from() lee estos datos para construir la definición
     * inicial antes de aplicar modifiers. BulletFactory ya NO lee estos datos
     * directamente — los recibe resueltos en el Blueprint.
     *
     * Default base: 10 daño, x1 speed, 10 ticks, sin gravedad, 8×8px.
     * Sobreescribir con los valores reales del behavior.
     */
    public ProjectileData getDefaultData() {
        return ProjectileData.flat(10, 1.0, 10);
    }

    /**
     * Estrategia de movimiento por defecto de este tipo de proyectil.
     *
     * Default: LinearMovement.INSTANCE (velocidad constante, sin efectos).
     *
     * Sobreescribir para tipos con movimiento intrínseco (gravedad, homing, orbital…).
     */
    public ProjectileMovement getDefaultMovement() {
        return LinearMovement.INSTANCE;
    }

    /**
     * Estado físico declarado por este tipo de proyectil.
     *
     * ── Mini-HRFC — Declarative PhysicalState Ownership ───────────────────
     *
     * Cada tipo de proyectil declara explícitamente su PhysicalState.
     * Si retorna null, el proyectil NO recibe PhysicsComponent y no participa
     * en dominios físicos (thermal, electrical, fluid, mechanical).
     *
     * Default: null (sin física).
     *
     * Sobreescribir para proyectiles con propiedades físicas:
     *
     * <pre>{@code
     * @Override
     * public PhysicalState getPhysicalState() {
     *     return PhysicalState.builder()
     *         .register(ThermalProperties.TEMPERATURE, 300.0)  // FireBullet
     *         .registerMaterial(METAL_MATERIAL::registerInto)
     *         .build();
     * }
     * }</pre>
     *
     * @return PhysicalState declarado o null si no tiene física
     */
    public Game.Engine.Physics.Core.PhysicalState getPhysicalState() {
        return null;  // Default: sin física
    }

    /**
     * Radio de interacción espacial de este tipo de proyectil.
     *
     * ── POR QUÉ ESTÁ AQUÍ ────────────────────────────────────────────────
     *
     * El radio de detección es una decisión del TYPE de proyectil, no de la
     * geometría del sprite. BulletBehavior define el tipo; Bullet es la entidad.
     * Colocarlo aquí permite que cada tipo concreto declare su propio radio
     * sin tocar Bullet, siguiendo el mismo patrón que getDefaultData() y
     * getDefaultMovement().
     *
     * ── DEFAULT ──────────────────────────────────────────────────────────
     *
     * El default conserva el comportamiento original de Bullet: la dimensión
     * mayor del collider dividida por 2. Cualquier tipo que no sobreescriba
     * este método mantiene exactamente el mismo radio que antes.
     *
     * ── SOBREESCRITURA ───────────────────────────────────────────────────
     *
     * Sobreescribir para tipos que requieran un radio diferente:
     *
     *   {@code @Override}
     *   {@code public double getInteractionRadius(Bullet bullet) { return 15; }}
     *
     * El parámetro bullet está disponible por si el radio depende de estado
     * individual (p.ej. un multiplicador de daño). Para radios fijos por tipo,
     * ignorarlo y retornar la constante directamente.
     *
     * @param bullet el proyectil que consulta su propio radio
     * @return radio de interacción en unidades del mundo (px)
     */
    public double getInteractionRadius(Bullet bullet) {
        return Math.max(bullet.getFlyweight().width(), bullet.getFlyweight().height()) / 2.0;
    }

    // ── Ciclo de vida del behavior ────────────────────────────────────────

    /**
     * Llamado cuando este behavior es asignado a un proyectil.
     *
     * Se invoca en dos situaciones:
     *   1. Al construir el Bullet (el behavior inicial recibe onAttached).
     *   2. Al adquirir del pool (pool.acquire() llama resetState que llama onAttached).
     *   3. Cuando ProjectileView.changeBehavior() reemplaza el behavior en vida.
     *
     * Usar para: capturar posición de spawn, registrar listeners, adquirir refs.
     *
     * GARANTÍA: el proyectil ya tiene posición, velocidad y collider configurados.
     * onDetached() del behavior anterior ya fue llamado (en caso de reemplazo).
     *
     * Default: sin efecto.
     */
    public void onAttached(Bullet bullet) {}

    /**
     * Llamado cuando este behavior es REEMPLAZADO en un proyectil VIVO.
     *
     * Se invoca SOLO cuando ProjectileView.changeBehavior() reemplaza este
     * behavior mientras el proyectil sigue activo. NO se invoca en muerte normal.
     *
     * Para liberación de recursos en muerte normal (expiración, colisión, kill()),
     * usar {@link #onRelease(Bullet)} que siempre está garantizado.
     *
     * Default: sin efecto.
     */
    public void onDetached(Bullet bullet) {}

    /**
     * Llamado justo antes de que el proyectil sea reciclado al pool o destruido.
     *
     * ── GARANTÍA DE CLEANUP ───────────────────────────────────────────────
     *
     * onRelease() se invoca EXACTAMENTE UNA VEZ por ciclo de vida, independientemente
     * de cómo murió el proyectil:
     *   - expiración por tiempo (lifeTime agotado)
     *   - muerte por colisión (kill() desde onCollision)
     *   - muerte manual (kill() desde exterior)
     *   - pool release (devuelta al pool via ownerPool.release())
     *
     * Es el único hook con esa garantía. No depender de onExpire ni onDetached
     * para liberación de recursos — esos no están garantizados en todos los paths.
     *
     * USO CORRECTO:
     *   - Cancelar Subscriptions registradas en onAttached.
     *   - Limpiar Sets de referencias (hitEntities, etc.).
     *   - Liberar referencias a entidades externas.
     *   - Cualquier limpieza que DEBE ocurrir al final del lifecycle.
     *
     * NO usar para: lógica de juego (eso es onExpire/onCollision).
     *
     * Default: sin efecto. Sobreescribir cuando se necesite cleanup.
     *
     * @param bullet el proyectil que está siendo liberado
     */
    public void onRelease(Bullet bullet) {}

    // ── Comportamiento de frame ───────────────────────────────────────────

    /**
     * Lógica de actualización por frame del proyectil.
     *
     * Llamado desde Bullet.update() cada frame que el proyectil está vivo.
     * Default: sin efecto.
     */
    public void onUpdate(Bullet bullet) {}

    // ── Comportamiento de colisión ────────────────────────────────────────

    /**
     * Reacción al contacto con cualquier objeto del mundo.
     *
     * @param bullet el proyectil que colisionó
     * @param other  el objeto con el que colisionó
     */
    public void onCollision(Bullet bullet, GameObjects other) {}

    // ── Comportamiento de expiración ──────────────────────────────────────

    /**
     * Reacción cuando el proyectil agota su tiempo de vida sin impactar nada.
     *
     * Se llama ANTES del evento OnProjectileExpire del bus.
     * El behavior puede spawnear proyectiles secundarios via ProjectileContext.
     *
     * Diferencia con onCollision:
     *   onCollision → el proyectil impacta un objeto.
     *   onExpire    → el proyectil agota su lifeTime sin impactar.
     *
     * NOTA: onRelease() se llamará DESPUÉS de onExpire(). No duplicar cleanup.
     *
     * Default: sin efecto.
     *
     * @param bullet el proyectil que expiró
     * @param ctx    contexto de interacción con el mundo
     */
    public void onExpire(Bullet bullet, ProjectileContext ctx) {}

    // ── Contrato de estado para pool ──────────────────────────────────────

    /**
     * Indica si este behavior NO tiene estado interno mutable.
     *
     * true  → el pool puede reutilizar esta instancia sin resetear su estado.
     * false → el pool llama resetBehaviorState() antes de reutilizar.
     *
     * IMPORTANTE: isBehaviorStateless() aplica al ESTADO INTERNO del behavior
     * (contadores, flags, colecciones). Los recursos EXTERNOS (listeners
     * registrados, referencias capturadas) siempre deben limpiarse en onRelease(),
     * independientemente de si el behavior es stateless.
     *
     * Default: true.
     */
    public boolean isBehaviorStateless() {
        return true;
    }

    /**
     * Resetea el estado interno del behavior para reutilización por el pool.
     *
     * Solo se llama si isBehaviorStateless() == false.
     * resetBehaviorState() resetea contadores y estado interno.
     * onRelease() libera recursos externos.
     * Son responsabilidades distintas — no mezclarlas.
     *
     * Default: no-op.
     */
    public void resetBehaviorState() {}
}
