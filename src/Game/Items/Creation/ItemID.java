package Game.Items.Creation;

/**
 * Identificador tipado de un Item.
 *
 * ── ARCHITECTURE — Items Module ──────────────────────────────────────────
 *
 * ItemID representa la identidad lógica de un Item.
 *
 * Cada familia concreta define su propio identificador:
 *
 *   BulletID implements ItemID
 *   WeaponID implements ItemID
 *   AmuletID implements ItemID
 *
 * El ID tipado es la identidad interna.
 * asString() proporciona su representación canónica externa.
 *
 * La representación externa debe ser:
 *
 *   - lowercase
 *   - snake_case
 *   - estable
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
 * NOTA:
 * name() ya pertenece a Enum, no necesita declararse en ItemID.
 */
public interface ItemID {

    /**
     * Retorna la representación canónica externa del ID.
     */
    String asString();
}