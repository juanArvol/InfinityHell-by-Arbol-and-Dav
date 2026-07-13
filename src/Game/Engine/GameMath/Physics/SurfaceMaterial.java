package Game.Engine.GameMath.Physics;

/**
 * Define las propiedades físicas de una superficie.
 *
 * Infraestructura del Engine — no contiene reglas del juego.
 * Physics2D y CollisionsSystem la leen para calcular fricción,
 * drag y control aéreo sin hardcodear valores.
 *
 * ── Propiedades ──────────────────────────────────────────────────────────
 *
 *   friction     Escala la aceleración cuando hay INPUT activo.
 *                1.0 = normal. 0.05 = hielo (casi sin tracción). >1 = pegajoso.
 *
 *   drag         Amortiguación PASIVA de vx cuando NO hay input.
 *                Se aplica multiplicativamente: vx *= drag por frame.
 *                1.0 = sin amortiguación. 0.4 = para casi instantáneo.
 *
 *   airControl   Modificador del control aéreo [0..1].
 *                1.0 = control aéreo completo.
 *                0.8 = valor clásico "en el aire hay menos control".
 *
 *   accelScale   Escala adicional sobre la aceleración base del objeto.
 *                Permite superficies que den más aceleración (hierba = 1.2)
 *                o menos (barro = 0.7) sin tocar aGround.
 *
 * ── Referencia de materiales ─────────────────────────────────────────────
 *
 *   Material    friction  drag   airControl  accelScale
 *   DEFAULT     1.00      0.82   0.80        1.00   → suelo normal
 *   AIR         0.55      0.96   1.00        1.00   → sin suelo
 *   ICE         0.05      0.99   0.80        1.00   → resbala, no para
 *   MUD         2.50      0.55   0.80        0.70   → cuesta arrancar, para rápido
 *   HONEY       4.00      0.35   0.80        0.50   → muy viscoso
 *   BOUNCY      1.00      0.82   1.20        1.00   → trampolín: más control aéreo
 *
 * Para crear un material ad-hoc:
 *   SurfaceMaterial custom = SurfaceMaterial.of(0.3, 0.90, 0.8, 1.0);
 *
 * MIGRADO DESDE: Game.World.Surface.SurfaceMaterial
 * RAZÓN: SurfaceMaterial es infraestructura de física pura. Physics2D,
 * MovementContext y CollisionsSystem (todos en Engine) la usan. Tenerla
 * en Game.World creaba una dependencia invertida: Engine → Game.
 * Al ubicarla en Engine.GameMath.Physics se elimina esa inversión.
 */
public interface SurfaceMaterial {

    /** Tracción activa (con input). */
    double getFriction();

    /** Amortiguación pasiva (sin input). */
    double getDrag();

    /**
     * Modificador de control en el aire.
     * Physics usa esto en lugar del "bonus" hardcodeado (onGround ? 1.0 : 0.8).
     */
    double getAirControl();

    /**
     * Escala adicional sobre aGround/aAir base del objeto.
     * Permite que la superficie module la aceleración sin cambiar los
     * parámetros propios del objeto físico.
     */
    double getAccelScale();

    // ── Factory ───────────────────────────────────────────────────────────

    /** Crea un material personalizado con los cuatro parámetros. */
    static SurfaceMaterial of(double friction, double drag,
                               double airControl, double accelScale) {
        return new SurfaceMaterial() {
            public double getFriction()   { return friction;   }
            public double getDrag()       { return drag;       }
            public double getAirControl() { return airControl; }
            public double getAccelScale() { return accelScale; }
        };
    }

    // ── Materiales predefinidos ───────────────────────────────────────────

    SurfaceMaterial DEFAULT = of(1.00, 0.82, 0.80, 1.00);
    SurfaceMaterial AIR     = of(0.55, 0.96, 1.00, 1.00);
    SurfaceMaterial ICE     = of(0.05, 0.99, 0.80, 1.00);
    SurfaceMaterial MUD     = of(2.50, 0.55, 0.80, 0.70);
    SurfaceMaterial HONEY   = of(4.00, 0.35, 0.80, 0.50);
    SurfaceMaterial BOUNCY  = of(1.00, 0.82, 1.20, 1.00);
}
