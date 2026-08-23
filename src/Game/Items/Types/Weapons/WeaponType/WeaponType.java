package Game.Items.Types.Weapons.WeaponType;

import Game.Items.Creation.ItemRarity;
import Game.Items.Types.Weapons.WeaponType.WeaponClass.WeaponEscopeta;
import Game.Items.Types.Weapons.WeaponType.WeaponClass.WeaponPistola;
import java.util.*;
import java.util.function.Supplier;

/**
 * Tipos de arma — el paradigma declarativo equivalente a BulletType.
 *
 * ── REFACTOR — Extensibilidad sin romper contratos ───────────────────────
 *
 * CAMBIO ARQUITECTÓNICO:
 *   - WeaponType ya NO es enum (conjunto cerrado)
 *   - WeaponType es ahora una class con registro estático
 *   - Permite añadir nuevos tipos sin modificar este archivo
 *
 * CONTRATOS PRESERVADOS:
 *   ✅ weaponType.createComport() — sigue funcionando exactamente igual
 *   ✅ weaponType.defaultRarity — sigue siendo public final
 *   ✅ weaponType.displayName — sigue siendo public final
 *   ✅ weaponType.description — sigue siendo public final
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
 * AHORA (class extensible):
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
 * @see Game.Items.Types.Bullets.Definition.BulletType  el patrón que este tipo replica
 * @see Game.Items.Types.Weapons.WeaponRegistry         registro de loot complementario
 */
public final class WeaponType {

    // ── Registro estático ─────────────────────────────────────────────────

    /** Registro de todos los tipos de arma (orden de registro preservado). */
    private static final Map<String, WeaponType> REGISTRY = new LinkedHashMap<>();

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

    // ── Identidad del tipo ────────────────────────────────────────────────

    /** ID único del tipo (snake_case). Inmutable. */
    public final String id;

    // ── Comportamiento del tipo ───────────────────────────────────────────

    /** Factory que crea WeaponComport. Inmutable. */
    private final Supplier<WeaponComport> factory;

    // ── Metadata del tipo ─────────────────────────────────────────────────

    /** Rareza por defecto. Puede sobreescribirse desde configuración externa. */
    public final ItemRarity defaultRarity;

    /** Nombre visible al jugador en UI de recompensa/tienda. */
    public final String displayName;

    /** Descripción del comportamiento para la UI de selección. */
    public final String description;

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
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("id no puede estar vacío");
        if (factory == null)
            throw new IllegalArgumentException("factory no puede ser null");
        if (defaultRarity == null)
            throw new IllegalArgumentException("defaultRarity no puede ser null");

        this.id            = id;
        this.factory       = factory;
        this.defaultRarity = defaultRarity;
        this.displayName   = displayName != null ? displayName : id;
        this.description   = description != null ? description : "";
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
        return factory.get();
    }

    // ── Registro y consulta ───────────────────────────────────────────────

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
        if (type == null)
            throw new IllegalArgumentException("type no puede ser null");
        if (REGISTRY.containsKey(type.id))
            throw new IllegalStateException("WeaponType duplicado: '" + type.id + "'");
        
        REGISTRY.put(type.id, type);
        return type;
    }

    /**
     * Obtiene un tipo por su ID.
     *
     * @param id identificador del tipo
     * @return el tipo correspondiente
     * @throws IllegalArgumentException si no existe
     */
    public static WeaponType get(String id) {
        WeaponType type = REGISTRY.get(id);
        if (type == null)
            throw new IllegalArgumentException("WeaponType no encontrado: '" + id + "'");
        return type;
    }

    /**
     * Busca un tipo por su ID sin lanzar excepción.
     *
     * @param id identificador del tipo
     * @return el tipo correspondiente, o null si no existe
     */
    public static WeaponType find(String id) {
        return REGISTRY.get(id);
    }

    /**
     * Verifica si existe un tipo con el ID dado.
     */
    public static boolean has(String id) {
        return REGISTRY.containsKey(id);
    }

    /**
     * Retorna todos los tipos registrados.
     *
     * ✅ CONTRATO PRESERVADO — reemplaza enum.values()
     *
     * @return colección inmutable de todos los tipos
     */
    public static Collection<WeaponType> values() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    // ── Object identity ───────────────────────────────────────────────────

    @Override
    public String toString() {
        return "WeaponType{id='" + id + "', rarity=" + defaultRarity + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WeaponType)) return false;
        WeaponType that = (WeaponType) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
