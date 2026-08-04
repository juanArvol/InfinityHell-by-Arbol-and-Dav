package Game.Engine.GameMath.Logic3D;

import Game.Engine.GameMath.Logic2D.Transform2D;

/**
 * Transform extendido con coordenada Z para soporte de sistema 2.5D.
 *
 * NUEVO SISTEMA: permite que cualquier GameObjects tenga altura.
 * Z=0 = en el suelo (comportamiento 2D idéntico al original).
 * Z>0 = elevado (enemigos voladores, proyectiles aéreos, plataformas).
 *
 * Retro-compatible: el Transform original sigue funcionando igual
 * para todo el código existente. Este es un REEMPLAZO opcional.
 *
 * Uso en EnemyFlying:
 *   transform3D.setZ(80); // el volador vuela a 80 unidades de altura
 *
 * Uso en render:
 *   int screenY = (int)(transform3D.getScreenY(0.5) - camera.getY());
 *
 * Uso en depth sort:
 *   objects.sort(Comparator.comparingDouble(obj -> obj.getTransform3D().getDepthSortValue()));
 */
public class Transform3D extends Transform2D {

    private double z = 0; // altura sobre el suelo
    private double velocityZ = 0; // velocidad vertical (para arcos en Z)

    public Transform3D() {}

    // ==================== COORDENADA Z ====================

    public double getZ() { return z; }

    public void setZ(double z) { this.z = z; }

    public void addZ(double dz) { this.z += dz; }

    public double getVelocityZ() { return velocityZ; }
    public void setVelocityZ(double vz) { this.velocityZ = vz; }

    // ==================== POSICIÓN COMPLETA 3D ====================

    public Vector3D getPosition3D() {
        return new Vector3D(getX(), getY(), z);
    }

    public void setPosition3D(Vector3D pos) {
        setX(pos.getX());
        setY(pos.getY());
        this.z = pos.getZ();
    }

    // ==================== RENDER 2.5D ====================

    /**
     * Y de pantalla ajustada por perspectiva.
     * Objetos más altos (Z mayor) aparecen visualmente más arriba.
     *
     * @param perspectiveFactor 0.5 es un valor suave y realista para top-down
     */
    public double getScreenY(double perspectiveFactor) {
        return getY() - z * perspectiveFactor;
    }

    /**
     * Valor de profundidad para depth sorting.
     * Se usa en DepthSortedRenderSystem para determinar el orden de dibujo.
     * Mayor valor = se dibuja encima (más "adelante" en la escena).
     */
    public double getDepthSortValue() {
        return getY() + z * 0.5;
    }

    // ==================== GROUND CHECK ====================

    /**
     * @return true si el objeto está en el suelo (Z <= 0)
     */
    public boolean isOnGround() {
        return z <= 0;
    }

    /**
     * Aplica gravedad en Z si el objeto está en el aire.
     * Para objetos que pueden "caer" sobre el plano (balas con arco, granadas).
     *
     * @param gravity factor de gravedad (positivo = cae hacia Z=0)
     */
    public void applyGravityZ(double gravity) {
        if (!isOnGround()) {
            velocityZ -= gravity;
            z += velocityZ;
            if (z < 0) {
                z = 0;
                velocityZ = 0;
            }
        }
    }
}
