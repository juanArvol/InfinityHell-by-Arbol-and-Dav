package Game.World.Chunk;

import Game.Engine.GameObjects;
import Game.World.Core.WorldCoordinator;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contenedor pasivo de objetos estáticos/persistentes de una región espacial.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Un Chunk almacena el contenido generado de una celda de la grilla del mundo:
 * terreno, obstáculos, decoración, loot spawner points, etc.
 *
 * El Chunk no sabe nada sobre:
 *   - Dónde está el Player
 *   - Qué objetos se están simulando
 *   - Física, colisiones, IA
 *   - La cámara
 *   - El tick actual del juego
 *
 * Su única responsabilidad es almacenar objetos con posiciones en
 * COORDENADAS GLOBALES de mundo y proveerlos para consulta.
 *
 * ── COORDENADAS GLOBALES ──────────────────────────────────────────────────
 * Todos los GameObjects dentro de un Chunk tienen posiciones en coordenadas
 * globales del mundo continuo. No existe conversión local → global en runtime.
 *
 * Ejemplo con chunkWidth=1280:
 *   Chunk(0,0): objetos con X ∈ [0, 1280)
 *   Chunk(1,0): objetos con X ∈ [1280, 2560)
 *   Chunk(2,0): objetos con X ∈ [2560, 3840)
 *
 * La conversión local → global ocurre UNA SOLA VEZ durante la generación
 * (en WorldLayer.generate(Chunk, Random)).
 *
 * ── INMUTABILIDAD ESTRUCTURAL ─────────────────────────────────────────────
 * El Chunk puede añadir/remover objetos durante la generación, pero una vez
 * marcado como loaded=true se considera estable. La lista de objetos puede
 * consultarse para simulación y render sin modificarla.
 *
 * ── NO TIENE update() ────────────────────────────────────────────────────
 * El Chunk no implementa ningún loop de simulación. Su contenido es actualizado
 * por los sistemas que consumen sus objetos (SimulationRegion → CollisionsSystem).
 */
public final class Chunk {

    // ── Identidad espacial ────────────────────────────────────────────────

    private final WorldCoordinator coordinator;

    /** X global del borde izquierdo de este chunk (en píxeles). */
    private final int originX;

    /** Y global del borde superior de este chunk (en píxeles). */
    private final int originY;

    private final int width;
    private final int height;

    // ── Contenido ─────────────────────────────────────────────────────────

    /**
     * Objetos estáticos/persistentes de este chunk.
     * Todos tienen posiciones en coordenadas GLOBALES de mundo.
     */
    private final List<GameObjects> objects = new ArrayList<>();

    /** True cuando el chunk fue generado completamente y puede usarse. */
    private volatile boolean loaded = false;

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * @param coordinator identificador de celda en la grilla
     * @param width       ancho del chunk en píxeles globales
     * @param height      alto del chunk en píxeles globales
     */
    public Chunk(WorldCoordinator coordinator, int width, int height) {
        this.coordinator = coordinator;
        this.width       = width;
        this.height      = height;
        this.originX     = GlobalChunkResolver.originX(coordinator, width);
        this.originY     = GlobalChunkResolver.originY(coordinator, height);
    }

    // ── Gestión de contenido (llamado durante generación) ─────────────────

    /**
     * Añade un objeto al contenido del chunk.
     * Llamar únicamente durante la generación (WorldLayer.generate).
     *
     * @param obj el objeto a añadir; debe tener posición en coords globales
     */
    public void add(GameObjects obj) {
        objects.add(obj);
    }

    /**
     * Elimina un objeto del chunk (por ejemplo, al recoger un item).
     *
     * @param obj el objeto a eliminar
     */
    public void remove(GameObjects obj) {
        objects.remove(obj);
    }

    /**
     * Marca el chunk como completamente generado y listo para ser usado.
     * Llamar al final de WorldGenerator.generate().
     */
    public void markLoaded() {
        this.loaded = true;
    }

    // ── Consulta ──────────────────────────────────────────────────────────

    /**
     * Vista de solo lectura de todos los objetos del chunk.
     * Los objetos tienen posiciones en coordenadas globales.
     *
     * @return lista inmutable de objetos
     */
    public List<GameObjects> getObjects() {
        return Collections.unmodifiableList(objects);
    }

    /**
     * Rectángulo que representa los bounds globales de este chunk.
     * Útil para tests de intersección con regiones.
     *
     * @return Rectangle(originX, originY, width, height) en coords globales
     */
    public Rectangle getBoundsGlobal() {
        return new Rectangle(originX, originY, width, height);
    }

    /**
     * True si la posición global (gx, gy) está dentro de los bounds de este chunk.
     *
     * @param gx posición X global
     * @param gy posición Y global
     * @return true si el punto pertenece a este chunk
     */
    public boolean containsGlobalPosition(double gx, double gy) {
        return gx >= originX && gx < (originX + width)
            && gy >= originY && gy < (originY + height);
    }

    // ── Accesores ─────────────────────────────────────────────────────────

    public WorldCoordinator getCoordinator() { return coordinator; }
    public int              getOriginX()     { return originX;     }
    public int              getOriginY()     { return originY;     }
    public int              getWidth()       { return width;       }
    public int              getHeight()      { return height;      }
    public boolean          isLoaded()       { return loaded;      }
    public int              objectCount()    { return objects.size(); }

    @Override
    public String toString() {
        return "Chunk[" + coordinator + " origin=(" + originX + "," + originY + ")"
               + " " + width + "×" + height
               + (loaded ? " LOADED" : " GENERATING")
               + " objects=" + objects.size() + "]";
    }
}
