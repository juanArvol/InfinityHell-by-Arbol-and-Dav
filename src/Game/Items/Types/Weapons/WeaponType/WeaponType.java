package Game.Items.Types.Weapons.WeaponType;

import Game.Items.Core.ObjectType;
import Game.Items.Core.ObjectTypeFactory;
import Game.Items.Creation.ItemDefinition;
import Game.Items.Types.Weapons.WeaponDefinitions;
import Game.Items.Types.Weapons.WeaponType.WeaponClass.WeaponEscopeta;
import Game.Items.Types.Weapons.WeaponType.WeaponClass.WeaponPistola;
import java.util.function.Supplier;

/**
 * Tipos de arma — paradigma declarativo.
 *
 * ── ARQUITECTURA FINAL — Items Module ────────────────────────────────────
 *
 * WeaponType SOLO contiene las instancias estáticas de tipos de arma.
 * Toda la lógica de registro y consulta está en ObjectTypeFactory.
 *
 * PATRÓN:
 *   WeaponDefinitions → ItemDefinition estática (ID + visual)
 *   WeaponType        → ObjectType (ItemDefinition + factory)
 *
 * @see ObjectType contenedor base
 * @see ObjectTypeFactory lógica de registro
 */
public final class WeaponType extends ObjectType<WeaponComport> {

    // ── Tipos predefinidos ────────────────────────────────────────────────

    public static final WeaponType PISTOLA;
    public static final WeaponType ESCOPETA;

    static {
        PISTOLA = register(new WeaponType(
            WeaponDefinitions.PISTOLA,
            WeaponPistola::new
        ));

        ESCOPETA = register(new WeaponType(
            WeaponDefinitions.ESCOPETA,
            WeaponEscopeta::new
        ));
    }

    // ── Constructor privado ───────────────────────────────────────────────

    private WeaponType(ItemDefinition definition, Supplier<WeaponComport> factory) {
        super(definition, factory);
    }

    // ── API específica de dominio ─────────────────────────────────────────

    /**
     * Crea una nueva instancia del WeaponComport asociado.
     * Alias de createInstance() para mayor claridad en el dominio.
     */
    public WeaponComport createComport() {
        return createInstance();
    }

    // ── Registro privado ──────────────────────────────────────────────────

    private static WeaponType register(WeaponType type) {
        return ObjectTypeFactory.register(WeaponType.class, type);
    }
}
