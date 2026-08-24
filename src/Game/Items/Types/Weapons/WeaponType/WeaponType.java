package Game.Items.Types.Weapons.WeaponType;

import Game.Items.Core.ItemTypeRegistry;
import Game.Items.Core.ObjectType;
import Game.Items.ItemRarity;
import Game.Items.Types.Weapons.WeaponType.WeaponClass.WeaponEscopeta;
import Game.Items.Types.Weapons.WeaponType.WeaponClass.WeaponPistola;
import java.util.*;
import java.util.function.Supplier;

/**
 * Tipos de arma — el paradigma declarativo equivalente a BulletType.
 *
 * ── HRFC — Items Module Architectural Consolidation ──────────────────────
 *
 * MIGRACIÓN COMPLETADA:
 *   - WeaponType ahora extiende ItemTypeBase<WeaponComport>
 *   - Usa ItemTypeRegistry para almacenamiento centralizado
 *   - Elimina duplicación de infraestructura con BulletType
 *
 * CONTRATOS PRESERVADOS:
 *   ✅ WeaponType.PISTOLA, WeaponType.ESCOPETA — static final accesibles
 *   ✅ weaponType.createComport() — sigue funcionando exactamente igual
 *   ✅ weaponType.defaultRarity — sigue siendo public final
 *   ✅ weaponType.displayName — sigue siendo public final
 *   ✅ weaponType.description — sigue siendo public final
 *   ✅ WeaponType.values() — reemplaza enum.values()
 *   ✅ WeaponType.get(id) — resolución por ID
 *
 * ── HRFC — Player Reengineering ───────────────────────────────────────────
 *
 * MOTIVACIÓN:
 *   Antes, PlayerCombat recibía directamente un {@code new WeaponPistola()},
 *   acoplando el código de loadout a la clase concreta. Eso viola el mismo
 *   principio que ya se resolvió en proyectiles con BulletType + ProjectileRegistry.
 *
 *   WeaponType introduce el paradigma equivalente para armas:
 *
 *       BulletType.NORMALBULLET  →  WeaponType.PISTOLA
 *       ProjectileRegistry       →  WeaponRegistry
 *       BulletBehavior.create()  →  WeaponType.createComport()
 *
 * ── DISEÑO ────────────────────────────────────────────────────────────────
 *
 * Cada WeaponType representa un arma que el jugador puede obtener en la run.
 * La rareza controla la frecuencia de aparición en loot/tiendas.
 *
 * La construcción concreta (WeaponPistola, WeaponEscopeta) queda centralizada
 * aquí — el resto del código declara solo el tipo:
 *
 *   PlayerLoadout.with(WeaponType.PISTOLA)
 *
 * sin conocer WeaponPistola.
 *
 * ── AÑADIR UN NUEVO ARMA ──────────────────────────────────────────────────
 *
 * ANTES (enum cerrado):
 *   1. Modificar WeaponType.java
 *   2. Añadir entrada en el enum
 *
 * AHORA (class extensible con infraestructura unificada):
 *   1. Crear la clase WeaponComport en tu módulo (ej: WeaponShotgun.java)
 *   2. Registrar en tu módulo:
 *      WeaponType.register(new WeaponType("shotgun", WeaponShotgun::new, ...))
 *   3. ¡Listo! — WeaponType.java NO se modifica
 *
 * ── RELACIÓN CON WeaponRegistry ───────────────────────────────────────────
 *
 * WeaponRegistry trabaja con IDs de string ("ethereal_revolver") para el
 * sistema de loot/tiendas. WeaponType trabaja con constantes tipadas para
 * el código de loadout y combate. Ambos son válidos y complementarios:
 *
 *   WeaponType  → código de gameplay (loadout, combate, tests)
 *   WeaponRegistry → sistema de loot, tiendas, balance externo
 *
 * WeaponType.createComport() produce el mismo WeaponComport que
 * WeaponRegistry.get(id).createComport() para los armas equivalentes.
 *
 * @see Game.Items.Creation.ObjectType  clase base unificada
 * @see Game.Items.Creation.ItemTypeRegistry  registro centralizado
 * @see Game.Items.Types.Bullets.Definition.BulletType  el patrón que este tipo replica
 * @see Game.Items.Types.Weapons.WeaponRegistry  registro de loot complementario
 */
public final class WeaponType extends ObjectType<WeaponComport> {

    // ── Tipos predefinidos ────────────────────────────────────────────────

    /** Tipos predefinidos — constantes para compatibilidad con código existente. */
    public static final WeaponType PISTOLA;
    public static final WeaponType ESCOPETA;

    static {
        // Inicializar tipos base — mismo orden que el enum original
        PISTOLA = register(new WeaponType(
            "pistola",
            WeaponPistola::new,
            ItemRarity.COMMON,
            "Pistola del Vacío",
            "Un arma forjada con residuos del vacío. Disparo automático, confiable."
        ));

        ESCOPETA = register(new WeaponType(
            "escopeta",
            WeaponEscopeta::new,
            ItemRarity.UNCOMMON,
            "Dispersor de Esquirlas",
            "Expulsa un cono de fragmentos de energía. Devastadora a corta distancia."
        ));
    }

    // ── Constructor (público para extensibilidad) ─────────────────────────

    /**
     * Construye un nuevo WeaponType.
     *
     * @param id             identificador único (snake_case)
     * @param factory        factory que crea WeaponComport
     * @param defaultRarity  rareza por defecto
     * @param displayName    nombre visible
     * @param description    descripción del comportamiento
     */
    public WeaponType(String id,
                      Supplier<WeaponComport> factory,
                      ItemRarity defaultRarity,
                      String displayName,
                      String description) {
        super(id, factory, defaultRarity, displayName, description);
    }

    // ── API pública — CONTRATOS PRESERVADOS ───────────────────────────────

    /**
     * Crea una nueva instancia del WeaponComport asociado a este tipo.
     *
     * <p>Cada arma equipada por el jugador tiene su propia instancia —
     * con su cooldown, ammo y estado de recarga independientes.
     *
     * ✅ CONTRATO PRESERVADO — este método funciona exactamente igual que antes.
     *
     * @return nueva instancia del WeaponComport. Nunca null.
     */
    public WeaponComport createComport() {
        return createInstance();
    }

    // ── Registro y consulta (delegación a infraestructura unificada) ──────

    /**
     * Registra un nuevo tipo de arma.
     *
     * EXTENSIBILIDAD:
     *   Módulos externos pueden registrar sus propios tipos sin modificar
     *   este archivo. Ejemplo:
     *
     *   WeaponType SHOTGUN = WeaponType.register(new WeaponType(
     *       "shotgun", WeaponShotgun::new, ItemRarity.UNCOMMON,
     *       "Escopeta", "Dispara múltiples perdigones..."
     *   ));
     *
     * @param type tipo a registrar
     * @return el mismo tipo (para asignación en constantes)
     * @throws IllegalStateException si el ID ya está registrado
     */
    public static WeaponType register(WeaponType type) {
        return ItemTypeRegistry.register(WeaponType.class, type);
    }

    /**
     * Obtiene un tipo por su ID.
     *
     * @param id identificador del tipo
     * @return el tipo correspondiente
     * @throws IllegalArgumentException si no existe
     */
    public static WeaponType get(String id) {
        return ItemTypeRegistry.get(WeaponType.class, id);
    }

    /**
     * Busca un tipo por su ID sin lanzar excepción.
     *
     * @param id identificador del tipo
     * @return el tipo correspondiente, o null si no existe
     */
    public static WeaponType find(String id) {
        return ItemTypeRegistry.find(WeaponType.class, id);
    }

    /**
     * Verifica si existe un tipo con el ID dado.
     */
    public static boolean has(String id) {
        return ItemTypeRegistry.has(WeaponType.class, id);
    }

    /**
     * Retorna todos los tipos registrados.
     *
     * ✅ CONTRATO PRESERVADO — reemplaza enum.values()
     *
     * @return colección inmutable de todos los tipos
     */
    public static Collection<WeaponType> values() {
        return ItemTypeRegistry.values(WeaponType.class);
    }
}
