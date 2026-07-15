package Game.Gameplay.Core.Events;

import Game.Engine.GameObjects;

/**
 * Catálogo de eventos de gameplay del núcleo de Infinity Hell.
 *
 * ── PROPÓSITO ─────────────────────────────────────────────────────────────
 * Define los eventos fundamentales que representan las fases del ciclo de
 * vida del gameplay. Son los puntos de intercepción que los sistemas futuros
 * utilizarán para participar en la resolución sin acoplarse entre sí.
 *
 * ── DISEÑO: EVENTS COMO CLASES INTERNAS ESTÁTICAS ─────────────────────────
 * Los eventos concretos se definen como clases estáticas dentro de esta clase
 * contenedora. Esto agrupa el vocabulario sin crear un archivo por evento,
 * y hace que el namespace sea explícito: CoreGameplayEvents.OnDamage,
 * CoreGameplayEvents.OnDeath, etc.
 *
 * ── CAMPOS INMUTABLES vs MUTABLES ────────────────────────────────────────
 * Cada evento tiene:
 *   - Campos final (contexto de quién, cuándo, dónde): inmutables, descriptivos.
 *   - Campos con setter (cuánto, cómo): mutables, los interceptores los modifican.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Nuevos sistemas definen sus propios eventos en sus propios catálogos:
 *
 *   // En un módulo de magia:
 *   public final class SpellEvents {
 *       public static class OnSpellCast extends AbstractGameplayEvent { ... }
 *   }
 */
public final class CoreGameplayEvents {

    private CoreGameplayEvents() {}

    // ─────────────────────────────────────────────────────────────────────
    // SPAWN — Una entidad aparece en el mundo
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Evento disparado cuando una entidad es creada y añadida al mundo.
     *
     * Casos de uso para interceptores:
     *   - Aplicar buffs de inicio de partida.
     *   - Registrar en minimapa / sistemas de tracking.
     *   - Reproducir efectos de aparición.
     *   - Cancelar: impedir el spawn bajo ciertas condiciones.
     */
    public static class OnSpawn extends AbstractGameplayEvent {
        private final GameObjects entity;

        public OnSpawn(GameObjects entity) {
            this.entity = entity;
        }

        /** La entidad que está apareciendo. */
        public GameObjects getEntity() { return entity; }
    }

    // ─────────────────────────────────────────────────────────────────────
    // DAMAGE — Una entidad va a recibir daño
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Evento disparado justo ANTES de aplicar daño a una entidad.
     *
     * Los interceptores pueden:
     *   - Reducir damage (resistencias, armadura, escudo).
     *   - Amplificar damage (vulnerabilidad, debuff).
     *   - Cancelar el evento (inmunidad total, invulnerabilidad).
     *
     * Tras fire(), si !isCancelled(), aplicar getDamage() real a la entidad.
     */
    public static class OnDamage extends AbstractGameplayEvent {
        private final GameObjects source;
        private final GameObjects target;
        private double damage;

        public OnDamage(GameObjects source, GameObjects target, double damage) {
            this.source = source;
            this.target = target;
            this.damage = damage;
        }

        /** Quién inflige el daño (puede ser null para daño del entorno). */
        public GameObjects getSource() { return source; }

        /** Quién recibe el daño. */
        public GameObjects getTarget() { return target; }

        /** Daño actual (modificable por interceptores). */
        public double getDamage()          { return damage; }
        public void   setDamage(double v)  { this.damage = Math.max(0.0, v); }
    }

    // ─────────────────────────────────────────────────────────────────────
    // DEATH — Una entidad ha llegado a 0 HP
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Evento disparado cuando una entidad muere (HP ≤ 0).
     *
     * Los interceptores pueden:
     *   - Cancelar: impedir la muerte (ej: buff "sobrevivir con 1 HP").
     *   - Observar: sistemas de loot, puntuación, misiones.
     */
    public static class OnDeath extends AbstractGameplayEvent {
        private final GameObjects entity;
        private final GameObjects killer;  // puede ser null (muerte por entorno)

        public OnDeath(GameObjects entity, GameObjects killer) {
            this.entity = entity;
            this.killer = killer;
        }

        /** La entidad que muere. */
        public GameObjects getEntity() { return entity; }

        /** La entidad responsable de la muerte, o null si fue por entorno. */
        public GameObjects getKiller() { return killer; }
    }

    // ─────────────────────────────────────────────────────────────────────
    // MOVE — Una entidad se mueve
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Evento disparado antes de que una entidad se desplace.
     * Permite a sistemas externos alterar la velocidad o dirección del movimiento.
     *
     * Los interceptores pueden:
     *   - Modificar speedX / speedY (efectos de ralentización, empuje).
     *   - Cancelar: paralizar completamente el movimiento.
     */
    public static class OnMove extends AbstractGameplayEvent {
        private final GameObjects entity;
        private double speedX;
        private double speedY;

        public OnMove(GameObjects entity, double speedX, double speedY) {
            this.entity = entity;
            this.speedX = speedX;
            this.speedY = speedY;
        }

        public GameObjects getEntity() { return entity; }

        public double getSpeedX()            { return speedX; }
        public void   setSpeedX(double v)    { this.speedX = v; }

        public double getSpeedY()            { return speedY; }
        public void   setSpeedY(double v)    { this.speedY = v; }
    }

    // ─────────────────────────────────────────────────────────────────────
    // COLLISION — Dos entidades colisionan
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Evento disparado cuando dos entidades se detectan en contacto.
     *
     * Los interceptores pueden:
     *   - Cancelar: impedir que los efectos de la colisión se ejecuten.
     *   - Observar: registrar impactos, reproducir efectos de sonido.
     */
    public static class OnCollision extends AbstractGameplayEvent {
        private final GameObjects entityA;
        private final GameObjects entityB;

        public OnCollision(GameObjects entityA, GameObjects entityB) {
            this.entityA = entityA;
            this.entityB = entityB;
        }

        public GameObjects getEntityA() { return entityA; }
        public GameObjects getEntityB() { return entityB; }
    }

    // ─────────────────────────────────────────────────────────────────────
    // BOUNCE — Un proyectil o entidad rebota
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Evento disparado cuando una entidad con CAN_BOUNCE rebota.
     *
     * Los interceptores pueden:
     *   - Modificar el número de rebotes restantes.
     *   - Añadir efectos al rebotar (daño de área en punto de impacto).
     *   - Cancelar: consumir el rebote sin efecto.
     */
    public static class OnBounce extends AbstractGameplayEvent {
        private final GameObjects entity;
        private int remainingBounces;

        public OnBounce(GameObjects entity, int remainingBounces) {
            this.entity           = entity;
            this.remainingBounces = remainingBounces;
        }

        public GameObjects getEntity()                    { return entity; }
        public int  getRemainingBounces()                 { return remainingBounces; }
        public void setRemainingBounces(int v)            { this.remainingBounces = v; }
    }

    // ─────────────────────────────────────────────────────────────────────
    // EXPIRE — Una entidad con vida limitada llega a su fin natural
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Evento disparado cuando la vida útil de una entidad (ej: un proyectil,
     * un efecto temporal) llega a cero por tiempo, no por daño.
     *
     * Los interceptores pueden:
     *   - Cancelar: extender la vida (buff de duración).
     *   - Observar: disparar efectos al expirar (explotar al expirar).
     */
    public static class OnExpire extends AbstractGameplayEvent {
        private final GameObjects entity;

        public OnExpire(GameObjects entity) {
            this.entity = entity;
        }

        public GameObjects getEntity() { return entity; }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SUMMON — Una entidad invoca a otra
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Evento disparado cuando una entidad es invocada por otra.
     *
     * Los interceptores pueden:
     *   - Modificar los parámetros de la invocada (stats, posición).
     *   - Cancelar: bloquear la invocación.
     */
    public static class OnSummon extends AbstractGameplayEvent {
        private final GameObjects summoner;
        private final GameObjects summoned;

        public OnSummon(GameObjects summoner, GameObjects summoned) {
            this.summoner = summoner;
            this.summoned = summoned;
        }

        public GameObjects getSummoner() { return summoner; }
        public GameObjects getSummoned() { return summoned; }
    }
}
