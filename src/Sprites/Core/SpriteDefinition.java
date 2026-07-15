package Sprites.Core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SpriteDefinition — definición completa de un sprite con sus animaciones.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Contiene todos los datos de un sprite: el frame por defecto (idle/estático)
 * y todas sus animaciones indexadas por clave de string.
 *
 * No sabe cómo dibujar. No sabe de cámaras ni de Display.
 * Solo organiza datos de frames para que el RenderEngine los consuma.
 *
 * ── ORIENTACIONES ─────────────────────────────────────────────────────────
 * Las animaciones se identifican por string libre. La convención recomendada:
 *
 *   "idle"          → frame(s) estáticos
 *   "walk_right"    → caminar derecha
 *   "walk_left"     → caminar izquierda
 *   "walk_up"       → caminar arriba (futuro)
 *   "walk_down"     → caminar abajo  (futuro)
 *   "attack_right"  → ataque derecha (futuro)
 *
 * No hay cantidad fija de orientaciones. El sistema es completamente abierto.
 *
 * ── PREPARACIÓN FUTURA ────────────────────────────────────────────────────
 * La arquitectura permite añadir:
 *   - Múltiples SpriteDefinitions por entidad (ej: por orientación de cámara)
 *   - Metadatos de hitbox relativa al sprite
 *   - Tags de comportamiento (ej: "is_looping", "is_attack")
 *   - Prioridades de animación
 * Sin modificar los consumidores actuales.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   SpriteDefinition def = new SpriteDefinition(idleFrame)
 *       .addAnimation("walk_right", Animation.loop(walkRightFrames, 8))
 *       .addAnimation("walk_left",  Animation.loop(walkLeftFrames,  8));
 *
 *   SpriteHandle handle = new SpriteHandle(def, "player");
 */
public final class SpriteDefinition {

    /** Frame mostrado cuando no hay animación activa. */
    private final SpriteFrame defaultFrame;

    /** Mapa de animaciones por clave. LinkedHashMap para orden de inserción. */
    private final Map<String, Animation> animations;

    // ── Constructor ──────────────────────────────────────────────────────

    /**
     * @param defaultFrame frame por defecto (idle/estático). No puede ser null;
     *                     usar SpriteFrame.empty() si no hay imagen todavía.
     */
    public SpriteDefinition(SpriteFrame defaultFrame) {
        this.defaultFrame = defaultFrame != null ? defaultFrame : SpriteFrame.empty();
        this.animations   = new LinkedHashMap<>();
    }

    // ── Builder fluido ────────────────────────────────────────────────────

    /**
     * Añade una animación a la definición.
     * Retorna this para encadenamiento fluido.
     *
     * @param key       clave de la animación (ej: "walk_right", "idle")
     * @param animation animación a registrar
     * @return this (para encadenamiento)
     */
    public SpriteDefinition addAnimation(String key, Animation animation) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("SpriteDefinition: la clave de animación no puede ser vacía");
        }
        if (animation == null) {
            throw new IllegalArgumentException("SpriteDefinition: la animación no puede ser null");
        }
        animations.put(key, animation);
        return this;
    }

    // ── Consulta ──────────────────────────────────────────────────────────

    /**
     * Frame por defecto del sprite (primer frame de idle o frame estático).
     * Nunca null.
     */
    public SpriteFrame getDefaultFrame() { return defaultFrame; }

    /**
     * Obtiene una animación por clave.
     *
     * @param key clave de la animación
     * @return Animation o null si no existe
     */
    public Animation getAnimation(String key) {
        return animations.get(key);
    }

    /**
     * Verifica si existe una animación con esa clave.
     */
    public boolean hasAnimation(String key) {
        return animations.containsKey(key);
    }

    /**
     * true si el sprite tiene al menos un frame válido (default o en alguna animación).
     */
    public boolean hasFrames() {
        return defaultFrame.isValid() || !animations.isEmpty();
    }

    /** Mapa de animaciones (inmutable). */
    public Map<String, Animation> getAnimations() {
        return Collections.unmodifiableMap(animations);
    }

    @Override
    public String toString() {
        return "SpriteDefinition[default=" + defaultFrame
               + ", animations=" + animations.keySet() + "]";
    }
}
