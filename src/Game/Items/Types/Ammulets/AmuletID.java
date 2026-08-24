package Game.Items.Types.Ammulets;

import Game.Items.Creation.ItemID;

/**
 * Identificadores tipados para tipos de amuleto.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * PROPÓSITO:
 *   Reemplazar String IDs con tipos fuertemente tipados.
 *   Cada constante representa un tipo de amuleto disponible en el juego.
 *
 * VENTAJAS:
 *   - No más typos: AmuletID.PIERCING vs "piercing"
 *   - Autocomplete del IDE
 *   - Refactoring seguro
 *   - Lista explícita de todos los IDs disponibles
 *
 * AÑADIR NUEVO ID:
 *   1. Agregar constante al enum
 *   2. El asString() convierte automáticamente a snake_case lowercase
 *   3. Listo — no requiere cambios en otros archivos
 */
public enum AmuletID implements ItemID {
    BONE_TIP,
    MARKSMAN_SIGHT,
    SWIFT_QUILL,
    TEMPO_RING,
    STEADY_GRIP,
    PHASE_SHARD,
    ECHO_STONE,
    SPLIT_CRYSTAL,
    PIERCING,
    BOUNCE;

    @Override
    public String asString() {
        return name().toLowerCase();
    }
}
