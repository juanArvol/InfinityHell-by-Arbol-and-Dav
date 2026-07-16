package Sprites.Core;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * SpriteExtractor — punto de extensión oficial del Sprite Module.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Cada implementación transforma una fuente gráfica (BufferedImage) en una
 * colección de SpriteFrame. Nada más.
 *
 * No conoce animaciones, gameplay, entidades, SpriteDrawer ni RenderEngine.
 * Su única salida es una colección consistente de SpriteFrame.
 *
 * ── PRINCIPIO OPEN/CLOSED ─────────────────────────────────────────────────
 * SpriteSheet depende únicamente de esta abstracción.
 * Para incorporar un nuevo formato de recursos basta con implementar esta
 * interfaz y registrar el extractor — sin tocar SpriteSheet, Animation,
 * SpriteDrawer, SpriteRenderer ni RenderEngine.
 *
 * ── EXTRACTORES EXISTENTES ────────────────────────────────────────────────
 *
 *   GridExtractor      → cuadrícula regular (24×24, 32×32, 64×64…)
 *   AutoRegionExtractor → detección automática de regiones no transparentes
 *
 * ── EXTRACTORES FUTUROS (no implementados en este HRFC) ───────────────────
 *
 *   AtlasExtractor     → atlas con coordenadas explícitas
 *   JsonExtractor      → descriptor Aseprite (.json)
 *   XmlExtractor       → descriptor TexturePacker (.xml)
 *   BinaryExtractor    → formato binario propietario
 *   ProceduralExtractor → generación dinámica de frames
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   // Uso indirecto vía SpriteSheet.load():
 *   SpriteSheet sheet = SpriteSheet.load(image, SpriteExtractors.grid(24, 24));
 *
 *   // Uso directo:
 *   SpriteExtractor extractor = SpriteExtractors.autoRegion();
 *   List<SpriteFrame> frames  = extractor.extract(image);
 */
public interface SpriteExtractor {

    /**
     * Extrae los SpriteFrame de la imagen fuente.
     *
     * La implementación decide cómo interpretar los píxeles para producir
     * frames individuales. El resultado debe ser consistente: ningún frame
     * puede contener píxeles de otro frame ni estar incompleto.
     *
     * @param source imagen fuente completa (nunca null — validar antes de llamar)
     * @return lista no-nula de SpriteFrame en orden lógico de reproducción;
     *         puede estar vacía si la imagen no contiene regiones válidas.
     */
    List<SpriteFrame> extract(BufferedImage source);
}
