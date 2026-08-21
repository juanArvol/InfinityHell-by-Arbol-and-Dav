package Game.Engine;

import Game.Engine.Entity.Components.HealthComponent;
import Game.Engine.Entity.Components.StatusEffectComponent;
import Game.Engine.GameMath.Logic2D.Transform2D;

/**
 * Capa de gameplay compartido entre todas las entidades interactuables.
 *
 * ── JERARQUÍA COMPLETA ────────────────────────────────────────────────────
 *
 *   GameObjects       ← infraestructura del engine: componentes, transform, colisiones
 *       ↑
 *   Entity            ← gameplay compartido: vida, efectos, shortcuts de entidad
 *       ↑
 *   MovingObjects     ← movimiento y física
 *       ↑
 *   Player / Enemy / Bullet / etc.
 *
 * ── POR QUÉ ENTITY EXISTE (y no es una clase vacía) ─────────────────────
 *
 * PROBLEMA QUE RESUELVE:
 *   Sin Entity, los conceptos de gameplay compartido terminaban dispersos:
 *   - HealthComponent lo añadía cada subclase por separado, sin contrato.
 *   - PlayerStats duplicaba la lógica de vida de HealthComponent.
 *   - No había un punto común para consultar si "algo tiene vida" sin
 *     saber si es Player o Enemy.
 *   - Los shortcuts de acceso (damage, isDead, addEffect) se duplicaban
 *     entre Player y Enemy, o no existían en uno de los dos.
 *
 * RESPONSABILIDAD DE ENTITY:
 *   Entity representa "cualquier cosa que existe en el mundo del juego
 *   como entidad interactuable". Aporta:
 *
 *   1. CONTRATO DE CICLO DE VIDA DE GAMEPLAY
 *      initEntityComponents() — punto único donde las subclases registran
 *      sus componentes de gameplay (HealthComponent, StatusEffectComponent).
 *      Esto centraliza la inicialización sin forzar configuraciones rígidas.
 *
 *   2. SHORTCUTS DE GAMEPLAY (acceso idiomático)
 *      damage(), heal(), isDead(), addEffect(), hasEffect()...
 *      Evitan que el código cliente tenga que conocer getComponent() para
 *      operaciones de gameplay de uso frecuente. Son wrappers delegantes,
 *      no lógica duplicada.
 *
 *   3. ACCESO TIPADO A COMPONENTES DE GAMEPLAY
 *      getHealth(), getStatusEffects() — acceso sin cast, sin conocer el
 *      nombre del componente.
 *
 * ── QUÉ NO HACE ENTITY ───────────────────────────────────────────────────
 *
 * - NO reemplaza GameObjects (el sistema de componentes sigue en GameObjects).
 * - NO reemplaza MovingObjects (física y movimiento siguen ahí).
 * - NO fuerza una configuración de HealthComponent — las subclases deciden
 *   el maxHp y si quieren vida o no (objetos indestructibles pueden omitirlo).
 * - NO contiene lógica de combate, IA ni movimiento.
 *
 * ── CONTRATOS DE SUBCLASE ────────────────────────────────────────────────
 *
 * Las subclases que quieren salud configuran un EntityStats y lo pasan
 * al constructor enlazado de HealthComponent:
 *
 *   EntityStats es = new EntityStats();
 *   es.setMaxHp(100);
 *   addComponent(new HealthComponent(es));
 *
 * Para objetos simples sin EntityStats (cajas destructibles, trampas):
 *   addComponent(new HealthComponent(maxHp));  // modo standalone
 *
 * Opcionalmente, para efectos de estado:
 *   addComponent(new StatusEffectComponent());
 *
 * Luego los shortcuts de Entity funcionan automáticamente.
 * Si HealthComponent no fue añadido, damage()/isDead() retornan sin efecto
 * (null-safe por diseño).
 *
 * ── CANDIDATOS FUTUROS ────────────────────────────────────────────────────
 *
 * - FactionComponent  → getFaction(), isAllyOf(Entity other)
 * - BuffComponent     → addBuff(), getActiveBuffs()
 * - TagComponent      → hasTag("boss"), hasTag("destructible")
 */
public abstract class AbstractEntity extends GameObjects {

    /**
     * Constructor con transform inyectable.
     * Propaga hacia GameObjects para soporte de Transform3D en la jerarquía.
     * Solo deben usarlo subclases que necesiten un Transform3D (ej. objetos 2.5D).
     */
    protected AbstractEntity(Transform2D transform) {
        super(transform);
    }

    /** Constructor por defecto — delega a GameObjects(), que usa Transform2D. */
    protected AbstractEntity() {
        super();
    }

    // ── Acceso tipado a componentes de gameplay ────────────────────────────

    /**
     * Acceso tipado al HealthComponent sin cast ni magic class.
     * Retorna null si esta entidad no tiene vida (objetos indestructibles).
     */
    public HealthComponent getHealth() {
        return getComponent(HealthComponent.class);
    }

    /**
     * Acceso tipado al StatusEffectComponent.
     * Retorna null si esta entidad no puede recibir efectos.
     */
    public StatusEffectComponent getStatusEffects() {
        return getComponent(StatusEffectComponent.class);
    }

    // ── Shortcuts de gameplay ─────────────────────────────────────────────
    //
    // Son wrappers idiomáticos sobre getComponent(). No duplican lógica —
    // delegan al componente. El valor es que el código cliente escribe
    //   enemy.damage(10)
    // en lugar de
    //   enemy.getComponent(HealthComponent.class).damage(10)
    // y no necesita saber qué componente gestiona la vida.

    /**
     * Aplica daño a esta entidad.
     * No tiene efecto si la entidad no tiene HealthComponent.
     */
    public void gotDamage(int amount) {
        HealthComponent hp = getHealth();
        if (hp != null) hp.damage(amount);
    }

    /**
     * Cura a esta entidad.
     * No tiene efecto si la entidad no tiene HealthComponent.
     */
    public void gotHeal(int amount) {
        HealthComponent hp = getHealth();
        if (hp != null) hp.heal(amount);
    }

    /**
     * Retorna true si esta entidad está muerta (HP <= 0).
     * Retorna false si no tiene HealthComponent (indestructible).
     */
    public boolean isDead() {
        HealthComponent hp = getHealth();
        return hp != null && hp.isDead();
    }

    /**
     * Retorna el porcentaje de vida en [0.0, 1.0].
     * Retorna 1.0 si no tiene HealthComponent (siempre "lleno").
     */
    public double getHealthPercent() {
        HealthComponent hp = getHealth();
        return hp != null ? hp.getHealthPercent() : 1.0;
    }

    /**
     * Añade un efecto de estado a esta entidad.
     * No tiene efecto si la entidad no tiene StatusEffectComponent.
     */
    public void addEffect(StatusEffectComponent.StatusEffect effect) {
        StatusEffectComponent fx = getStatusEffects();
        if (fx != null) fx.add(effect);
    }

    /**
     * Retorna true si hay un efecto activo del tipo dado.
     * Consulta completamente tipada — sin Strings como clave lógica.
     *
     * @param type clase del tipo de efecto.
     */
    public <T extends StatusEffectComponent.StatusEffect> boolean hasEffect(Class<T> type) {
        StatusEffectComponent fx = getStatusEffects();
        return fx != null && fx.hasEffect(type);
    }

    /**
     * Elimina todos los efectos activos del tipo dado.
     * Llama onExpire() en cada efecto eliminado.
     *
     * @param type clase del tipo de efecto.
     */
    public <T extends StatusEffectComponent.StatusEffect> void removeEffects(Class<T> type) {
        StatusEffectComponent fx = getStatusEffects();
        if (fx != null) fx.removeAll(type);
    }
}
