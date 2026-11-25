package Display.Settings;

import Display.State.DisplayMode;
import Display.State.Resolution;
import java.util.ArrayList;
import java.util.List;

/**
 * ResolutionManager — gestiona resoluciones compartidas para fullscreen y windowed.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Mantiene un único conjunto de resoluciones disponibles para ambos modos:
 *   - Resoluciones: 640x360, 960x540, 1280x720, 1920x1080, 2560x1440, 3840x2160
 *   - Tanto fullscreen como windowed pueden usar cualquiera de estas resoluciones
 *
 * Cada modo (fullscreen/windowed) recuerda su última resolución aplicada.
 * Al cambiar de modo, la ventana se abre en la resolución que tenía ese modo.
 *
 * ── COMPORTAMIENTO ────────────────────────────────────────────────────────
 * 
 * Escenario de uso:
 * 1. Usuario inicia en fullscreen 1920x1080
 * 2. Usuario en settings cambia a 640x360 en fullscreen (ahora es posible!)
 * 3. Usuario presiona F11 → modo windowed se abre en su última resolución
 * 4. Usuario redimensiona ventana manualmente → resolución windowed cambia dinámicamente
 * 5. Usuario presiona F11 → fullscreen vuelve a 640x360 (resolución guardada)
 *
 * ── RESIZE DINÁMICO ───────────────────────────────────────────────────────
 * En modo windowed, si el usuario redimensiona la ventana manualmente (drag),
 * la resolución windowed se actualiza automáticamente. NO se considera una
 * de las resoluciones predefinidas, pero se guarda como "última resolución windowed".
 *
 * ── THREADING ─────────────────────────────────────────────────────────────
 * Thread-safe: métodos sincronizados para acceso desde EDT y GameLoop.
 */
public final class ResolutionManager {

    // ── Resoluciones predefinidas ─────────────────────────────────────────
    // Ahora fullscreen y windowed comparten las MISMAS resoluciones disponibles
    // para que no haya limitaciones arbitrarias en fullscreen

    private static final List<Resolution> AVAILABLE_RESOLUTIONS = List.of(
        new Resolution(640, 360),     // Pequeña
        new Resolution(960, 540),     // Media
        new Resolution(1280, 720),    // 720p HD
        new Resolution(1920, 1080),   // 1080p Full HD
        new Resolution(2560, 1440),   // 1440p QHD
        new Resolution(3840, 2160)    // 4K UHD
    );

    // ── Estado actual ─────────────────────────────────────────────────────

    private Resolution currentFullscreenResolution;
    private Resolution currentWindowedResolution;
    
    private DisplayMode currentMode = DisplayMode.WINDOWED;

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * Crea el ResolutionManager con resoluciones iniciales.
     *
     * @param initialFullscreenRes resolución inicial para fullscreen
     * @param initialWindowedRes resolución inicial para windowed
     */
    public ResolutionManager(Resolution initialFullscreenRes, Resolution initialWindowedRes) {
        this.currentFullscreenResolution = findClosestOrDefault(
            initialFullscreenRes, AVAILABLE_RESOLUTIONS, AVAILABLE_RESOLUTIONS.get(2) // 720p default
        );
        this.currentWindowedResolution = findClosestOrDefault(
            initialWindowedRes, AVAILABLE_RESOLUTIONS, AVAILABLE_RESOLUTIONS.get(2) // 720p default
        );
    }

    // ── API pública ───────────────────────────────────────────────────────

    /**
     * Retorna la resolución actual según el modo activo.
     */
    public synchronized Resolution getCurrentResolution() {
        return currentMode.isFullscreen() 
            ? currentFullscreenResolution 
            : currentWindowedResolution;
    }

    /**
     * Retorna la resolución del modo fullscreen (independiente del modo actual).
     */
    public synchronized Resolution getFullscreenResolution() {
        return currentFullscreenResolution;
    }

    /**
     * Retorna la resolución del modo windowed (independiente del modo actual).
     */
    public synchronized Resolution getWindowedResolution() {
        return currentWindowedResolution;
    }

    /**
     * Establece la resolución para el modo especificado.
     * NO cambia el modo actual, solo actualiza la resolución guardada.
     *
     * @param mode modo para el cual cambiar la resolución
     * @param resolution nueva resolución
     */
    public synchronized void setResolution(DisplayMode mode, Resolution resolution) {
        if (resolution == null) return;
        
        if (mode.isFullscreen()) {
            currentFullscreenResolution = resolution;
        } else {
            currentWindowedResolution = resolution;
        }
    }

    /**
     * Actualiza el modo actual (llamado cuando se hace toggle fullscreen).
     */
    public synchronized void setCurrentMode(DisplayMode mode) {
        this.currentMode = mode;
    }

    /**
     * Retorna el modo actual.
     */
    public synchronized DisplayMode getCurrentMode() {
        return currentMode;
    }

    /**
     * Retorna todas las resoluciones disponibles para fullscreen.
     * Ahora fullscreen y windowed comparten las mismas resoluciones.
     */
    public List<Resolution> getAvailableFullscreenResolutions() {
        return new ArrayList<>(AVAILABLE_RESOLUTIONS);
    }

    /**
     * Retorna todas las resoluciones disponibles para windowed.
     * Ahora fullscreen y windowed comparten las mismas resoluciones.
     */
    public List<Resolution> getAvailableWindowedResolutions() {
        return new ArrayList<>(AVAILABLE_RESOLUTIONS);
    }

    /**
     * Actualiza la resolución windowed cuando el usuario redimensiona manualmente.
     * Esto NO es una de las resoluciones predefinidas, pero se guarda como
     * "última resolución windowed" para cuando vuelva de fullscreen.
     *
     * @param width nuevo ancho de la ventana
     * @param height nuevo alto de la ventana
     */
    public synchronized void onWindowManualResize(int width, int height) {
        if (currentMode == DisplayMode.WINDOWED) {
            currentWindowedResolution = new Resolution(width, height);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Encuentra la resolución más cercana en la lista, o retorna el default.
     */
    private Resolution findClosestOrDefault(Resolution target, List<Resolution> available, Resolution defaultRes) {
        if (target == null) return defaultRes;
        
        Resolution closest = defaultRes;
        int minDiff = Integer.MAX_VALUE;
        
        for (Resolution res : available) {
            int diff = Math.abs(res.width - target.width) + Math.abs(res.height - target.height);
            if (diff < minDiff) {
                minDiff = diff;
                closest = res;
            }
        }
        
        return closest;
    }
}
