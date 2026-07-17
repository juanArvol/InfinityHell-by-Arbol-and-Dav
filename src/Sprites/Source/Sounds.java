package Sprites.Source;

import java.io.IOException;
import java.net.URL;
import javax.sound.sampled.*;

public class Sounds {

    public static void playSound(String filename) {
        try {
            URL soundURL = Sounds.class.getResource("/Sprites/Source/Utils/effects/" + filename);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundURL);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace(); // Para depurar errores de sonido
        }
    }
}