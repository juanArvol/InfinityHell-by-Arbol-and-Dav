package Game.Engine.Entity.Components;

import Game.Engine.Component;
import Game.Engine.Physics.Contact.ContactState;
import Game.Engine.Physics.KineticPhysics.Types.Physics2D;

/**
 * Componente que le da física a un GameObjects.
 * Solo es un wrapper — la lógica real está en Physics y sus subclases.
 *
 * ── HRFC — Generalización del sistema de Collision ────────────────────────
 *
 * Añade almacenamiento del ContactState del frame actual.
 * CollisionsSystem lo escribe en FASE 1 después de resolver todos los
 * contactos del objeto. Los consumidores (animaciones, gameplay, AI) lo
 * leen para conocer el estado de contacto completo sin recalcular geometría.
 *
 *   ContactState cs = physComp.getContactState();
 *   if (cs.isOnGround()) { ... }
 *   if (cs.isOnWall())   { ... }
 *   if (cs.isOnCeiling()) { ... }
 *
 * El estado por defecto es ContactState.NONE (sin contacto).
 * Se resetea automáticamente al inicio de cada frame por CollisionsSystem.
 */
public class Physics2DComponent extends Component {

    private final Physics2D physics;

    /**
     * Estado de contacto del frame actual.
     * Escrito por CollisionsSystem en FASE 1 tras combinar todos los
     * ContactRecords del frame. Nunca es null — el estado vacío es
     * ContactState.NONE.
     */
    private ContactState contactState = ContactState.NONE;

    public Physics2DComponent(Physics2D physics) {
        this.physics = physics;
    }

    public Physics2D getPhysics() {
        return physics;
    }

    // ── ContactState ──────────────────────────────────────────────────────

    /**
     * Estado de contacto calculado por CollisionsSystem en el frame actual.
     * Nunca retorna null. Si no hay contacto activo retorna {@link ContactState#NONE}.
     */
    public ContactState getContactState() {
        return contactState;
    }

    /**
     * Asigna el ContactState del frame actual.
     * Solo debe ser llamado por CollisionsSystem en FASE 1.
     *
     * @param state estado de contacto. Si es null se asigna ContactState.NONE.
     */
    public void setContactState(ContactState state) {
        this.contactState = (state != null) ? state : ContactState.NONE;
    }
}
