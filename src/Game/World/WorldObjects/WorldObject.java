package Game.World.WorldObjects;

import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.Entity.Components.Collisions.MaterialComponent;
import Game.Engine.Entity.Components.HealthComponent;
import Game.Engine.Entity.Components.InteractionSideComponent;
import Game.Engine.Entity.Components.Physics2DComponent;
import Game.Engine.Entity.Components.PushableComponent;
import Game.Engine.Entity.Components.StatusEffectComponent;
import Game.Engine.Entity.Components.SurfaceComponent;
import Game.Engine.Entity.Components.Visuals.HitBoxComponent;
import Game.Engine.Entity.Components.Visuals.SpriteRendererComponent;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.GameObjects;
import Game.Engine.Physics.KineticPhysics.SurfaceMaterial;
import Game.Engine.RenderEngine.Sprites.SizeSyncMode;
import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Base de todos los objetos del mundo.
 *
 * ── HRFC — World Objects extensibles ─────────────────────────────────────
 *
 * PROBLEMA RESUELTO:
 *   BlockWorld y Obstacle eran clases casi idénticas que extendían GameObjects
 *   directamente, saltando AbstractEntity. Eso significaba:
 *
 *     1. No podían usar los shortcuts de entity (damage, isDead, getHealth)
 *        aunque HealthComponent puede añadirse a cualquier GameObjects.
 *     2. SurfaceMaterial estaba implementado en la clase concreta —
 *        una capacidad de infraestructura acoplada a la identidad de tipo.
 *     3. No había un concepto de "World Object" como entidad con capacidades
 *        componibles. Cada nueva variante requería una nueva subclase.
 *
 * SOLUCIÓN:
 *   WorldObject extiende AbstractEntity, conectando los World Objects a
 *   la jerarquía de entidades del Engine sin forzar física en el constructor.
 *
 *   La composición de capacidades se hace via addComponent() post-construcción
 *   o via los métodos de conveniencia del Builder:
 *
 *     WorldObject block = new WorldObject(pos, tex, w, h, CollisionProfile.WORLD);
 *
 *     // Bloque destruible
 *     WorldObject crate = new WorldObject(pos, tex, w, h, CollisionProfile.WORLD_DYNAMIC)
 *         .withHealth(50)
 *         .withPhysics(new Physics2DComponent(cratePhysics))
 *         .withPushable(0.8);
 *
 *     // Plataforma de hielo
 *     WorldObject icePlatform = new WorldObject(pos, tex, w, h, CollisionProfile.WORLD)
 *         .withSurface(SurfaceMaterial.ICE);
 *
 *     // Pared escalable
 *     WorldObject climbWall = new WorldObject(pos, tex, w, h, CollisionProfile.WORLD)
 *         .withSurface(SurfaceMaterial.DEFAULT)
 *         .withInteractions(new InteractionSideComponent()
 *             .setLeft(InteractionSideComponent.SideInteraction.CLIMBABLE)
 *             .setRight(InteractionSideComponent.SideInteraction.CLIMBABLE));
 *
 * ── JERARQUÍA ─────────────────────────────────────────────────────────────
 *
 *   GameObjects → AbstractEntity → WorldObject
 *
 *   WorldObject NO extiende MovingObjects porque MovingObjects hardcodea
 *   Physics2DComponent en el constructor. Los World Objects estáticos no
 *   participan en la simulación física — añadir física opcionalmente es
 *   más correcto que imponerla en la construcción.
 *
 * ── COMPONENTES BASE ──────────────────────────────────────────────────────
 *
 *   Todo WorldObject tiene:
 *     ColliderComponent    → área de colisión y perfil
 *     SpriteRendererComponent → visual (si texture != null)
 *     HitBoxComponent      → debug visual
 *
 *   Capacidades opcionales via métodos fluent o addComponent():
 *     SurfaceComponent     → propiedades de fricción/drag
 *     HealthComponent      → vida y destrucción
 *     Physics2DComponent   → participación en simulación física
 *     PushableComponent    → receptividad a impulsos de contacto
 *     StatusEffectComponent → efectos de estado
 *     InteractionSideComponent → capacidades por cara (preparatorio para Collision HRFC)
 *     MaterialComponent    → propiedades de material para simulaciones físicas avanzadas
 *
 * ── COMPAT CON AbstractEntity ─────────────────────────────────────────────
 *
 *   Al extender AbstractEntity, WorldObject tiene acceso a:
 *     damage(int)          → delega en HealthComponent si existe
 *     heal(int)            → delega en HealthComponent si existe
 *     isDead()             → true si HealthComponent.isDead()
 *     getHealthPercent()   → porcentaje de vida [0.0, 1.0]
 *     addEffect(effect)    → delega en StatusEffectComponent si existe
 *     hasEffect(type)      → consulta tipada
 *
 *   Si el WorldObject no tiene HealthComponent, damage() y isDead() son
 *   no-ops — el objeto es indestructible por diseño.
 *
 * ── CAPACIDADES COMPARTIDAS ────────────────────────────────────────────────
 *
 *   Los componentes añadidos a WorldObject son los mismos del Engine.
 *   No existen versiones especiales para World Objects:
 *
 *     HealthComponent    → mismo que Player, Enemy
 *     Physics2DComponent → mismo que Player, Enemy, Bullet
 *     PushableComponent  → puede usarse en Bullet (GrapplingBullet, futuro)
 *     SurfaceComponent   → puede usarse en cualquier entidad con superficie
 */
public class WorldObject extends GameObjects {

    // ── Constructor principal ─────────────────────────────────────────────

    /**
     * Crea un WorldObject con collider, sprite y debug hitbox.
     *
     * @param position posición en coordenadas del mundo. No puede ser null.
     * @param texture  sprite del objeto. Null = sin render (solo colisión).
     * @param width    ancho del collider en píxeles lógicos. > 0.
     * @param height   alto del collider en píxeles lógicos. > 0.
     * @param profile  perfil de colisión. No puede ser null.
     *                 Usar CollisionProfile.WORLD para terreno estático,
     *                 CollisionProfile.WORLD_DYNAMIC para objetos dinámicos.
     */
    public WorldObject(Vector2D position,
                       BufferedImage texture,
                       int width,
                       int height,
                       CollisionProfile profile) {

        if (position == null) throw new IllegalArgumentException("position no puede ser null");
        if (profile  == null) throw new IllegalArgumentException("profile no puede ser null");
        if (width  <= 0)      throw new IllegalArgumentException("width debe ser > 0");
        if (height <= 0)      throw new IllegalArgumentException("height debe ser > 0");

        getTransform().setPosition(position);

        // ── Collider ──────────────────────────────────────────────────────
        addComponent(new ColliderComponent(width, height, profile));

        // ── Render ────────────────────────────────────────────────────────
        if (texture != null) {
            addComponent(new SpriteRendererComponent(texture, SizeSyncMode.COLLIDER_TO_SPRITE));
        }

        // ── Debug hitbox ──────────────────────────────────────────────────
        addComponent(new HitBoxComponent(Color.BLUE));
    }

    /**
     * Crea un WorldObject con el perfil WORLD por defecto.
     * Equivalente al comportamiento original de BlockWorld para terreno estático.
     *
     * @param position posición en el mundo
     * @param texture  sprite del objeto. Null = sin render.
     * @param width    ancho en píxeles
     * @param height   alto en píxeles
     */
    public WorldObject(Vector2D position,
                       BufferedImage texture,
                       int width,
                       int height) {
        this(position, texture, width, height, CollisionProfile.WORLD);
    }

    // ── API fluent de capacidades opcionales ──────────────────────────────
    //
    // Los métodos "with*()" añaden un componente y retornan this para
    // encadenado. Son azúcar sintáctico sobre addComponent().
    // Pueden usarse en construcción inline:
    //
    //   WorldObject crate = new WorldObject(pos, tex, 40, 40, WORLD_DYNAMIC)
    //       .withHealth(50)
    //       .withSurface(SurfaceMaterial.DEFAULT)
    //       .withPhysics(new Physics2DComponent(cratePhysics))
    //       .withPushable(0.8);

    /**
     * Añade vida/destrucción a este objeto.
     *
     * Usa HealthComponent en modo standalone — no requiere EntityStats.
     * El objeto puede ser dañado vía {@link #damage(int)} o via cualquier
     * sistema que tenga referencia al componente.
     *
     * Hooks disponibles sobreescribiendo HealthComponent:
     *   onDamage(int) → reacción al daño (efectos, sonido)
     *   onDeath()     → destrucción (marcar para eliminación, spawner loot)
     *   onHeal(int)   → curación
     *
     * @param maxHp vida máxima. Debe ser > 0.
     * @return this, para encadenado.
     */
    public WorldObject withHealth(int maxHp) {
        addComponent(new HealthComponent(maxHp));
        return this;
    }

    /**
     * Añade propiedades de superficie al objeto.
     *
     * Permite que CollisionsSystem use las propiedades de fricción/drag
     * de este objeto cuando otro aterriza sobre él, sin que WorldObject
     * implemente SurfaceMaterial directamente.
     *
     * @param material propiedades de superficie. No puede ser null.
     * @return this, para encadenado.
     */
    public WorldObject withSurface(SurfaceMaterial material) {
        addComponent(new SurfaceComponent(material));
        return this;
    }

    /**
     * Añade el SurfaceComponent por defecto ({@link SurfaceMaterial#DEFAULT}).
     *
     * @return this, para encadenado.
     */
    public WorldObject withDefaultSurface() {
        addComponent(new SurfaceComponent());
        return this;
    }

    /**
     * Añade participación en la simulación física del Engine.
     *
     * El componente recibido envuelve la instancia de Physics2D que define
     * el comportamiento físico concreto del objeto (masa, gravedad, aceleración).
     *
     * Objetos estáticos (terreno) NO deben llamar este método.
     * Objetos dinámicos (cajas empujables, plataformas móviles) sí deben hacerlo.
     *
     * @param physicsComponent componente de física 2D. No puede ser null.
     * @return this, para encadenado.
     */
    public WorldObject withPhysics(Physics2DComponent physicsComponent) {
        if (physicsComponent == null)
            throw new IllegalArgumentException("physicsComponent no puede ser null");
        addComponent(physicsComponent);
        return this;
    }

    /**
     * Declara que este objeto puede recibir impulsos de contacto.
     *
     * Normalmente se combina con withPhysics() para que el impulso se
     * transmita a la simulación física. Sin Physics2DComponent, el empuje
     * se recibe pero no produce desplazamiento físico.
     *
     * @param pushReceptivity factor de receptividad al empuje [0.0, 1.0].
     *                        1.0 = absorbe el 100% del impulso. 0.0 = inerte.
     * @return this, para encadenado.
     */
    public WorldObject withPushable(double pushReceptivity) {
        addComponent(new PushableComponent(pushReceptivity));
        return this;
    }

    /**
     * Declara que este objeto puede recibir impulsos de contacto con
     * receptividad completa (1.0).
     *
     * @return this, para encadenado.
     */
    public WorldObject withPushable() {
        addComponent(new PushableComponent());
        return this;
    }

    /**
     * Añade soporte de efectos de estado.
     *
     * Permite que este objeto reciba efectos (veneno, fuego, congelación).
     * Útil para objetos interactivos: una trampa que puede apagarse con agua,
     * un barril que puede incendiarse, etc.
     *
     * @return this, para encadenado.
     */
    public WorldObject withStatusEffects() {
        addComponent(new StatusEffectComponent());
        return this;
    }

    /**
     * Declara las capacidades de interacción por cara del objeto.
     *
     * Preparatorio para el HRFC de Collision: permite que el sistema de
     * colisiones futuro consulte qué puede hacer este objeto desde cada
     * dirección de impacto sin modificar WorldObject.
     *
     * @param sides configuración de interacciones por cara. No puede ser null.
     * @return this, para encadenado.
     */
    public WorldObject withInteractions(InteractionSideComponent sides) {
        if (sides == null) throw new IllegalArgumentException("sides no puede ser null");
        addComponent(sides);
        return this;
    }

    /**
     * Añade propiedades de material para simulaciones físicas avanzadas
     * (térmica, eléctrica, mecánica).
     *
     * Distinto de SurfaceComponent: MaterialComponent describe la naturaleza
     * intrínseca del material (conductividad, dureza, densidad). SurfaceComponent
     * describe cómo se comporta la superficie al contacto en movimiento.
     *
     * @param material propiedades del material. No puede ser null.
     * @return this, para encadenado.
     */
    public WorldObject withMaterial(MaterialComponent material) {
        if (material == null) throw new IllegalArgumentException("material no puede ser null");
        addComponent(material);
        return this;
    }

    // ── Update ────────────────────────────────────────────────────────────

    @Override
    public void update() {
        super.update(); // propaga update() a todos los Component registrados
    }
}
