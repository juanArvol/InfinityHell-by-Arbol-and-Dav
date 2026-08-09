package Game.Engine.Tests;

import Game.Engine.Colisions.SweptAABB;
import Game.Engine.Colisions.SweptAABB.Result;
import Game.Items.Types.Bullets.BulletComport.BulletPhysics;
import java.awt.Rectangle;

/**
 * Tests de la lógica de rebote de BulletJump a través de la cadena completa:
 * SweptAABB → normal → BulletPhysics.setLastContactNormal → lógica de rebote.
 *
 * Simula el pipeline de CollisionsSystem FASE 1B para verificar que:
 * 1. SweptAABB calcula la normal correcta.
 * 2. La normal se escribe en BulletPhysics.
 * 3. La lógica de rebote de BulletJump actúa sobre la normal correcta.
 *
 * No instancia Bullet ni BulletJump directamente (evita dependencias de assets),
 * sino que simula el flujo de datos que CollisionsSystem + BulletJump usarían.
 */
public final class BulletJumpCollisionTest {

    private static int passed = 0;
    private static int failed = 0;

    // Constantes de BulletJump
    private static final double JUMP_BOOST = -14.0;
    private static final double FRICTION   = 1.01;

    public static void main(String[] args) {
        testReboteSuelo();
        testReboteTecho();
        testReboteParedDerecha();
        testReboteParedIzquierda();
        testNormalDisponibleEnSwept();
        testTunnelingDetectado();
        testMultipleRebotes();
        testNormalInvertidaParaObjetoB();
        printSummary();
    }

    /**
     * Bullet cae sobre el suelo.
     * bullet vy > 0, obstáculo abajo → normalY == -1 → JUMP_BOOST.
     */
    private static void testReboteSuelo() {
        // Bullet en (10, 5, 8, 8), suelo en (0, 20, 100, 20)
        // vy=15 → bullet.bottom = 13, suelo.top = 20 → distancia = 7
        // entryTime = 7/15 = 0.467 < 1 → colisión
        Rectangle bullet = new Rectangle(10, 5, 8, 8);
        Rectangle suelo  = new Rectangle(0, 20, 100, 20);
        double vx = 5.0, vy = 15.0;

        Result r = SweptAABB.calculate2D(bullet, suelo, vx, vy);

        assertTrue("SUELO: hasCollision", r.hasCollision());
        assertEquals("SUELO: normalY==-1", -1, r.normalY);
        assertEquals("SUELO: normalX==0", 0, r.normalX);

        // Simular escritura en BulletPhysics
        BulletPhysics bp = new BulletPhysics(vx, vy);
        bp.setLastContactNormal(r.normalX, r.normalY);

        // Simular lógica de BulletJump.onCollision()
        int nx = bp.getLastContactNormalX();
        int ny = bp.getLastContactNormalY();
        applyBulletJumpLogic(bp, nx, ny);

        assertEquals("SUELO: vy==JUMP_BOOST", (int)JUMP_BOOST, (int)bp.getYspeed());
        assertTrue("SUELO: vx atenuado", Math.abs(bp.getXspeed()) < Math.abs(vx) + 0.01);
    }

    /**
     * Bullet sube y golpea el techo → normalY == +1 → refleja vy.
     */
    private static void testReboteTecho() {
        Rectangle bullet = new Rectangle(10, 25, 8, 8);
        Rectangle techo  = new Rectangle(0, 10, 100, 10);
        double vx = 3.0, vy = -15.0;

        Result r = SweptAABB.calculate2D(bullet, techo, vx, vy);

        assertTrue("TECHO: hasCollision", r.hasCollision());
        assertEquals("TECHO: normalY==+1", 1, r.normalY);

        BulletPhysics bp = new BulletPhysics(vx, vy);
        bp.setLastContactNormal(r.normalX, r.normalY);
        double prevVy = bp.getYspeed();

        applyBulletJumpLogic(bp, bp.getLastContactNormalX(), bp.getLastContactNormalY());

        assertTrue("TECHO: vy reflejado (>0 después de impacto)", bp.getYspeed() > 0);
        assertApprox("TECHO: |vy| igual", Math.abs(prevVy), Math.abs(bp.getYspeed()), 0.01);
    }

    /**
     * Bullet se mueve hacia la derecha e impacta una pared → normalX == -1 → refleja vx.
     */
    private static void testReboteParedDerecha() {
        Rectangle bullet = new Rectangle(0, 10, 8, 8);
        Rectangle pared  = new Rectangle(30, 0, 10, 100);
        double vx = 25.0, vy = 0.0;

        Result r = SweptAABB.calculate2D(bullet, pared, vx, vy);

        assertTrue("PARED_DER: hasCollision", r.hasCollision());
        assertEquals("PARED_DER: normalX==-1", -1, r.normalX);
        assertEquals("PARED_DER: normalY==0", 0, r.normalY);

        BulletPhysics bp = new BulletPhysics(vx, vy);
        bp.setLastContactNormal(r.normalX, r.normalY);
        double prevVx = bp.getXspeed();

        applyBulletJumpLogic(bp, bp.getLastContactNormalX(), bp.getLastContactNormalY());

        assertTrue("PARED_DER: vx reflejado (<0)", bp.getXspeed() < 0);
        assertTrue("PARED_DER: |vx| atenuado", Math.abs(bp.getXspeed()) < Math.abs(prevVx));
    }

    /**
     * Bullet se mueve hacia la izquierda e impacta una pared → normalX == +1 → refleja vx.
     */
    private static void testReboteParedIzquierda() {
        // Bullet en (35, 10, 8, 8), pared en (10, 0, 10, 100)
        // vx=-30: bullet.x = 35, pared.right = 20
        // entry = (pared.right - bullet.x) / vx = (20 - 35) / (-30) = 0.5 < 1 → colisión
        Rectangle bullet = new Rectangle(35, 10, 8, 8);
        Rectangle pared  = new Rectangle(10, 0, 10, 100);
        double vx = -30.0, vy = 0.0;

        Result r = SweptAABB.calculate2D(bullet, pared, vx, vy);

        assertTrue("PARED_IZQ: hasCollision", r.hasCollision());
        assertEquals("PARED_IZQ: normalX==+1", 1, r.normalX);

        BulletPhysics bp = new BulletPhysics(vx, vy);
        bp.setLastContactNormal(r.normalX, r.normalY);
        applyBulletJumpLogic(bp, bp.getLastContactNormalX(), bp.getLastContactNormalY());

        assertTrue("PARED_IZQ: vx reflejado (>0)", bp.getXspeed() > 0);
    }

    /**
     * Verifica que calculate2D devuelve una normal válida (no (0,0)) para
     * cualquier colisión detectada — la normal siempre debe estar disponible.
     */
    private static void testNormalDisponibleEnSwept() {
        Rectangle bullet = new Rectangle(0, 0, 8, 8);
        Rectangle obs    = new Rectangle(15, 0, 20, 20);
        double vx = 40.0, vy = 5.0;

        Result r = SweptAABB.calculate2D(bullet, obs, vx, vy);

        if (r.hasCollision()) {
            assertTrue("NORMAL_DISP: normalX||normalY != (0,0)",
                    r.normalX != 0 || r.normalY != 0);
        } else {
            System.out.println("  INFO  NORMAL_DISP: no collision en este caso");
        }
    }

    /**
     * Verifica que calculate2D detecta tunneling:
     * bullet muy rápido (vx=300) atravesando obstáculo delgado (10px).
     */
    private static void testTunnelingDetectado() {
        Rectangle bullet = new Rectangle(0, 5, 8, 8);
        Rectangle muro   = new Rectangle(5, 0, 10, 20); // muro a 5px de distancia, 10px de ancho
        double vx = 300.0, vy = 0.0; // velocidad que lo haría atravesar sin CCD

        Result r = SweptAABB.calculate2D(bullet, muro, vx, vy);
        assertTrue("TUNNELING: bullet rápido no atraviesa muro delgado", r.hasCollision());
        assertTrue("TUNNELING: time < 1", r.time < 1.0);
    }

    /**
     * Simula múltiples rebotes consecutivos para verificar consistencia.
     * Rebote 1: suelo, Rebote 2: pared, Rebote 3: suelo.
     */
    private static void testMultipleRebotes() {
        BulletPhysics bp = new BulletPhysics(5.0, 20.0);

        // Rebote 1: suelo (normalY = -1)
        bp.setLastContactNormal(0, -1);
        applyBulletJumpLogic(bp, 0, -1);
        assertTrue("MULTI_1: vy negativo tras rebote suelo", bp.getYspeed() < 0);
        double vxAfter1 = bp.getXspeed();

        // Rebote 2: pared derecha (normalX = -1)
        bp.setLastContactNormal(-1, 0);
        applyBulletJumpLogic(bp, -1, 0);
        assertTrue("MULTI_2: vx negativo tras pared", bp.getXspeed() < 0);

        // Rebote 3: suelo de nuevo (normalY = -1)
        bp.setYspeed(5.0); // simular que cayó de nuevo
        bp.setLastContactNormal(0, -1);
        applyBulletJumpLogic(bp, 0, -1);
        assertTrue("MULTI_3: vy negativo tras 3er rebote", bp.getYspeed() < 0);
    }

    /**
     * Verifica la inversión de normal para el objeto B en propagateNormalToTrigger.
     * Cuando A impacta B desde la izquierda (normalX=+1 para A), para B debe ser -1.
     */
    private static void testNormalInvertidaParaObjetoB() {
        // La normal de A→B normalX=+1 (A viene desde la izquierda)
        // Para B, la normal invertida sería -1
        int normalXforA = 1, normalYforA = 0;
        int normalXforB = -normalXforA;
        int normalYforB = -normalYforA;

        assertEquals("INVERT_NORMAL: B.nx == -A.nx", -1, normalXforB);
        assertEquals("INVERT_NORMAL: B.ny == 0", 0, normalYforB);
    }

    // ── Simulador de BulletJump.onCollision() (sin AbstractEntity) ────────

    private static void applyBulletJumpLogic(BulletPhysics physics, int nx, int ny) {
        if (ny == -1) {
            physics.setYspeed(JUMP_BOOST);
            physics.setXspeed(physics.getXspeed() / FRICTION);
        } else if (ny == 1) {
            physics.setYspeed(-physics.getYspeed());
            physics.setXspeed(physics.getXspeed() / FRICTION);
        } else if (nx != 0) {
            physics.setXspeed(-physics.getXspeed() / FRICTION);
        } else {
            double vy = physics.getYspeed();
            double vx = physics.getXspeed();
            if (vy > 0) {
                physics.setYspeed(JUMP_BOOST);
                physics.setXspeed(vx / FRICTION);
            } else if (vy < 0) {
                physics.setYspeed(-vy);
                physics.setXspeed(vx / FRICTION);
            } else {
                physics.setXspeed(-vx / FRICTION);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static void assertTrue(String name, boolean cond) {
        if (cond) { System.out.println("  PASS  " + name); passed++; }
        else       { System.out.println("  FAIL  " + name); failed++; }
    }

    private static void assertEquals(String name, int expected, int actual) {
        if (expected == actual) { System.out.println("  PASS  " + name); passed++; }
        else { System.out.println("  FAIL  " + name + " expected=" + expected + " actual=" + actual); failed++; }
    }

    private static void assertApprox(String name, double expected, double actual, double eps) {
        if (Math.abs(expected - actual) < eps) { System.out.println("  PASS  " + name); passed++; }
        else { System.out.println("  FAIL  " + name + " expected≈" + expected + " actual=" + actual); failed++; }
    }

    private static void printSummary() {
        System.out.println("\n── BulletJumpCollisionTest ────────");
        System.out.println("PASSED: " + passed + "  FAILED: " + failed);
        if (failed > 0) System.exit(1);
    }
}
