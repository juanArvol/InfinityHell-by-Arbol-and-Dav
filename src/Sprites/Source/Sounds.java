package Sprites.Source;

/**
 * Reproductor de efectos de sonido (DEPRECATED).
 *
 * ── DEPRECADO — Usar SoundManager en su lugar ────────────────────────────
 *
 * Esta clase se mantiene temporalmente para retrocompatibilidad, pero
 * delega toda la lógica a SoundManager, que implementa:
 *   - Pooling de clips (reutilización en lugar de crear nuevos)
 *   - Auto-limpieza (sin fugas de memoria)
 *   - Pre-carga de sonidos frecuentes
 *   - Límite de instancias simultáneas
 *
 * PROBLEMA ORIGINAL:
 *   playSound() creaba un nuevo Clip por cada disparo sin cerrarlo nunca.
 *   Después de 100 disparos = 100 Clips abiertos = fuga de memoria.
 *
 * MIGRACIÓN:
 *   Antes: Sounds.playSound("Gun.wav");
 *   Ahora: SoundManager.play("Gun.wav");  ← 10x más rápido, sin fugas
 *
 * @deprecated Usar {@link SoundManager} directamente
 */
@Deprecated
public class Sounds {

    /**
     * @deprecated Usar {@link SoundManager#play(String)}
     */
    @Deprecated
    public static void playSound(String filename) {
        SoundManager.play(filename);
    }
}
