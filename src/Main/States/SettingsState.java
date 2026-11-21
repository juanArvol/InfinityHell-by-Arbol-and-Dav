package Main.States;

import Display.Managers.DisplayManager;
import Display.Settings.ResolutionManager;
import Display.Settings.ScalingMode;
import Display.State.DisplayMode;
import Display.State.Resolution;
import Display.Surface.LayerIndex;
import Display.Surface.RenderFrame;
import Inputs.KeyBoard;
import Inputs.MouseInput;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * SettingsState — Menú de configuración de display con UI BIEN ESPACIADA.
 */
public final class SettingsState implements State {

    private final DisplayManager displayManager;
    private final ResolutionManager resolutionManager;
    private final MouseInput mouseInput;
    private final KeyBoard keyBoard;
    private final Runnable onExitCallback;
    
    private boolean leftButtonWasPressed = false;
    
    // Flag para diferir recreación de botones hasta después del render
    private boolean needsButtonRecreation = false;
    
    private int virtualWidth;
    private int virtualHeight;
    
    private ScalingMode currentScalingMode = ScalingMode.INTEGER_SCALE;
    private DisplayMode currentDisplayMode = DisplayMode.WINDOWED;
    
    private final List<UIButton> resolutionButtons = new ArrayList<>();
    private final List<UIButton> scalingButtons = new ArrayList<>();
    private UIButton toggleFullscreenButton;
    private UIButton backButton;
    private UIButton hoveredButton = null;
    
    private Font titleFont;
    private Font sectionFont;
    private Font normalFont;
    private Font smallFont;
    
    private static final Color BG_COLOR = new Color(20, 20, 30);
    private static final Color BUTTON_COLOR = new Color(60, 60, 70);
    private static final Color BUTTON_HOVER = new Color(80, 80, 100);
    private static final Color BUTTON_ACTIVE = new Color(100, 180, 100);
    private static final Color TEXT_COLOR = new Color(220, 220, 220);
    private static final Color ACCENT_COLOR = new Color(100, 200, 255);
    
    public SettingsState(DisplayManager displayManager, 
                         ResolutionManager resolutionManager,
                         KeyBoard keyBoard,
                         MouseInput mouseInput,
                         Runnable onExitCallback) {
        this.displayManager = displayManager;
        this.resolutionManager = resolutionManager;
        this.keyBoard = keyBoard;
        this.mouseInput = mouseInput;
        this.virtualWidth = displayManager.getVirtualWidth();
        this.virtualHeight = displayManager.getVirtualHeight();
        this.onExitCallback = onExitCallback;
        
        // Obtener estado actual del sistema
        this.currentDisplayMode = displayManager.getMode();
        this.currentScalingMode = displayManager.getScalingMode();
        
        createFonts();
        createButtons();
    }
    
    @Override
    public void onEnter() {
        virtualWidth = displayManager.getVirtualWidth();
        virtualHeight = displayManager.getVirtualHeight();
        // Obtener el estado REAL del sistema, no usar cache
        currentDisplayMode = displayManager.getMode();
        currentScalingMode = displayManager.getScalingMode();
        createFonts();
        createButtons();
        leftButtonWasPressed = false;  // Reset mouse state
    }
    
    @Override
    public void onExit() {
    }
    
    @Override
    public void update(Main.TemporalContext temporalContext) {
        keyBoard.update();
        mouseInput.flushEvents();
        
        // Refrescar el estado actual en cada frame para sincronizar con F11 y cambios externos
        DisplayMode newDisplayMode = displayManager.getMode();
        ScalingMode newScalingMode = displayManager.getScalingMode();
        
        // Si el modo o scaling cambió (por F11 o comando), marcar para recrear botones
        // NO recrear aquí directamente para evitar ConcurrentModificationException
        if (newDisplayMode != currentDisplayMode || newScalingMode != currentScalingMode) {
            currentDisplayMode = newDisplayMode;
            currentScalingMode = newScalingMode;
            needsButtonRecreation = true;
        }
        
        // Mouse hover - USAR COORDENADAS VIRTUALES (no raw)
        // Los botones están definidos en espacio virtual, así que necesitamos
        // las coordenadas del mouse también en espacio virtual
        float mouseVirtualX = mouseInput.mouseVirtualX;
        float mouseVirtualY = mouseInput.mouseVirtualY;
        
        hoveredButton = null;
        
        for (UIButton btn : resolutionButtons) {
            if (btn.bounds.contains((int)mouseVirtualX, (int)mouseVirtualY)) {
                hoveredButton = btn;
                break;
            }
        }
        
        if (hoveredButton == null) {
            for (UIButton btn : scalingButtons) {
                if (btn.bounds.contains((int)mouseVirtualX, (int)mouseVirtualY)) {
                    hoveredButton = btn;
                    break;
                }
            }
        }
        
        if (hoveredButton == null) {
            if (toggleFullscreenButton.bounds.contains((int)mouseVirtualX, (int)mouseVirtualY)) {
                hoveredButton = toggleFullscreenButton;
            } else if (backButton.bounds.contains((int)mouseVirtualX, (int)mouseVirtualY)) {
                hoveredButton = backButton;
            }
        }
        
        // Mouse clicks
        boolean leftButtonCurrentlyPressed = mouseInput.getInstanceButtonState("leftPressed");
        if (leftButtonCurrentlyPressed && !leftButtonWasPressed) {
            if (hoveredButton != null) {
                hoveredButton.action.run();
            }
        }
        leftButtonWasPressed = leftButtonCurrentlyPressed;
        
        // Recrear botones DESPUÉS de procesar input, para evitar ConcurrentModificationException
        if (needsButtonRecreation) {
            createButtons();
            needsButtonRecreation = false;
        }
    }
    
    @Override
    public void draw(RenderFrame frame) {
        Graphics2D g = frame.getLayerGraphics(LayerIndex.WORLD_BACKGROUND);
        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Background
        g.setColor(BG_COLOR);
        g.fillRect(0, 0, virtualWidth, virtualHeight);
        
        // Title
        g.setFont(titleFont);
        g.setColor(ACCENT_COLOR);
        String title = "DISPLAY SETTINGS";
        int titleWidth = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (virtualWidth - titleWidth) / 2, (int)(virtualHeight * 0.07f));
        
        // Current mode indicator (más abajo y con más espacio)
        g.setFont(smallFont);
        g.setColor(new Color(150, 180, 220));
        String modeText = "Mode: " + (currentDisplayMode.isFullscreen() ? "FULLSCREEN" : "WINDOWED");
        Resolution currentRes = resolutionManager.getCurrentResolution();
        String resText = currentRes.width + " × " + currentRes.height;
        String fullText = modeText + "  |  " + resText + "  |  " + currentScalingMode.name();
        int textWidth = g.getFontMetrics().stringWidth(fullText);
        g.drawString(fullText, (virtualWidth - textWidth) / 2, (int)(virtualHeight * 0.12f));
        
        // Resolution section
        renderSection(g, "RESOLUTION", (int)(virtualHeight * 0.16f));
        renderButtons(g, resolutionButtons);
        
        // Scaling mode section - MUCHO MÁS ABAJO para que no se superponga
        renderSection(g, "SCALING MODE", (int)(virtualHeight * 0.49f));
        renderButtons(g, scalingButtons);
        
        // Special buttons
        renderButton(g, toggleFullscreenButton);
        renderButton(g, backButton);
        
        // Instructions
        g.setFont(smallFont);
        g.setColor(new Color(120, 120, 130));
        String instructions = "Press F4 or ESC to return to game";
        int instrWidth = g.getFontMetrics().stringWidth(instructions);
        g.drawString(instructions, (virtualWidth - instrWidth) / 2, (int)(virtualHeight * 0.97f));
    }
    
    @Override
    public void shutdown() {
    }
    
    @Override
    public void onVirtualDimensionsChanged(int newW, int newH) {
        this.virtualWidth = newW;
        this.virtualHeight = newH;
        createFonts();
        createButtons();
    }
    
    private void createFonts() {
        int titleSize = Math.max(24, (int)(virtualHeight * 0.045f));
        int sectionSize = Math.max(18, (int)(virtualHeight * 0.032f));
        int normalSize = Math.max(16, (int)(virtualHeight * 0.026f));
        int smallSize = Math.max(14, (int)(virtualHeight * 0.020f));
        
        titleFont = new Font("Arial", Font.BOLD, titleSize);
        sectionFont = new Font("Arial", Font.BOLD, sectionSize);
        normalFont = new Font("Arial", Font.PLAIN, normalSize);
        smallFont = new Font("Arial", Font.PLAIN, smallSize);
    }
    
    private void createButtons() {
        resolutionButtons.clear();
        scalingButtons.clear();
        
        List<Resolution> availableResolutions = currentDisplayMode.isFullscreen()
            ? resolutionManager.getAvailableFullscreenResolutions()
            : resolutionManager.getAvailableWindowedResolutions();
        
        Resolution currentRes = resolutionManager.getCurrentResolution();
        
        // LAYOUT CON ESPACIADO APROPIADO
        int buttonWidth = Math.min(450, (int)(virtualWidth * 0.45f));
        int buttonHeight = Math.max(35, (int)(virtualHeight * 0.052f));
        int spacing = Math.max(12, (int)(virtualHeight * 0.022f));  // MÁS ESPACIO
        int centerX = virtualWidth / 2;
        
        // Resolution buttons - empiezan después del título de sección
        int startY = (int)(virtualHeight * 0.19f);
        
        for (int i = 0; i < availableResolutions.size(); i++) {
            Resolution res = availableResolutions.get(i);
            int y = startY + i * (buttonHeight + spacing);
            
            boolean isActive = res.equals(currentRes);
            String label = res.width + " × " + res.height;
            
            if (res.width == 1280 && res.height == 720) label += " (720p HD)";
            else if (res.width == 1920 && res.height == 1080) label += " (1080p Full HD)";
            else if (res.width == 2560 && res.height == 1440) label += " (1440p QHD)";
            else if (res.width == 3840 && res.height == 2160) label += " (4K UHD)";
            
            UIButton btn = new UIButton(
                new Rectangle(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight),
                label,
                () -> changeResolution(res),
                isActive
            );
            resolutionButtons.add(btn);
        }
        
        // Scaling mode buttons - MUCHO MÁS ABAJO para evitar superposición
        ScalingMode[] modes = {ScalingMode.FIT, ScalingMode.INTEGER_SCALE, ScalingMode.PIXEL_PERFECT, ScalingMode.STRETCH};
        startY = (int)(virtualHeight * 0.52f);  // Movido más abajo (era 0.50f)
        
        for (int i = 0; i < modes.length; i++) {
            ScalingMode mode = modes[i];
            int y = startY + i * (buttonHeight + spacing);
            
            boolean isActive = mode == currentScalingMode;
            String label = mode.name().replace("_", " ");
            
            UIButton btn = new UIButton(
                new Rectangle(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight),
                label,
                () -> changeScalingMode(mode),
                isActive
            );
            scalingButtons.add(btn);
        }
        
        // Toggle fullscreen button - AL FINAL con ESPACIO
        int toggleY = (int)(virtualHeight * 0.81f);  // Movido más abajo (era 0.78f)
        String toggleLabel = currentDisplayMode.isFullscreen() ? "SWITCH TO WINDOWED" : "SWITCH TO FULLSCREEN";
        toggleFullscreenButton = new UIButton(
            new Rectangle(centerX - buttonWidth / 2, toggleY, buttonWidth, buttonHeight),
            toggleLabel,
            this::toggleFullscreen,
            false
        );
        
        // Back button - CON ESPACIO
        int backY = (int)(virtualHeight * 0.90f);  // Movido más abajo (era 0.88f)
        backButton = new UIButton(
            new Rectangle(centerX - buttonWidth / 2, backY, buttonWidth, buttonHeight),
            "← BACK TO GAME",
            this::exitSettings,
            false
        );
    }
    
    private void renderSection(Graphics2D g, String title, int y) {
        g.setFont(sectionFont);
        g.setColor(ACCENT_COLOR);
        int titleWidth = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (virtualWidth - titleWidth) / 2, y);
    }
    
    private void renderButtons(Graphics2D g, List<UIButton> buttons) {
        // Crear snapshot de la lista para evitar ConcurrentModificationException
        // si createButtons() se llama durante la iteración
        List<UIButton> snapshot = new ArrayList<>(buttons);
        for (UIButton btn : snapshot) {
            renderButton(g, btn);
        }
    }
    
    private void renderButton(Graphics2D g, UIButton btn) {
        // Background
        if (btn.isActive) {
            g.setColor(BUTTON_ACTIVE);
        } else if (btn == hoveredButton) {
            g.setColor(BUTTON_HOVER);
        } else {
            g.setColor(BUTTON_COLOR);
        }
        g.fillRoundRect(btn.bounds.x, btn.bounds.y, btn.bounds.width, btn.bounds.height, 8, 8);
        
        // Border
        if (btn.isActive) {
            g.setColor(ACCENT_COLOR);
            g.setStroke(new java.awt.BasicStroke(3));
        } else {
            g.setColor(new Color(100, 100, 120));
            g.setStroke(new java.awt.BasicStroke(2));
        }
        g.drawRoundRect(btn.bounds.x, btn.bounds.y, btn.bounds.width, btn.bounds.height, 8, 8);
        
        // Text
        g.setFont(normalFont);
        g.setColor(TEXT_COLOR);
        int textWidth = g.getFontMetrics().stringWidth(btn.label);
        int textHeight = g.getFontMetrics().getAscent();
        int textX = btn.bounds.x + (btn.bounds.width - textWidth) / 2;
        int textY = btn.bounds.y + (btn.bounds.height + textHeight) / 2 - 2;
        g.drawString(btn.label, textX, textY);
    }
    
    private void changeResolution(Resolution newResolution) {
        // Obtener el modo ACTUAL del DisplayManager (no usar cache local)
        DisplayMode actualMode = displayManager.getMode();
        resolutionManager.setResolution(actualMode, newResolution);
        displayManager.requestChangeResolution(newResolution);
        // No necesitamos createButtons() aquí - el update() lo hará automáticamente
    }
    
    private void changeScalingMode(ScalingMode newMode) {
        displayManager.requestChangeScalingMode(newMode);
        // No necesitamos actualizar manualmente - el update() detecta el cambio
    }
    
    private void toggleFullscreen() {
        displayManager.requestToggleFullscreen();
        // El update() detectará el cambio de modo y recreará la UI automáticamente
    }
    
    private void exitSettings() {
        onExitCallback.run();
    }
    
    // UI Button helper class
    private static class UIButton {
        final Rectangle bounds;
        final String label;
        final Runnable action;
        final boolean isActive;
        
        UIButton(Rectangle bounds, String label, Runnable action, boolean isActive) {
            this.bounds = bounds;
            this.label = label;
            this.action = action;
            this.isActive = isActive;
        }
    }
}
