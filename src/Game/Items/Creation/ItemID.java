package Game.Items.Creation;

/**
 * Interfaz base para identificadores tipados de Items.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * PROPÓSITO:
 *   Reemplazar String IDs con tipos fuertemente tipados (enums).
 *   Cada familia de Items (Bullet, Weapon, Amulet) define su propio enum
 *   que implementa esta interfaz.
 *
 * VENTAJAS:
 *   - Seguridad en tiempo de compilación (no más typos en strings)
 *   - Autocomplete del IDE
 *   - Refactoring seguro
 *   - Lista explícita de todos los IDs disponibles
 *
 * EJEMPLO:
 *   public enum BulletID implements ItemID {
 *       NORMAL_BULLET,
 *       SPRING_BULLET,
 *       METEOR_BULLET;
 *
 *       @Override
 *       public String asString() {
 *           return name().toLowerCase();
 *       }
 *   }
 *
 * NOTA:
 *   name() ya pertenece a Enum, no necesita declararse en ItemID.
 *
 * USO:
 *   BulletID id = BulletID.NORMAL_BULLET;
 *   String stringId = id.asString(); // "normal_bullet"
 */
public interface ItemID {
    
    /**
     * Identificador externo/canónico del Item.
     * Por convención: snake_case lowercase.
     */
    String asString();
}
