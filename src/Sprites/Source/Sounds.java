package Sprites.Source;

import java.io.IOException;
import java.net.URL;
import javax.sound.sampled.*;

/**
 * Reproductor de efectos de sonido.
 *
 * ── CORRECCIÓN — Regresión disparo ────────────────────────────────────────
 *
 * PROBLEMA ANTERIOR:
 *   playSound() llamaba AudioSystem.getAudioInputStream(soundURL) donde
 *   soundURL podía ser null si el archivo no existía en el classpath.
 *   AudioSystem.getAudioInputStream(null) lanza NullPointerException,
 *   lo que terminaba crasheando el flujo de disparo completo.
 *
 *   Causa concreta: WeaponEscopeta y WeaponPistola declaraban "Pistol.wav"
 *   como sonido de disparo, pero solo existe "Gun.wav" en el proyecto.
 *
 * SOLUCIÓN:
 *   1. Corregidos los nombres de archivo en WeaponEscopeta y WeaponPistola.
 *   2. playSound() verifica que soundURL != null antes de llamar AudioSystem.
 *      Un sonido faltante es un error de assets, no un error de lógica — el
 *      juego no debe crashear por ello.
 */
public class Sounds {

    public static void playSound(String filename) {
        if (filename == null || filename.isBlank()) return;

        try {
            URL soundURL = Sounds.class.getResource("/Sprites/Source/Utils/effects/" + filename);

            if (soundURL == null) {
                System.err.println("[Sounds] Archivo de sonido no encontrado: " + filename);
                return;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundURL);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("[Sounds] Error reproduciendo '" + filename + "': " + e.getMessage());
        }
    }
}
