package Game.Items.Core;

import java.util.function.Supplier;

import Game.Items.Creation.ItemDefinition;
import Game.Items.Creation.ItemID;
import Game.Items.Creation.ItemRarity;

/**
 * Contenedor esqueleto para tipos de Items — almacena ItemDefinition + Factory.
 *
 * ── ARQUITECTURA FINAL — Items Module ────────────────────────────────────
 *
 * JERARQUÍA:
 *   ItemDefinition
 *     │
 *     ├── ItemID
 *     └── VisualDefinition
 *
 *   ObjectType<T>
 *     │
 *     ├── ItemDefinition
 *     └── Supplier<T>
 *
 * SEPARACIÓN:
 *   ObjectType        → Contenedor (definition + factory)
 *   ObjectTypeFactory → Lógica (register, get, find, has, values, buildOfferPool)
 *
 * PROPÓSITO:
 *   ObjectType es un simple contenedor inmutable que las clases concretas
 *   (BulletType, WeaponType, AmuletType) extienden para almacenar:
 *     - ItemDefinition (identidad tipada + visual)
 *     - Factory para crear instancias de comportamiento
 *
 * RESPONSABILIDAD:
 *   Solo almacenamiento y getters básicos. TODA la lógica está en ObjectTypeFactory.
 *
 * @param <T> tipo de instancia que este ObjectType puede crear
 */
public abstract class ObjectType<T> {

    /** Definición completa del Item (ID + visual). */
    private final ItemDefinition definition;

    /** Factory que crea instancias de T. */
    private final Supplier<T> factory;

    /**
     * Constructor protegido — solo subclases concretas pueden instanciar.
     *
     * @param definition definición del Item (ID + visual)
     * @param factory    factory para crear instancias de T
     * @throws IllegalArgumentException si definition o factory son null
     */
    protected ObjectType(ItemDefinition definition, Supplier<T> factory) {
        if (definition == null)
            throw new IllegalArgumentException("definition no puede ser null");
        if (factory == null)
            throw new IllegalArgumentException("factory no puede ser null");

        this.definition = definition;
        this.factory    = factory;
    }

    // ── Getters básicos ───────────────────────────────────────────────────

    public ItemDefinition getDefinition() {
        return definition;
    }

    public ItemID getId() {
        return definition.getItemId();
    }

    public String getDisplayName() {
        return definition.getDisplayName();
    }

    public String getDescription() {
        return definition.getDescription();
    }

    public ItemRarity getRarity() {
        return definition.getRarity();
    }

    // ── Creación de instancias ────────────────────────────────────────────

    /**
     * Crea una nueva instancia de T.
     * Cada invocación produce una instancia independiente con su propio estado.
     *
     * @return nueva instancia de T. Nunca null.
     */
    public T createInstance() {
        return factory.get();
    }

    // ── Object identity ───────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectType<?> that = (ObjectType<?>) o;
        return definition.equals(that.definition);
    }

    @Override
    public int hashCode() {
        return definition.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id='" + getId() + "', name='" + getDisplayName() + "'}";
    }
}
