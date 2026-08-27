package Game.Items.Types.Ammulets;

import Game.Items.Creation.ItemDefinition;
import Game.Items.Creation.ItemRarity;
import Game.Items.Creation.VisualDefinition;

/**
 * Definiciones estáticas de tipos de amuleto.
 *
 * ── ARQUITECTURA FINAL — Items Module ────────────────────────────────────
 *
 * AmuletDefinition representa la definición declarativa de un amuleto.
 *
 * HERENCIA:
 *
 *   ItemDefinition
 *        └── AmuletDefinition
 *
 * La definición contiene únicamente identidad y datos visuales.
 * No contiene el efecto runtime del amuleto.
 *
 * PATRÓN:
 *
 *   AmuletDefinition → ItemDefinition especializada (ID + visual)
 *   AmuletType       → ObjectType (AmuletDefinition + factory)
 */
public final class AmuletDefinition extends ItemDefinition {

    // ── Definiciones estáticas ────────────────────────────────────────────

    public static final AmuletDefinition BONE_TIP =
            new AmuletDefinition(
                    AmuletID.BONE_TIP,
                    new VisualDefinition(
                            "Punta de Hueso",
                            "Aumenta el daño base del arma",
                            ItemRarity.COMMON
                    )
            );

    public static final AmuletDefinition MARKSMAN_SIGHT =
            new AmuletDefinition(
                    AmuletID.MARKSMAN_SIGHT,
                    new VisualDefinition(
                            "Mira de Tirador",
                            "Muestra la trayectoria de las balas",
                            ItemRarity.UNCOMMON
                    )
            );

    public static final AmuletDefinition SWIFT_QUILL =
            new AmuletDefinition(
                    AmuletID.SWIFT_QUILL,
                    new VisualDefinition(
                            "Pluma Veloz",
                            "Aumenta la velocidad de las balas",
                            ItemRarity.UNCOMMON
                    )
            );

    public static final AmuletDefinition TEMPO_RING =
            new AmuletDefinition(
                    AmuletID.TEMPO_RING,
                    new VisualDefinition(
                            "Anillo del Tempo",
                            "Reduce el tiempo de recarga del arma",
                            ItemRarity.RARE
                    )
            );

    public static final AmuletDefinition STEADY_GRIP =
            new AmuletDefinition(
                    AmuletID.STEADY_GRIP,
                    new VisualDefinition(
                            "Empuñadura Firme",
                            "Reduce la dispersión del arma",
                            ItemRarity.RARE
                    )
            );

    public static final AmuletDefinition PHASE_SHARD =
            new AmuletDefinition(
                    AmuletID.PHASE_SHARD,
                    new VisualDefinition(
                            "Fragmento de Fase",
                            "Las balas atraviesan un enemigo adicional",
                            ItemRarity.EPIC
                    )
            );

    public static final AmuletDefinition ECHO_STONE =
            new AmuletDefinition(
                    AmuletID.ECHO_STONE,
                    new VisualDefinition(
                            "Piedra del Eco",
                            "Las balas rebotan una vez al impactar",
                            ItemRarity.EPIC
                    )
            );

    public static final AmuletDefinition SPLIT_CRYSTAL =
            new AmuletDefinition(
                    AmuletID.SPLIT_CRYSTAL,
                    new VisualDefinition(
                            "Cristal Divisor",
                            "Multiplica significativamente el número de proyectiles",
                            ItemRarity.LEGENDARY
                    )
            );

    // ── Constructor ──────────────────────────────────────────────────────

    private AmuletDefinition(
            AmuletID amuletId,
            VisualDefinition visual
    ) {
        super(amuletId, visual);
    }

    // ── API específica de dominio ─────────────────────────────────────────

    /**
     * Retorna el identificador específico de amuletos.
     */
    public AmuletID getAmuletId() {
        return (AmuletID) getItemId();
    }
}