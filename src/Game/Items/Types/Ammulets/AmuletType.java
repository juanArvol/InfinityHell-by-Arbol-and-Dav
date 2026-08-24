package Game.Items.Types.Ammulets;

import Game.Items.Core.ObjectType;
import Game.Items.Core.ObjectTypeFactory;
import Game.Items.Creation.ItemDefinition;
import Game.Items.Types.Ammulets.Effects.*;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;
import java.util.function.Supplier;

/**
 * Tipos de amuleto — mejoras pasivas acumulables.
 *
 * ── ARQUITECTURA FINAL — Items Module ────────────────────────────────────
 *
 * AmuletType SOLO contiene las instancias estáticas de tipos de amuleto.
 * Toda la lógica de registro y consulta está en ObjectTypeFactory.
 *
 * PATRÓN:
 *   AmuletDefinitions → ItemDefinition estática (ID + visual)
 *   AmuletType        → ObjectType (ItemDefinition + factory)
 *
 * @see ObjectType contenedor base
 * @see ObjectTypeFactory lógica de registro
 */
public final class AmuletType extends ObjectType<AmuletEffect> {

    // ── Tipos predefinidos ────────────────────────────────────────────────

    public static final AmuletType BONE_TIP;
    public static final AmuletType MARKSMAN_SIGHT;
    public static final AmuletType SWIFT_QUILL;
    public static final AmuletType TEMPO_RING;
    public static final AmuletType STEADY_GRIP;
    public static final AmuletType PHASE_SHARD;
    public static final AmuletType ECHO_STONE;
    public static final AmuletType SPLIT_CRYSTAL;

    static {
        BONE_TIP = register(new AmuletType(
            AmuletDefinitions.BONE_TIP,
            () -> new AmuletEffect() {
                @Override
                public void applyToStats(WeaponStats stats) {
                    stats.setDamageBonusByWeapon(stats.getDamageBonusByWeapon() + 8.0);
                }
            }
        ));

        MARKSMAN_SIGHT = register(new AmuletType(
            AmuletDefinitions.MARKSMAN_SIGHT,
            () -> new UICapabilityEffect(
                () -> new Game.Gameplay.UI.Aim.TrajectoryVisualizationCapability(
                    Game.Gameplay.UI.Aim.TrajectoryVisualizationCapability.TrajectoryStyle.FADE,
                    java.awt.Color.CYAN
                )
            )
        ));

        SWIFT_QUILL = register(new AmuletType(
            AmuletDefinitions.SWIFT_QUILL,
            () -> new AmuletEffect() {
                @Override
                public void applyToStats(WeaponStats stats) {
                    stats.setBulletSpeedBase(stats.getBulletSpeedBase() * 1.15);
                }
            }
        ));

        TEMPO_RING = register(new AmuletType(
            AmuletDefinitions.TEMPO_RING,
            () -> new AmuletEffect() {
                @Override
                public void applyToStats(WeaponStats stats) {
                    stats.setCooldown(stats.getCooldown() * 0.90);
                }
            }
        ));

        STEADY_GRIP = register(new AmuletType(
            AmuletDefinitions.STEADY_GRIP,
            () -> new AmuletEffect() {
                @Override
                public void applyToStats(WeaponStats stats) {
                    stats.setSpread(stats.getSpread() * 0.80);
                }
            }
        ));

        PHASE_SHARD = register(new AmuletType(
            AmuletDefinitions.PHASE_SHARD,
            () -> new AmuletEffect() {
                @Override
                public BulletBehavior wrapBehavior(BulletBehavior base) {
                    return new PiercingAmuletWrapper(base, 1);
                }
            }
        ));

        ECHO_STONE = register(new AmuletType(
            AmuletDefinitions.ECHO_STONE,
            () -> new AmuletEffect() {
                @Override
                public BulletBehavior wrapBehavior(BulletBehavior base) {
                    return new BounceAmuletWrapper(
                        base, 1, AmuletRegistry.getEntityProvider()
                    );
                }
            }
        ));

        SPLIT_CRYSTAL = register(new AmuletType(
            AmuletDefinitions.SPLIT_CRYSTAL,
            () -> new AmuletEffect() {
                @Override
                public void applyToStats(WeaponStats stats) {
                    stats.setBulletsPerShot(stats.getBulletsPerShot() + 100);
                }
            }
        ));
    }

    // ── Constructor privado ───────────────────────────────────────────────

    private AmuletType(ItemDefinition definition, Supplier<AmuletEffect> factory) {
        super(definition, factory);
    }

    // ── API específica de dominio ─────────────────────────────────────────

    /**
     * Crea una nueva instancia del AmuletEffect asociado.
     * Alias de createInstance() para mayor claridad en el dominio.
     */
    public AmuletEffect createEffect() {
        return createInstance();
    }

    // ── Registro privado ──────────────────────────────────────────────────

    private static AmuletType register(AmuletType type) {
        return ObjectTypeFactory.register(AmuletType.class, type);
    }
}
