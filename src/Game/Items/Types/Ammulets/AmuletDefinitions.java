package Game.Items.Types.Ammulets;

import Game.Items.Creation.ItemDefinition;
import Game.Items.Creation.ItemRarity;
import Game.Items.VisualDefinition;

/**
 * Definiciones estáticas de tipos de amuleto.
 *
 * ── ARQUITECTURA FINAL — Items Module ────────────────────────────────────
 *
 * PROPÓSITO:
 *   Centralizar todas las definiciones (ID + visual) de amuletos en un solo lugar.
 *   AmuletType solo referencia estas definiciones + añade el factory.
 *
 * PATRÓN:
 *   AmuletDefinitions → ItemDefinition estática (ID + visual)
 *   AmuletType        → ObjectType (ItemDefinition + factory)
 */
public final class AmuletDefinitions {

    // ── Definiciones estáticas ────────────────────────────────────────────

    public static final ItemDefinition BONE_TIP = new ItemDefinition(
        AmuletID.BONE_TIP,
        new VisualDefinition(
            "Punta de Hueso",
            "Aumenta el daño base del arma",
            ItemRarity.COMMON
        )
    );

    public static final ItemDefinition MARKSMAN_SIGHT = new ItemDefinition(
        AmuletID.MARKSMAN_SIGHT,
        new VisualDefinition(
            "Mira de Tirador",
            "Muestra la trayectoria de las balas",
            ItemRarity.UNCOMMON
        )
    );

    public static final ItemDefinition SWIFT_QUILL = new ItemDefinition(
        AmuletID.SWIFT_QUILL,
        new VisualDefinition(
            "Pluma Veloz",
            "Aumenta la velocidad de las balas",
            ItemRarity.UNCOMMON
        )
    );

    public static final ItemDefinition TEMPO_RING = new ItemDefinition(
        AmuletID.TEMPO_RING,
        new VisualDefinition(
            "Anillo del Tempo",
            "Reduce el tiempo de recarga del arma",
            ItemRarity.RARE
        )
    );

    public static final ItemDefinition STEADY_GRIP = new ItemDefinition(
        AmuletID.STEADY_GRIP,
        new VisualDefinition(
            "Empuñadura Firme",
            "Reduce la dispersión del arma",
            ItemRarity.RARE
        )
    );

    public static final ItemDefinition PHASE_SHARD = new ItemDefinition(
        AmuletID.PHASE_SHARD,
        new VisualDefinition(
            "Fragmento de Fase",
            "Las balas atraviesan un enemigo adicional",
            ItemRarity.EPIC
        )
    );

    public static final ItemDefinition ECHO_STONE = new ItemDefinition(
        AmuletID.ECHO_STONE,
        new VisualDefinition(
            "Piedra del Eco",
            "Las balas rebotan una vez al impactar",
            ItemRarity.EPIC
        )
    );

    public static final ItemDefinition SPLIT_CRYSTAL = new ItemDefinition(
        AmuletID.SPLIT_CRYSTAL,
        new VisualDefinition(
            "Cristal Divisor",
            "Multiplica significativamente el número de proyectiles",
            ItemRarity.LEGENDARY
        )
    );

    // ── Constructor privado ───────────────────────────────────────────────

    private AmuletDefinitions() {
        throw new AssertionError("No se puede instanciar AmuletDefinitions");
    }
}
