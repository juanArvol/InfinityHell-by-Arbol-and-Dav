package Game.Items.Types.Weapons.WeaponType;

import Game.Items.Core.ObjectType;
import Game.Items.Core.ObjectTypeFactory;
import Game.Items.Types.Weapons.WeaponDefinition;
import Game.Items.Types.Weapons.WeaponType.WeaponClass.WeaponEscopeta;
import Game.Items.Types.Weapons.WeaponType.WeaponClass.WeaponPistola;

import java.util.function.Supplier;

/**
 * Tipos de arma — paradigma declarativo.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * WeaponType asocia una WeaponDefinition con la factory que crea
 * su comportamiento runtime.
 *
 * WeaponType NO administra el registro global.
 * ObjectTypeFactory es responsable del registro y las consultas.
 *
 * PATRÓN:
 *
 * WeaponDefinition
 *      +
 * Supplier<WeaponComport>
 *      ↓
 * WeaponType
 *      ↓
 * WeaponComport
 */
public final class WeaponType
        extends ObjectType<WeaponComport> {

    // ── Tipos predefinidos ────────────────────────────────────────────────

    public static final WeaponType PISTOLA;

    public static final WeaponType ESCOPETA;

    static {
        PISTOLA = register(new WeaponType(
            WeaponDefinition.PISTOLA,
            WeaponPistola::new
        ));

        ESCOPETA = register(new WeaponType(
            WeaponDefinition.ESCOPETA,
            WeaponEscopeta::new
        ));
    }

    // ── Constructor ──────────────────────────────────────────────────────

    private WeaponType(
            WeaponDefinition definition,
            Supplier<WeaponComport> factory
    ) {
        super(definition, factory);
    }

    // ── API específica ────────────────────────────────────────────────────

    /**
     * Crea una nueva instancia del comportamiento asociado.
     */
    public WeaponComport createComport() {
        return createInstance();
    }

    // ── Registro ─────────────────────────────────────────────────────────

    private static WeaponType register(WeaponType type) {
        return ObjectTypeFactory.register(
                WeaponType.class,
                type
        );
    }
}