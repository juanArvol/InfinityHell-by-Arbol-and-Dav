package Game.Items.Types.Ammulets;

import Game.Items.Core.ObjectType;
import Game.Items.Core.ObjectTypeFactory;
import Game.Items.Types.Ammulets.Types.*;
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
            AmuletDefinition.BONE_TIP,
            BoneTipEffect::new
        ));

        MARKSMAN_SIGHT = register(new AmuletType(
            AmuletDefinition.MARKSMAN_SIGHT,
            MarksmanSightEffect::new
        ));

        SWIFT_QUILL = register(new AmuletType(
            AmuletDefinition.SWIFT_QUILL,
            SwiftQuillEffect::new
        ));

        TEMPO_RING = register(new AmuletType(
            AmuletDefinition.TEMPO_RING,
            TempoRingEffect::new
        ));

        STEADY_GRIP = register(new AmuletType(
            AmuletDefinition.STEADY_GRIP,
            SteadyGripEffect::new
        ));

        PHASE_SHARD = register(new AmuletType(
            AmuletDefinition.PHASE_SHARD,
            PhaseShardEffect::new
        ));

        ECHO_STONE = register(new AmuletType(
            AmuletDefinition.ECHO_STONE,
            EchoStoneEffect::new
        ));

        SPLIT_CRYSTAL = register(new AmuletType(
            AmuletDefinition.SPLIT_CRYSTAL,
            SplitCrystalEffect::new
        ));
    }

    // ── Constructor privado ───────────────────────────────────────────────

    private AmuletType(AmuletDefinition definition, Supplier<AmuletEffect> factory) {
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
