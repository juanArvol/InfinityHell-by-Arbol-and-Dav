package Game.Enemys.Core;

import Game.Enemys.EnemyPhysics;
import Game.Enemys.EnemyPhysicsConfig;
import Sprites.Core.SpriteHandle;

/**
 * Definición estática de un Enemy — los parámetros de construcción.
 *
 * ── Por qué existe ───────────────────────────────────────────────────────
 * EnemyDefinition separa "qué datos tiene un enemy" de "cómo se ensambla".
 * El Assembler lee la definición y construye el Enemy con ella.
 *
 * Esto permite almacenar definiciones en registros, ficheros de configuración
 * o factories sin acoplar la lógica de ensamblado a los datos concretos.
 *
 * ── Qué contiene ─────────────────────────────────────────────────────────
 *   sprite    — handle del sprite principal del enemy.
 *   maxHealth — vida máxima.
 *   physics   — física del engine (gravedad, masa, velocidad, etc.).
 *   colliderW — ancho del collider en píxeles.
 *   colliderH — alto del collider en píxeles.
 *
 * ── Qué NO contiene ──────────────────────────────────────────────────────
 * Comportamiento. Una definición no sabe cómo se mueve, qué IA usa ni qué
 * ataca. Eso es responsabilidad exclusiva del Assembler.
 *
 * ── Construcción ─────────────────────────────────────────────────────────
 * Usar el Builder estático para mayor legibilidad:
 *
 *   EnemyDefinition def = EnemyDefinition.builder()
 *       .sprite(EnemyAssets.normalHandle)
 *       .health(100)
 *       .physics(EnemyPhysicsConfig.groundStandard())
 *       .collider(24, 30)
 *       .build();
 */
public final class EnemyDefinition {

    public final SpriteHandle    sprite;
    public final int             maxHealth;
    public final EnemyPhysics    physics;
    public final int             colliderW;
    public final int             colliderH;

    private EnemyDefinition(Builder b) {
        if (b.sprite == null)  throw new IllegalStateException("EnemyDefinition: sprite is required");
        if (b.health <= 0)     throw new IllegalStateException("EnemyDefinition: health must be > 0");
        if (b.physics == null) throw new IllegalStateException("EnemyDefinition: physics is required");

        this.sprite    = b.sprite;
        this.maxHealth = b.health;
        this.physics   = b.physics;
        this.colliderW = b.colliderW > 0 ? b.colliderW : 24;
        this.colliderH = b.colliderH > 0 ? b.colliderH : 30;
    }

    // ── Builder ───────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {

        private SpriteHandle sprite;
        private int          health;
        private EnemyPhysics physics;
        private int          colliderW;
        private int          colliderH;

        private Builder() {}

        public Builder sprite(SpriteHandle handle) {
            this.sprite = handle;
            return this;
        }

        public Builder health(int maxHealth) {
            this.health = maxHealth;
            return this;
        }

        public Builder physics(EnemyPhysicsConfig config) {
            this.physics = new EnemyPhysics(config);
            return this;
        }

        public Builder physics(EnemyPhysics physics) {
            this.physics = physics;
            return this;
        }

        public Builder collider(int width, int height) {
            this.colliderW = width;
            this.colliderH = height;
            return this;
        }

        public EnemyDefinition build() {
            return new EnemyDefinition(this);
        }
    }
}
