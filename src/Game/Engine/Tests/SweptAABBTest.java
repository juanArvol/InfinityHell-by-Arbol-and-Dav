package Game.Engine.Tests;

import Game.Engine.Colisions.SweptAABB;
import Game.Engine.Colisions.SweptAABB.Result;
import java.awt.Rectangle;

/**
 * Tests del motor de SweptAABB.
 *
 * Cubre calculate() (por eje, para sólidos) y calculate2D() (2D completo, para triggers).
 * Se ejecutan con main() — sin dependencia de JUnit para no requerir classpath externo.
 *
 * Para ejecutar: javac + java Game.Engine.Tests.SweptAABBTest
 */
public final class SweptAABBTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testCalculatePerEje();
        testCalculate2D();
        testSweptBounds();
        printSummary();
    }

    // ── Tests de calculate() — por eje (sólidos) ─────────────────────────

    private static void testCalculatePerEje() {
        // Muro a la derecha: moving en (0,0,10,10), target en (20,0,10,10)
        // bullet vx=+30, debe impactar: entry = (20-10)/30 = 0.333
        Result r = SweptAABB.calculate(new Rectangle(0, 0, 10, 10),
                                        new Rectangle(20, 0, 10, 10),
                                        30.0, 0.0);
        assertTrue("SOLID_HIT_RIGHT: hasCollision", r.hasCollision());
        assertEquals("SOLID_HIT_RIGHT: normalX", -1, r.normalX);
        assertEquals("SOLID_HIT_RIGHT: normalY", 0, r.normalY);
        assertApprox("SOLID_HIT_RIGHT: time", 10.0 / 30.0, r.time);

        // Muro a la izquierda: moving en (30,0,10,10), target en (10,0,10,10)
        // bullet vx=-20, entry = (10+10-30)/(-20) = -10/-20 = 0.5
        Result r2 = SweptAABB.calculate(new Rectangle(30, 0, 10, 10),
                                         new Rectangle(10, 0, 10, 10),
                                         -20.0, 0.0);
        assertTrue("SOLID_HIT_LEFT: hasCollision", r2.hasCollision());
        assertEquals("SOLID_HIT_LEFT: normalX", 1, r2.normalX);

        // Sin colisión en Y: pero calculate() con vy=0 solo considera eje X activo.
        // El filtrado de banda en Y es responsabilidad de CollisionsSystem, no de calculate().
        // Verificar que un objeto completamente a la izquierda (fuera de rango X) no colisiona:
        Result r3 = SweptAABB.calculate(new Rectangle(0, 0, 10, 10),
                                         new Rectangle(200, 50, 10, 10),  // mucho más lejos en X que vx
                                         30.0, 0.0);
        assertFalse("SOLID_MISS_X_FAR: no collision (target más lejos que vx)", r3.hasCollision());

        // Penetración preexistente: ya solapados, velocidad positiva
        Result r4 = SweptAABB.calculate(new Rectangle(18, 0, 10, 10),
                                         new Rectangle(20, 0, 10, 10),
                                         5.0, 0.0);
        assertTrue("SOLID_PREEXISTING: hasCollision", r4.hasCollision());
        assertEquals("SOLID_PREEXISTING: time==0", 0, Double.compare(r4.time, 0.0));

        // Suelo: moving (0,0,10,10), target (0,20,10,10), vy=+30
        Result r5 = SweptAABB.calculate(new Rectangle(0, 0, 10, 10),
                                         new Rectangle(0, 20, 10, 10),
                                         0.0, 30.0);
        assertTrue("SOLID_HIT_FLOOR: hasCollision", r5.hasCollision());
        assertEquals("SOLID_HIT_FLOOR: normalY", -1, r5.normalY);
        assertEquals("SOLID_HIT_FLOOR: normalX", 0, r5.normalX);

        // Techo: moving (0,30,10,10), target (0,10,10,10), vy=-25
        Result r6 = SweptAABB.calculate(new Rectangle(0, 30, 10, 10),
                                         new Rectangle(0, 10, 10, 10),
                                         0.0, -25.0);
        assertTrue("SOLID_HIT_CEIL: hasCollision", r6.hasCollision());
        assertEquals("SOLID_HIT_CEIL: normalY", 1, r6.normalY);
    }

    // ── Tests de calculate2D() — 2D simultáneo (triggers/bullets) ─────────

    private static void testCalculate2D() {
        // Muro frontal, movimiento solo horizontal
        Result r = SweptAABB.calculate2D(new Rectangle(0, 0, 10, 10),
                                          new Rectangle(20, 0, 10, 10),
                                          30.0, 0.0);
        assertTrue("TRIGGER_HIT_WALL_X: hasCollision", r.hasCollision());
        assertEquals("TRIGGER_HIT_WALL_X: normalX", -1, r.normalX);
        assertEquals("TRIGGER_HIT_WALL_X: normalY", 0, r.normalY);

        // Suelo, movimiento solo vertical
        Result r2 = SweptAABB.calculate2D(new Rectangle(0, 0, 10, 10),
                                           new Rectangle(0, 20, 10, 10),
                                           0.0, 30.0);
        assertTrue("TRIGGER_HIT_FLOOR: hasCollision", r2.hasCollision());
        assertEquals("TRIGGER_HIT_FLOOR: normalY", -1, r2.normalY);
        assertEquals("TRIGGER_HIT_FLOOR: normalX", 0, r2.normalX);

        // Techo, movimiento vertical negativo
        Result r3 = SweptAABB.calculate2D(new Rectangle(0, 30, 10, 10),
                                           new Rectangle(0, 10, 10, 10),
                                           0.0, -30.0);
        assertTrue("TRIGGER_HIT_CEIL: hasCollision", r3.hasCollision());
        assertEquals("TRIGGER_HIT_CEIL: normalY", 1, r3.normalY);

        // Movimiento diagonal: más rápido en X que en Y → cara X impactada
        // moving (0,0,10,10), target (30,0,10,10), vx=40, vy=5
        // txEntry=(30-10)/40=0.5, tyEntry nunca entra => txEntry domina
        Result r4 = SweptAABB.calculate2D(new Rectangle(0, 0, 10, 10),
                                           new Rectangle(30, 0, 10, 10),
                                           40.0, 5.0);
        assertTrue("TRIGGER_DIAGONAL_X: hasCollision", r4.hasCollision());
        assertEquals("TRIGGER_DIAGONAL_X: normalX", -1, r4.normalX);

        // Movimiento diagonal: más rápido en Y → cara Y impactada
        // moving (0,0,10,10), target (0,40,10,10), vx=5, vy=60
        // tyEntry=(40-10)/60=0.5, txEntry nunca domina
        Result r5 = SweptAABB.calculate2D(new Rectangle(0, 0, 10, 10),
                                           new Rectangle(0, 40, 10, 10),
                                           5.0, 60.0);
        assertTrue("TRIGGER_DIAGONAL_Y: hasCollision", r5.hasCollision());
        assertEquals("TRIGGER_DIAGONAL_Y: normalY", -1, r5.normalY);

        // Tunneling: bullet vx=200, obstáculo de 10px a distancia de 5px
        // Sin CCD esto se perdería; con calculate2D debe detectarse
        Result r6 = SweptAABB.calculate2D(new Rectangle(0, 0, 8, 8),
                                           new Rectangle(5, 0, 10, 10),
                                           200.0, 0.0);
        assertTrue("TRIGGER_TUNNELING: CCD detecta colision", r6.hasCollision());

        // Miss total: moving y target separados en Y, sin posibilidad de overlap
        Result r7 = SweptAABB.calculate2D(new Rectangle(0, 0, 8, 8),
                                           new Rectangle(20, 100, 10, 10),
                                           30.0, 0.0);
        assertFalse("TRIGGER_MISS_Y_GAP: no collision", r7.hasCollision());

        // Colisión exacta en borde: moving en (0,0,10,10), target en (10,0,10,10), vx=0.1
        // entry=0, barely touching
        Result r8 = SweptAABB.calculate2D(new Rectangle(0, 0, 10, 10),
                                           new Rectangle(10, 0, 10, 10),
                                           0.1, 0.0);
        assertTrue("TRIGGER_EDGE_TOUCH: hasCollision at t~0", r8.hasCollision());
    }

    // ── Tests de sweptBounds() ────────────────────────────────────────────

    private static void testSweptBounds() {
        // Movimiento hacia la derecha
        Rectangle sb = SweptAABB.sweptBounds(new Rectangle(10, 10, 8, 8), 30.0, 0.0);
        assertTrue("SWEPT_BOUNDS_RIGHT: x <= 10", sb.x <= 10);
        assertTrue("SWEPT_BOUNDS_RIGHT: width >= 38", sb.width >= 38);

        // Movimiento hacia la izquierda
        Rectangle sb2 = SweptAABB.sweptBounds(new Rectangle(50, 10, 8, 8), -20.0, 0.0);
        assertTrue("SWEPT_BOUNDS_LEFT: x <= 30", sb2.x <= 30);

        // Movimiento diagonal
        Rectangle sb3 = SweptAABB.sweptBounds(new Rectangle(0, 0, 8, 8), 15.0, 10.0);
        assertTrue("SWEPT_BOUNDS_DIAG: width >= 23", sb3.width >= 23);
        assertTrue("SWEPT_BOUNDS_DIAG: height >= 18", sb3.height >= 18);
    }

    // ── Helpers de assertion ──────────────────────────────────────────────

    private static void assertTrue(String name, boolean cond) {
        if (cond) { System.out.println("  PASS  " + name); passed++; }
        else       { System.out.println("  FAIL  " + name + " (expected true)"); failed++; }
    }

    private static void assertFalse(String name, boolean cond) {
        if (!cond) { System.out.println("  PASS  " + name); passed++; }
        else        { System.out.println("  FAIL  " + name + " (expected false)"); failed++; }
    }

    private static void assertEquals(String name, int expected, int actual) {
        if (expected == actual) { System.out.println("  PASS  " + name); passed++; }
        else { System.out.println("  FAIL  " + name + " expected=" + expected + " actual=" + actual); failed++; }
    }

    private static void assertApprox(String name, double expected, double actual) {
        if (Math.abs(expected - actual) < 1e-6) { System.out.println("  PASS  " + name); passed++; }
        else { System.out.println("  FAIL  " + name + " expected≈" + expected + " actual=" + actual); failed++; }
    }

    private static void printSummary() {
        System.out.println("\n── SweptAABBTest ──────────────────");
        System.out.println("PASSED: " + passed + "  FAILED: " + failed);
        if (failed > 0) System.exit(1);
    }
}
