package Game.Engine.Entity.Components;

import Game.Engine.Component;
import Game.Engine.Physics.KineticPhysics.SurfaceMaterial;

/**
 * Componente que declara las propiedades de superficie de una entidad.
 *
 * ── HRFC — World Objects extensibles ─────────────────────────────────────
 *
 * PROBLEMA QUE RESUELVE:
 *   CollisionsSystem FASE 0 detectaba propiedades de superficie haciendo
 *   {@code instanceof SurfaceMaterial} directamente sobre el GameObjects.
 *   Eso obligaba a que BlockWorld y Obstacle implementasen la interfaz
 *   como parte de su herencia de clase, acoplando propiedades físicas a
 *   la identidad del tipo.
 *
 *   Consecuencia: ninguna entidad que no fuera BlockWorld/Obstacle podía
 *   declarar propiedades de superficie sin modificar su jerarquía de clases.
 *
 * SOLUCIÓN:
 *   SurfaceComponent envuelve un SurfaceMaterial y actúa como el portador
 *   genérico de esas propiedades. CollisionsSystem FASE 0 ahora consulta:
 *
 *     SurfaceComponent sc = other.getComponent(SurfaceComponent.class);
 *     SurfaceMaterial sm  = (sc != null) ? sc : SurfaceMaterial.DEFAULT;
 *
 *   SurfaceComponent implementa SurfaceMaterial directamente (delegando en
 *   el material interno) para que sea transparente en cualquier contexto
 *   que espere SurfaceMaterial.
 *
 * ── CAPACIDAD GENÉRICA ────────────────────────────────────────────────────
 *
 *   SurfaceComponent no pertenece a World Objects. Cualquier entidad puede
 *   declarar propiedades de superficie:
 *
 *     // Suelo de hielo
 *     worldObject.addComponent(new SurfaceComponent(SurfaceMaterial.ICE));
 *
 *     // Plataforma de barro
 *     worldObject.addComponent(new SurfaceComponent(SurfaceMaterial.MUD));
 *
 *     // Proyectil con superficie resbaladiza (futuro)
 *     bullet.addComponent(new SurfaceComponent(SurfaceMaterial.ICE));
 *
 *     // Surface material personalizado
 *     worldObject.addComponent(new SurfaceComponent(
 *         SurfaceMaterial.of(0.3, 0.90, 0.8, 1.0)));
 *
 * ── RELACIÓN CON MaterialComponent ───────────────────────────────────────
 *
 *   MaterialComponent describe propiedades intrínsecas del material del objeto
 *   (conductividad térmica, elasticidad, dureza...) para simulaciones físicas
 *   avanzadas (térmica, eléctrica, mecánica).
 *
 *   SurfaceComponent describe cómo se comporta la superficie del objeto
 *   al contacto con otros objetos en movimiento (fricción, drag, control
 *   aéreo). Son responsabilidades distintas que pueden coexistir.
 *
 *   Un bloque puede tener ambos:
 *     addComponent(new SurfaceComponent(SurfaceMaterial.ICE));    // fricción/drag
 *     addComponent(new MaterialComponent.Builder()                // térmica/eléctrica
 *         .thermalConductivity(0.9).build());
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 *
 *   El SurfaceMaterial es inyectado en construcción y no cambia en runtime.
 *   Si se necesita cambiar las propiedades de superficie en runtime
 *   (ej: plataforma que se congela), reemplazar el componente o usar
 *   SurfaceMaterial.of() con valores calculados dinámicamente.
 */
public final class SurfaceComponent extends Component implements SurfaceMaterial {

    /** Material que define las propiedades de esta superficie. Nunca null. */
    private final SurfaceMaterial material;

    // ── Constructores ─────────────────────────────────────────────────────

    /**
     * Superficie con material explícito.
     *
     * @param material propiedades de la superficie. No puede ser null.
     */
    public SurfaceComponent(SurfaceMaterial material) {
        if (material == null) throw new IllegalArgumentException("material no puede ser null");
        this.material = material;
    }

    /**
     * Superficie con material por defecto ({@link SurfaceMaterial#DEFAULT}).
     * Equivalente a {@code new SurfaceComponent(SurfaceMaterial.DEFAULT)}.
     */
    public SurfaceComponent() {
        this(SurfaceMaterial.DEFAULT);
    }

    // ── SurfaceMaterial — delegación completa ─────────────────────────────

    /** Tracción activa (con input). */
    @Override public double getFriction()   { return material.getFriction();   }

    /** Amortiguación pasiva (sin input). */
    @Override public double getDrag()       { return material.getDrag();       }

    /** Modificador de control aéreo [0..1]. */
    @Override public double getAirControl() { return material.getAirControl(); }

    /** Escala adicional sobre la aceleración base del objeto. */
    @Override public double getAccelScale() { return material.getAccelScale(); }

    // ── Acceso al material subyacente ─────────────────────────────────────

    /**
     * El SurfaceMaterial subyacente.
     * Útil para comparar con materiales predefinidos o para debugging.
     */
    public SurfaceMaterial getMaterial() { return material; }
}
