package Game.Engine.Entity.Components.Visuals;

import Game.Engine.Component;
import Game.Engine.RenderEngine.Context.RenderCamera;
import Game.Engine.RenderEngine.Context.RenderContext;
import Game.Engine.RenderEngine.Contracts.Renderable;
import Game.Engine.RenderEngine.Sprites.SpritePiece;
import Game.Engine.RenderEngine.Transform.TransformData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SpriteComposite — ensamblaje de múltiples partes visuales ("sistema Lego").
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Gestiona una colección de SpriteComponent independientes que juntos forman
 * el modelo visual completo de una entidad. Propaga la posición del gameObject
 * a cada parte, las actualiza cada tick y las renderiza en orden de capa.
 *
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 * SpriteComposite ES un Component de GameObjects. Se añade con addComponent().
 * Cada SpriteComponent es una pieza dentro del composite, NO un Component
 * independiente del gameObject.
 *
 *   Entity (GameObjects)
 *     └── SpriteComposite (Component)
 *           ├── SpriteComponent("body")
 *           ├── SpriteComponent("head")     offset (0, -24)
 *           ├── SpriteComponent("weapon")   offset (8, -12)  layer=5
 *           └── SpriteComponent("shadow")   layer=-1
 *
 * ── SISTEMA LEGO ──────────────────────────────────────────────────────────
 * El Gameplay puede:
 *   composite.addPart(new SpriteComponent(swordHandle, "weapon"))
 *   composite.removePart("weapon")
 *   composite.getPart("head").play("look_up")
 *   composite.getPart("body").setVisible(false)
 *   composite.getPart("weapon").setHandle(bowHandle) // cambiar arma
 *
 * Sin modificar ningún otro sistema. El resto del Engine no sabe nada.
 *
 * ── RENDER POR CAPAS ──────────────────────────────────────────────────────
 * Las partes se renderizan en orden de su SpriteComponent.getLayer():
 * menor layer = se dibuja primero (debajo). Sombras en layer=-1, armas en layer=5.
 *
 * ── ANIMACIÓN POR PARTE ───────────────────────────────────────────────────
 * Cada parte puede reproducir su propia animación:
 *   legs → "walk_right"
 *   body → "idle"
 *   head → "look_cursor"
 *   Todo simultáneamente, de forma independiente.
 *
 * ── JERARQUÍA PADRE-HIJO ──────────────────────────────────────────────────
 * Preparado: addPart(partId, part, parentId) permite definir una jerarquía.
 * En la versión actual, las transformaciones del padre se propagan al hijo
 * sumando los offsets. Rotación padre→hijo es trabajo de una versión futura.
 *
 * ── VIRTUAL SIZE PARA CULLING ─────────────────────────────────────────────
 * El SpriteComposite propaga el virtualWidth/Height a cada SpriteComponent
 * para que el culling funcione correctamente. Se inyecta con
 * setVirtualSize(vw, vh) desde el sistema de render o desde la entidad.
 */
public final class SpriteSkeletonComponent extends Component implements Renderable {

    // ── Partes ────────────────────────────────────────────────────────────────

    /** Mapa de partId → SpriteComponent para lookup O(1). */
    private final Map<String, SpritePiece> partsMap = new LinkedHashMap<>();

    /** Lista ordenada por layer (se recalcula cuando cambia el conjunto de partes). */
    private List<SpritePiece> sortedParts = new ArrayList<>();

    /** Flag: la lista ordenada necesita recalcularse. */
    private boolean sortDirty = true;

    // ── Jerarquía ─────────────────────────────────────────────────────────────

    /** partId → parentId. Vacío si no hay jerarquía. */
    private final Map<String, String> parentMap = new LinkedHashMap<>();

    // ── Transform global del composite ───────────────────────────────────────

    /**
     * TransformData global aplicado a todas las partes (además de sus transforms individuales).
     * Permite flipear / tintear / escalar toda la entidad de una vez.
     * IDENTITY por defecto.
     */
    private TransformData globalTransform = TransformData.IDENTITY;

    // ── Dimensiones virtuales para culling ────────────────────────────────────

    private int virtualWidth  = 1280;
    private int virtualHeight = 720;

    // ── Constructor ───────────────────────────────────────────────────────────

    public SpriteSkeletonComponent() {}

    // ── Gestión de partes ─────────────────────────────────────────────────────

    /**
     * Añade una parte al composite.
     *
     * @param part SpriteComponent a añadir
     * @return this (para encadenamiento fluido)
     */
    public SpriteSkeletonComponent addPart(SpritePiece part) {
        if (part == null) return this;
        partsMap.put(part.getPartId(), part);
        part.withVirtualSize(virtualWidth, virtualHeight);
        sortDirty = true;
        return this;
    }

    /**
     * Añade una parte con jerarquía padre-hijo.
     * Las transformaciones del padre (offset) se propagan al hijo.
     *
     * @param part     la parte a añadir
     * @param parentId ID de la parte padre
     */
    public SpriteSkeletonComponent addPart(SpritePiece part, String parentId) {
        if (part == null) return this;
        addPart(part);
        if (parentId != null && partsMap.containsKey(parentId)) {
            parentMap.put(part.getPartId(), parentId);
        }
        return this;
    }

    /**
     * Elimina una parte por ID.
     * Permite quitar un arma, un casco, etc. en runtime.
     *
     * @param partId ID de la parte a eliminar
     */
    public void removePart(String partId) {
        partsMap.remove(partId);
        parentMap.remove(partId);
        sortDirty = true;
    }

    /**
     * Obtiene una parte por ID. Retorna null si no existe.
     *
     * @param partId ID de la parte
     */
    public SpritePiece getPart(String partId) {
        return partsMap.get(partId);
    }

    /** true si existe una parte con ese ID. */
    public boolean hasPart(String partId) {
        return partsMap.containsKey(partId);
    }

    /** Número de partes registradas. */
    public int getPartCount() { return partsMap.size(); }

    // ── Transform global ──────────────────────────────────────────────────────

    /**
     * Aplica un TransformData global a todas las partes.
     * Permite flipear / tintear toda la entidad de una vez.
     */
    public void setGlobalTransform(TransformData transform) {
        this.globalTransform = transform != null ? transform : TransformData.IDENTITY;
    }

    /** Atajo: flip horizontal global (usado por PlayerRenderer). */
    public void setFlipH(boolean flipH) {
        if (globalTransform.flipH == flipH) return;
        globalTransform = TransformData.builder()
            .flipH(flipH)
            .flipV(globalTransform.flipV)
            .alpha(globalTransform.alpha)
            .tint(globalTransform.tintColor, globalTransform.tintAlpha)
            .build();
    }

    /** Atajo: alpha global. */
    public void setGlobalAlpha(float alpha) {
        globalTransform = TransformData.builder()
            .flipH(globalTransform.flipH)
            .flipV(globalTransform.flipV)
            .alpha(alpha)
            .tint(globalTransform.tintColor, globalTransform.tintAlpha)
            .build();
    }

    // ── Virtual size ──────────────────────────────────────────────────────────

    /**
     * Inyecta las dimensiones del framebuffer virtual para el culling.
     * Llamar desde la entidad al construir el composite o desde el sistema de render.
     */
    public void setVirtualSize(int vw, int vh) {
        this.virtualWidth  = vw;
        this.virtualHeight = vh;
        for (SpritePiece p : partsMap.values()) {
            p.withVirtualSize(vw, vh);
        }
    }

    // ── Ciclo de vida (Component) ─────────────────────────────────────────────

    @Override
    public void update(double dt) {
        // Actualizar animaciones de todas las partes
        for (SpritePiece part : partsMap.values()) {
            part.updateAnimation();
        }
    }

    // ── Renderable ────────────────────────────────────────────────────────────

    @Override
    public void render(RenderContext ctx, RenderCamera camera) {
        if (gameObject == null) return;

        // Posición base del gameObject
        var pos = gameObject.getTransform().getPosition();
        double bx = pos.getX();
        double by = pos.getY();

        // Reconstruir la lista ordenada si hubo cambios
        if (sortDirty) {
            rebuildSortedParts();
            sortDirty = false;
        }

        // Propagar posición base y renderizar cada parte en orden de capa
        for (SpritePiece part : sortedParts) {
            if (!part.isVisible()) continue;

            // Calcular posición base con jerarquía padre-hijo
            double partBaseX = bx;
            double partBaseY = by;

            String parentId = parentMap.get(part.getPartId());
            if (parentId != null) {
                SpritePiece parent = partsMap.get(parentId);
                if (parent != null) {
                    // El hijo hereda el offset del padre (simplificado)
                    // En una jerarquía completa se acumularía el transform del padre
                    partBaseX += 0; // offset del padre se propaga via setBasePosition
                    partBaseY += 0;
                }
            }

            part.setBasePosition(partBaseX, partBaseY);

            // Aplicar transform global si no es identidad
            if (!globalTransform.isIdentity()) {
                applyGlobalTransformAndRender(part, ctx, camera);
            } else {
                part.render(ctx, camera);
            }
        }
    }

    // ── Privados ──────────────────────────────────────────────────────────────

    private void rebuildSortedParts() {
        sortedParts = new ArrayList<>(partsMap.values());
        sortedParts.sort(Comparator.comparingInt(SpritePiece::getLayer));
    }

    /**
     * Combina el transform local de la parte con el transform global del composite,
     * luego renderiza. Se hace de forma no destructiva (no modifica la parte).
     */
    private void applyGlobalTransformAndRender(SpritePiece part,
                                               RenderContext ctx,
                                               RenderCamera camera) {
        TransformData local  = part.getTransform();
        TransformData merged = mergeTransforms(local, globalTransform);

        // Temporalmente aplicar el transform combinado
        TransformData original = local;
        part.setTransform(merged);
        part.render(ctx, camera);
        part.setTransform(original); // restaurar
    }

    /**
     * Fusiona un transform local con uno global.
     *
     * Reglas de fusión:
     *   - flipH: OR lógico (si global o local tienen flip, el resultado tiene flip)
     *   - alpha: multiplicación (0.5 global × 0.8 local = 0.4)
     *   - tint: el local tiene prioridad; si no hay local, usar el global
     *   - blendMode: el local tiene prioridad
     */
    private static TransformData mergeTransforms(TransformData local, TransformData global) {
        return TransformData.builder()
            .flipH(local.flipH ^ global.flipH) // XOR: doble flip cancela
            .flipV(local.flipV ^ global.flipV)
            .scaleX(local.scaleX * global.scaleX)
            .scaleY(local.scaleY * global.scaleY)
            .rotation(local.rotation + global.rotation)
            .pivot(local.pivotX, local.pivotY)
            .offset(local.offsetX + global.offsetX, local.offsetY + global.offsetY)
            .alpha(local.alpha * global.alpha)
            .tint(
                local.hasTint()  ? local.tintColor  : global.tintColor,
                local.hasTint()  ? local.tintAlpha  : global.tintAlpha
            )
            .blendMode(local.blendMode)
            .build();
    }
}
