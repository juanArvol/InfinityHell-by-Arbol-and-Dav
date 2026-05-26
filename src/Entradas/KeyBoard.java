package Entradas;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyBoard implements KeyListener {

    private final boolean[] keys = new boolean[256];
    private final boolean[] lastKeys = new boolean[256];

    public static boolean up, left, right, space, c, r, down, shift, f11;


    public void update(){

        up = keys[KeyEvent.VK_W];
        space = keys[KeyEvent.VK_SPACE];
        left = keys[KeyEvent.VK_A];
        right = keys[KeyEvent.VK_D];
        down = keys[KeyEvent.VK_S];
        c = keys[KeyEvent.VK_C];
        shift = keys[KeyEvent.VK_SHIFT];
        r = keys[KeyEvent.VK_R];

        f11 = keys[KeyEvent.VK_F11] && !lastKeys[KeyEvent.VK_F11];
        // Copiar estado actual a anterior
        System.arraycopy(keys, 0, lastKeys, 0, keys.length);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() < keys.length)
            keys[e.getKeyCode()] = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() < keys.length)
            keys[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}