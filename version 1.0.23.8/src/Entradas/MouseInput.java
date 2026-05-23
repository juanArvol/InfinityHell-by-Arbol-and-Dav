package Entradas;

import java.awt.event.*;

public class MouseInput implements 
        MouseListener, 
        MouseMotionListener, 
        MouseWheelListener {

    public static int mouseX;
    public static int mouseY;
    private static int wheelDelta;

    public static boolean leftPressed;
    public static boolean rightPressed;

    private static boolean leftClicked;
    private static boolean rightClicked;
    
    public static boolean isLeftClicked() {
        boolean result = leftClicked;
        leftClicked = false; // se consume
        return result;
    }

    public static boolean isRightClicked() {
        boolean result = rightClicked;
        rightClicked = false; // se consume
        return result;
    }

    @Override
    public void mousePressed(MouseEvent e) {

        if (e.getButton() == MouseEvent.BUTTON1) {
            leftPressed = true;
            leftClicked = true;
        }

        if (e.getButton() == MouseEvent.BUTTON3) {
            rightPressed = true;
            rightClicked = true;
        }
    }

    public static int getWheelDelta() {
        int delta = wheelDelta;
        wheelDelta = 0; // se consume
        return delta;
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {

        if (e.getButton() == MouseEvent.BUTTON1) {
            leftPressed = false;
        }

        if (e.getButton() == MouseEvent.BUTTON3) {
            rightPressed = false;
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        wheelDelta += e.getWheelRotation();
    }

    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
} 