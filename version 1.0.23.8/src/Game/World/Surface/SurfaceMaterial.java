package Game.World.Surface;

/**
 * Define las propiedades físicas de la superficie de un objeto del mundo.
 *
 * Implementada por BlockWorld, Obstacle, y cualquier objeto pisable.
 * CollisionsSystem la lee en Fase 0 y la asigna a physics.setCurrentSurface().
 * Physics.moveX() la usa en el frame siguiente.
 *
 * ── friction vs drag ────────────────────────────────────────────────────
 *
 *   friction  Escala la aceleración cuando hay INPUT activo.
 *             Controla la tracción: qué tan rápido el objeto puede acelerar.
 *             1.0 = normal. 0.05 = hielo (casi sin tracción). >1 = adherente.
 *
 *   drag      Amortiguación PASIVA de vx cuando NO hay input.
 *             Se aplica multiplicativamente: vx *= drag por frame.
 *             1.0 = sin amortiguación (se desliza para siempre).
 *             0.85 = freno normal de suelo. 0.4 = para casi instantáneo.
 *
 * ── Valores de referencia ────────────────────────────────────────────────
 *
 *   suelo normal  friction=1.0,  drag=0.82   freno limpio, control normal
 *   hielo         friction=0.05, drag=0.99   resbala, casi no para
 *   barro         friction=2.5,  drag=0.55   cuesta arrancar, para rápido
 *   miel          friction=4.0,  drag=0.35   muy viscoso, movimiento lento
 *   aire          friction=0.55, drag=0.96   control reducido, poca resistencia
 *
 * AIR se usa cuando el objeto no tiene suelo debajo.
 * friction<1 en AIR = menos control aéreo que en suelo (realista).
 * drag alto en AIR = poca resistencia del aire (no frena tan rápido al soltar).
 */
public interface SurfaceMaterial {

    double getFriction();
    double getDrag();

    // ── Materiales predefinidos ───────────────────────────────────────────

    SurfaceMaterial DEFAULT = new SurfaceMaterial() {
        public double getFriction() { return 1.0;  }
        public double getDrag()     { return 0.82; }
        public String toString()    { return "DEFAULT"; }
    };

    SurfaceMaterial AIR = new SurfaceMaterial() {
        public double getFriction() { return 0.55; }
        public double getDrag()     { return 0.96; }
        public String toString()    { return "AIR"; }
    };

    SurfaceMaterial ICE = new SurfaceMaterial() {
        public double getFriction() { return 0.05; }
        public double getDrag()     { return 0.99; }
        public String toString()    { return "ICE"; }
    };

    SurfaceMaterial MUD = new SurfaceMaterial() {
        public double getFriction() { return 2.5;  }
        public double getDrag()     { return 0.55; }
        public String toString()    { return "MUD"; }
    };

    SurfaceMaterial HONEY = new SurfaceMaterial() {
        public double getFriction() { return 4.0;  }
        public double getDrag()     { return 0.35; }
        public String toString()    { return "HONEY"; }
    };
}
