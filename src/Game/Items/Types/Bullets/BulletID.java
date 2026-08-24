package Game.Items.Types.Bullets;

import Game.Items.Creation.ItemID;

/**
 * Identificadores tipados para tipos de bala.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * PROPÓSITO:
 *   Reemplazar String IDs con tipos fuertemente tipados.
 *   Cada constante representa un tipo de bala disponible en el juego.
 *
 * VENTAJAS:
 *   - No más typos: BulletID.NORMAL_BULLET vs "normal_bullet"
 *   - Autocomplete del IDE
 *   - Refactoring seguro
 *   - Lista explícita de todos los IDs disponibles
 *
 * AÑADIR NUEVO ID:
 *   1. Agregar constante al enum
 *   2. El asString() convierte automáticamente a snake_case lowercase
 *   3. Listo — no requiere cambios en otros archivos
 */
public enum BulletID implements ItemID {
    NORMAL_BULLET,
    SPRING_BULLET,
    METEOR_BULLET,
    THUNDER_BULLET,
    FROST_BULLET,
    SUMMON_BULLET;

    @Override
    public String asString() {
        return name().toLowerCase();
    }
}
