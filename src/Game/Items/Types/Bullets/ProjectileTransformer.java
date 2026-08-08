package Game.Items.Types.Bullets;

import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.Definition.Bullet;

/**
 * Transformación de un proyectil YA INSTANCIADO (post-build, runtime).
 *
 * ── HRFC — Projectile Construction & Transformation Pipeline ─────────────
 *
 * ProjectileTransformer opera sobre instancias Bullet vivas.
 * Es la abstracción que permite a sistemas externos (Parry, Freeze,
 * ConversionSystem, etc.) modificar un proyectil sin que BulletFactory
 * ni ninguna otra infraestructura lo conozca.
 *
 * ── SEPARACIÓN vs ProjectileModifier ─────────────────────────────────────
 *
 *   ProjectileModifier   = transforma la definición (pre-build, sin Bullet).
 *   ProjectileTransformer= transforma la instancia (post-build, Bullet vivo).
 *
 * ── API CONTROLADA — ProjectileView ──────────────────────────────────────
 *
 * El transformer recibe un ProjectileView, no el Bullet directamente.
 * ProjectileView expone únicamente las operaciones que son semánticamente
 * válidas en una transformación de proyectil. Esto evita que los sistemas
 * externos accedan a APIs internas de Bullet de forma arbitraria.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   // Sistema de Parry:
 *   ProjectileTransformer parry = view -> {
 *       view.redirect(-view.getXSpeed(), view.getYSpeed()); // invertir X
 *       view.changeCollisionProfile(CollisionProfile.PLAYER_BULLET);
 *   };
 *
 *   // Sistema de Freeze:
 *   ProjectileTransformer freeze = view -> {
 *       view.redirect(0, 0);  // detener
 *       // El sistema de Freeze gestiona el estado temporal externamente
 *   };
 *
 *   // Sistema de conversión (proyectil enemigo → aliado):
 *   ProjectileTransformer convert = view -> {
 *       view.changeCollisionProfile(CollisionProfile.PLAYER_BULLET);
 *       view.scaleDamage(1.5); // bonus de daño al convertir
 *   };
 *
 *   // Aplicar al bullet:
 *   parry.apply(bullet.getView());  // desde BulletTransformAPI
 *
 * ── @FunctionalInterface ─────────────────────────────────────────────────
 *
 * Funcional para permitir lambdas y method references.
 *
 * ── COMPOSICIÓN ──────────────────────────────────────────────────────────
 *
 *   ProjectileTransformer combined = parry.andThen(damageBoost);
 */
@FunctionalInterface
public interface ProjectileTransformer {

    /**
     * Aplica la transformación sobre el proyectil representado por la view.
     *
     * @param view API controlada del proyectil a transformar
     */
    void apply(ProjectileView view);

    // ── Composición ───────────────────────────────────────────────────────

    /**
     * Compone este transformer con otro, aplicándolos en secuencia.
     */
    default ProjectileTransformer andThen(ProjectileTransformer after) {
        return view -> {
            this.apply(view);
            after.apply(view);
        };
    }

    /** Transformer que no hace nada — identidad. */
    ProjectileTransformer IDENTITY = view -> {};

    // ── API de transformación ─────────────────────────────────────────────

    /**
     * Vista controlada de un proyectil para uso en transformaciones runtime.
     *
     * Expone únicamente las operaciones semánticamente válidas en una
     * transformación de proyectil. No expone la instancia Bullet directamente.
     * No expone APIs de ciclo de vida ni de construcción.
     *
     * ── OPERACIONES DISPONIBLES ──────────────────────────────────────────
     *
     *   redirect(xSpeed, ySpeed)        — cambia dirección/velocidad.
     *   changeCollisionProfile(profile) — cambia con qué capas colisiona.
     *   changeDamage(damage)            — establece nuevo valor de daño.
     *   scaleDamage(factor)             — multiplica el daño por un factor.
     *   changeBehavior(behavior)        — reemplaza el behavior (conversión).
     *   changeMovement(movement)        — reemplaza el movement (freeze, etc.).
     *
     * ── ACCESO DE LECTURA ────────────────────────────────────────────────
     *
     *   getXSpeed() / getYSpeed()       — velocidad actual.
     *   getDamage()                     — daño actual.
     *   getBehavior()                   — behavior actual.
     *   getMovement()                   — movement actual.
     *
     * ── LO QUE NO HACE ───────────────────────────────────────────────────
     *
     *   - No expone setDead()/kill() — los transformers no deben matar proyectiles.
     *   - No expone resetState() — eso es del pool.
     *   - No expone getComponent() — acceso al árbol de componentes no corresponde.
     *   - No expone posición/transform — para redirección basta con velocity.
     *
     * Si un sistema necesita matar un proyectil, lo hace directamente sobre
     * la instancia Bullet (bullet.getBulletLife().kill()), no via transformer.
     */
    interface ProjectileView {

        // ── Velocidad ─────────────────────────────────────────────────────

        double getXSpeed();
        double getYSpeed();

        /**
         * Cambia la velocidad del proyectil.
         * Llamar con (0, 0) para detenerlo (freeze).
         * Llamar con valores invertidos para reflejar (parry/reflection).
         */
        void redirect(double xSpeed, double ySpeed);

        // ── Colisión ──────────────────────────────────────────────────────

        Game.Engine.Colisions.Filter.CollisionProfile getCollisionProfile();

        /**
         * Cambia el perfil de colisión del proyectil.
         * Esencial para: Parry (ENEMY_BULLET → PLAYER_BULLET),
         *                conversión de facción, proyectiles neutrales.
         */
        void changeCollisionProfile(Game.Engine.Colisions.Filter.CollisionProfile profile);

        // ── Daño ──────────────────────────────────────────────────────────

        double getDamage();

        /** Establece el daño del proyectil a un valor absoluto. */
        void changeDamage(double damage);

        /** Multiplica el daño actual por un factor. */
        default void scaleDamage(double factor) {
            changeDamage(getDamage() * factor);
        }

        // ── Behavior ──────────────────────────────────────────────────────

        BulletBehavior getBehavior();

        /**
         * Reemplaza el behavior del proyectil.
         *
         * Usar para conversiones semánticas: un proyectil enemigo que cambia
         * de facción necesita un behavior diferente para su impacto.
         *
         * Si el behavior nuevo tiene estado, el caller es responsable de
         * inicializarlo correctamente antes de pasarlo aquí.
         */
        void changeBehavior(BulletBehavior behavior);

        // ── Movement ──────────────────────────────────────────────────────

        ProjectileMovement getMovement();

        /**
         * Reemplaza el movement del proyectil.
         *
         * Usar para: freeze (parar movimiento), redirección completa,
         * cambio de trayectoria, homing post-spawn.
         *
         * El movement anterior se descarta. Si se quiere componer,
         * el caller puede hacer: bp.getMovement().andThen(newMovement).
         */
        void changeMovement(ProjectileMovement movement);

        // ── Ciclo de vida ─────────────────────────────────────────────────

        /**
         * Mata el proyectil inmediatamente desde una transformación runtime.
         *
         * ── HRFC §19 — Futuras mecánicas: absorción, posesión ────────────
         *
         * Permite que sistemas externos (absorción, posesión, chaining)
         * destruyan el proyectil sin necesitar una referencia directa a
         * Bullet.getBulletLife().kill(). Mantiene el acceso interno encapsulado.
         *
         * Usar para:
         *   - Absorción: absorber el proyectil y destruirlo.
         *   - Posesión: el proyectil "muere" para re-spawnear modificado.
         *   - Chaining: el proyectil se consume al generar una cadena.
         *
         * El proyectil se elimina en el próximo flush del mundo.
         */
        void kill();

        /**
         * Extiende la vida restante del proyectil.
         *
         * ── HRFC §19 — Futuras mecánicas: freeze, posesión, duración ─────
         *
         * Permite que sistemas externos alarguen la duración del proyectil
         * sin modificar la infraestructura de Bullet. No reactiva un
         * proyectil ya muerto — para eso usar changeMovement(LinearMovement.INSTANCE)
         * + extendLifetime combinado si el proyectil fue "pausado" con redirect(0,0).
         *
         * Usar para:
         *   - Freeze: detener velocidad + extender lifetime para que no expire.
         *   - Status effects: proyectiles que duran más bajo ciertos efectos.
         *   - Redirección tardía: proyectil que "espera" antes de moverse.
         *
         * @param extraTicks ticks adicionales de vida (> 0; ignorado si <= 0)
         */
        void extendLifetime(int extraTicks);

        // ── Acceso a la instancia subyacente (package-private bridge) ────

        /**
         * Retorna la instancia Bullet subyacente.
         *
         * Acceso intencionalmente restringido: se usa desde dentro del
         * módulo Bullets para implementar transformaciones que requieren
         * acceso a APIs package-private (resetDamage, etc.).
         *
         * Los sistemas externos NO deben depender de este método.
         * Su uso crea acoplamiento con la implementación concreta.
         *
         * @return la instancia Bullet que respalda esta view
         */
        Bullet bullet();
    }
}
