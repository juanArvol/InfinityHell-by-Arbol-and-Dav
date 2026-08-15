package Game.Enemys.Core;

import Game.Enemys.EnemyPhysics;
import Game.Enemys.EnemyPhysicsConfig;
import Game.Engine.Entity.Attributes.EntityAttributes;
import Game.Engine.Entity.Components.Collisions.MaterialComponent;
import Game.Engine.Entity.Flags.EntityFlags;
import Game.Engine.Entity.Stats.EntityStats;
import Sprites.Core.SpriteHandle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Prefab reutilizable de un Enemy — descripción completa de construcción.
 *
 * ── HRFC-007 — Living Entity Core ────────────────────────────────────────
 * Los configuradores de módulos (defaultStats, defaultFlags, defaultAttributes)
 * ahora reciben los tipos genéricos del Living Entity Core:
 *
 *   StatsConfigurator      → configura EntityStats  (era EnemyStats)
 *   FlagsConfigurator      → configura EntityFlags  (era EnemyFlags)
 *   AttributesConfigurator → configura EntityAttributes (era EnemyAttributes)
 *
 * ── HRFC FASE 1.5 — Universal Physical Properties Integration ─────────────
 * EnemyDefinition ahora incluye un MaterialComponent opcional.
 * Si no se declara en el Builder, EnemyAssembler usa BIOLOGICAL_DEFAULT.
 *
 * Esto permite declarar materiales distintos por tipo de enemigo:
 *
 *   Zombie    → material biológico por defecto
 *   Esqueleto → material óseo (dureza alta, densidad baja)
 *   Golem     → material rocoso (conductividad baja, densidad alta)
 *   Elemental → material específico del elemento (fuego, hielo, rayo)
 *
 * sin modificar el EnemyAssembler base.
 *
 * ── Qué describe un EnemyDefinition ──────────────────────────────────────
 *   Núcleo     : sprite, maxHealth, physics, colliderW/H
 *   Físico     : material (opcional — default biológico si null)
 *   Visual     : renderLayer, animationIds, hasShadow, shadowW/H
 *   Audio      : soundIds (claves semánticas de audio)
 *   Loot       : lootTableId
 *   Módulos    : defaultStats, defaultFlags, defaultAttributes
 *
 * ── Qué NO contiene ──────────────────────────────────────────────────────
 * Comportamiento. Una definición no sabe cómo se mueve, qué IA usa ni
 * qué ataca. Eso es responsabilidad exclusiva del Assembler.
 */
public final class EnemyDefinition {

    // ── Material biológico por defecto ────────────────────────────────────
    // Usado por EnemyAssembler cuando la definición no declara material.
    // Representa un ser vivo genérico (criatura orgánica, no-muerto básico).
    //
    // Propiedades basadas en tejido biológico húmedo:
    //   thermalConductivity  0.5  — conductor moderado (agua + tejido)
    //   heatCapacity        2000  — capacidad media (tejido muscular)
    //   thermalDiffusivity  0.12  — disipación moderada
    //   electricalConductivity 0.35 — conductor moderado (fluidos corporales)
    //   humidityAbsorption  0.4  — absorbe humedad (piel porosa)
    //   density             950  — ligeramente menos denso que el agua
    //   compressibility     0.08 — tejidos blandos, algo compresibles
    //   elasticity          0.35 — tejidos con algo de elasticidad
    //   hardness            0.15 — cuerpo blando
    public static final MaterialComponent BIOLOGICAL_DEFAULT = MaterialComponent.builder()
        .thermalConductivity(0.5)
        .heatCapacity(2000.0)
        .thermalDiffusivity(0.12)
        .electricalConductivity(0.35)
        .humidityAbsorption(0.4)
        .density(950.0)
        .compressibility(0.08)
        .elasticity(0.35)
        .hardness(0.15)
        .build();

    // ── Núcleo ────────────────────────────────────────────────────────────
    public final SpriteHandle      sprite;
    public final int               maxHealth;
    public final EnemyPhysics      physics;
    public final int               colliderW;
    public final int               colliderH;

    // ── Material físico ───────────────────────────────────────────────────
    // Null = usar BIOLOGICAL_DEFAULT en EnemyAssembler.
    public final MaterialComponent material;

    // ── Visual ────────────────────────────────────────────────────────────
    public final int          renderLayer;
    public final List<String> animationIds;
    public final boolean      hasShadow;
    public final int          shadowW;
    public final int          shadowH;

    // ── Audio ─────────────────────────────────────────────────────────────
    public final List<SoundEntry> sounds;

    // ── Loot ─────────────────────────────────────────────────────────────
    public final String lootTableId;

    // ── Módulos por defecto — Living Entity Core ──────────────────────────
    public final StatsConfigurator      defaultStats;
    public final FlagsConfigurator      defaultFlags;
    public final AttributesConfigurator defaultAttributes;

    // ── Interfaces funcionales para configuradores ────────────────────────

    @FunctionalInterface
    public interface StatsConfigurator {
        void configure(EntityStats stats);
    }

    @FunctionalInterface
    public interface FlagsConfigurator {
        void configure(EntityFlags flags);
    }

    @FunctionalInterface
    public interface AttributesConfigurator {
        void configure(EntityAttributes attributes);
    }

    // ── Record de sonido ──────────────────────────────────────────────────

    public record SoundEntry(String key, String assetId) {}

    // ── Constructor privado ───────────────────────────────────────────────

    private EnemyDefinition(Builder b) {
        if (b.sprite == null)  throw new IllegalStateException("EnemyDefinition: sprite is required");
        if (b.health <= 0)     throw new IllegalStateException("EnemyDefinition: health must be > 0");
        if (b.physics == null) throw new IllegalStateException("EnemyDefinition: physics is required");

        this.sprite    = b.sprite;
        this.maxHealth = b.health;
        this.physics   = b.physics;
        this.colliderW = b.colliderW > 0 ? b.colliderW : 24;
        this.colliderH = b.colliderH > 0 ? b.colliderH : 30;

        // null = EnemyAssembler usa BIOLOGICAL_DEFAULT
        this.material  = b.material;

        this.renderLayer  = b.renderLayer;
        this.animationIds = Collections.unmodifiableList(new ArrayList<>(b.animationIds));
        this.hasShadow    = b.hasShadow;
        this.shadowW      = b.shadowW;
        this.shadowH      = b.shadowH;

        this.sounds      = Collections.unmodifiableList(new ArrayList<>(b.sounds));
        this.lootTableId = b.lootTableId;

        this.defaultStats      = b.defaultStats;
        this.defaultFlags      = b.defaultFlags;
        this.defaultAttributes = b.defaultAttributes;
    }

    // ── Builder ───────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {

        // Núcleo
        private SpriteHandle sprite;
        private int          health;
        private EnemyPhysics physics;
        private int          colliderW;
        private int          colliderH;

        // Material físico (null = usar BIOLOGICAL_DEFAULT)
        private MaterialComponent material = null;

        // Visual
        private int          renderLayer  = 1;
        private List<String> animationIds = new ArrayList<>();
        private boolean      hasShadow    = false;
        private int          shadowW      = 18;
        private int          shadowH      = 7;

        // Audio
        private List<SoundEntry> sounds = new ArrayList<>();

        // Loot
        private String lootTableId;

        // Módulos por defecto
        private StatsConfigurator       defaultStats;
        private FlagsConfigurator       defaultFlags;
        private AttributesConfigurator  defaultAttributes;

        private Builder() {}

        public Builder sprite(SpriteHandle handle)          { this.sprite = handle; return this; }
        public Builder health(int maxHealth)                { this.health = maxHealth; return this; }
        public Builder physics(EnemyPhysicsConfig config)   { this.physics = new EnemyPhysics(config); return this; }
        public Builder physics(EnemyPhysics physics)        { this.physics = physics; return this; }
        public Builder material(MaterialComponent mat)      { this.material = mat; return this; }
        public Builder collider(int width, int height)      { this.colliderW = width; this.colliderH = height; return this; }
        public Builder renderLayer(int layer)               { this.renderLayer = layer; return this; }
        public Builder animation(String animationId)        { this.animationIds.add(animationId); return this; }

        public Builder shadow(int width, int height) {
            this.hasShadow = true;
            this.shadowW   = width;
            this.shadowH   = height;
            return this;
        }

        public Builder sound(String key, String assetId) {
            this.sounds.add(new SoundEntry(key, assetId));
            return this;
        }

        public Builder lootTable(String tableId)                        { this.lootTableId = tableId; return this; }
        public Builder defaultStats(StatsConfigurator c)               { this.defaultStats = c; return this; }
        public Builder defaultFlags(FlagsConfigurator c)               { this.defaultFlags = c; return this; }
        public Builder defaultAttributes(AttributesConfigurator c)     { this.defaultAttributes = c; return this; }

        public EnemyDefinition build() { return new EnemyDefinition(this); }
    }
}
