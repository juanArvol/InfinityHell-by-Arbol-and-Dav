package Game.Items.Core;

import Game.Items.ItemRarity;
import java.util.function.Supplier;

/**
 * Clase base abstracta para todos los tipos de Item del sistema.
 *
 * ── HRFC — Items Module Architectural Consolidation ──────────────────────
 *
 * RESPONSABILIDAD:
 *   ItemTypeBase representa el TIPO de Item disponible en el catálogo del juego.
 *   Proporciona infraestructura común para identidad, metadata y creación.
 *
 * SEPARACIÓN DE CONCEPTOS:
 *
 *   ItemTypeBase<T>
 *     ↓
 *   Definition
 *     ↓
 *   Instance
 *
 *   - ItemTypeBase: identidad del tipo + capacidad de creación
 *   - Definition: plantilla estática/documental del Item
 *   - Instance: objeto concreto existente en el mundo/inventario
 *
 * CONTRATO:
 *   Cada subclase concreta (BulletType, WeaponType, AmuletType) debe:
 *     1. Declarar sus tipos mediante constantes static final
 *     2. Registrar cada tipo en su bloque static usando register()
 *     3. Proporcionar una factory para crear instancias de T
 *
 * EJEMPLO DE USO (en subclase):
 *
 *   public final class BulletType extends ItemTypeBase<BulletBehavior> {
 *       public static final BulletType NORMALBULLET;
 *
 *       static {
 *           NORMALBULLET = register(new BulletType(
 *               "normal_bullet",
 *               BulletNormal::new,
 *               ItemRarity.COMMON,
 *               "Bala",
 *               "Fragmento de energía pura"
 *           ));
 *       }
 *
 *       private static BulletType register(BulletType type) {
 *           return ItemTypeRegistry.register(BulletType.class, type);
 *       }
 *   }
 *
 * @param <T> tipo de instancia que este ItemType puede crear
 *
 * @see ItemTypeRegistry  registro unificado de todos los tipos
 * @see Game.Items.ItemDefinition  plantilla estática del Item
 */
public abstract class ObjectType<T> {

    // ── Identidad del tipo ────────────────────────────────────────────────

    /** ID único del tipo (snake_case). Inmutable. */
    public final String id;

    // ── Metadata del tipo ─────────────────────────────────────────────────

    /** Rareza por defecto. Puede sobreescribirse desde configuración externa. */
    public final ItemRarity defaultRarity;

    /** Nombre visible al jugador en UI. */
    public final String displayName;

    /** Descripción del comportamiento/efecto para UI de selección. */
    public final String description;

    // ── Comportamiento del tipo ───────────────────────────────────────────

    /** Factory que crea instancias de T. Inmutable. */
    private final Supplier<T> factory;

    // ── Constructor protegido ─────────────────────────────────────────────

    /**
     * Construye un nuevo ItemTypeBase.
     *
     * Este constructor es protegido — solo subclases concretas pueden invocarlo.
     * Las subclases deben proporcionar validación adicional según sus necesidades.
     *
     * @param id             identificador único (snake_case)
     * @param factory        factory que crea instancias de T
     * @param defaultRarity  rareza por defecto
     * @param displayName    nombre visible
     * @param description    descripción del efecto/comportamiento
     * @throws IllegalArgumentException si algún parámetro obligatorio es inválido
     */
    protected ObjectType(String id,
                          Supplier<T> factory,
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

    // ── API pública ───────────────────────────────────────────────────────

    /**
     * Crea una nueva instancia de T.
     *
     * Cada invocación produce una instancia independiente con su propio estado.
     * Las subclases pueden exponer este método con un nombre más específico
     * (createBehavior(), createComport(), etc.) para mayor claridad.
     *
     * @return nueva instancia de T. Nunca null.
     */
    public T createInstance() {
        return factory.get();
    }

    // ── Object identity ───────────────────────────────────────────────────

    /**
     * La identidad del tipo está definida únicamente por su ID.
     * Dos tipos con el mismo ID son considerados iguales.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectType<?> that = (ObjectType<?>) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id='" + id + "', rarity=" + defaultRarity + "}";
    }
}
