package Game.Items.Types.Weapons;

import Game.Items.Creation.ItemID;

/**
 * Identificadores tipados para tipos de arma.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * PROPÓSITO:
 *   Reemplazar String IDs con tipos fuertemente tipados.
 *   Cada constante representa un tipo de arma disponible en el juego.
 *
 * VENTAJAS:
 *   - No más typos: WeaponID.PISTOLA vs "pistola"
 *   - Autocomplete del IDE
 *   - Refactoring seguro
 *   - Lista explícita de todos los IDs disponibles
 *
 * AÑADIR NUEVO ID:
 *   1. Agregar constante al enum
 *   2. El asString() convierte automáticamente a snake_case lowercase
 *   3. Listo — no requiere cambios en otros archivos
 */
public enum WeaponID implements ItemID {
    PISTOLA,
    ESCOPETA;

    @Override
    public String asString() {
        return name().toLowerCase();
    }
}
