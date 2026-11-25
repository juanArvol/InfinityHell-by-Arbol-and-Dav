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
 * SettingsState — Menú de configuración responsive con selector de flechas y dropdown.
 */
public final class SettingsState implements State {

    private final DisplayManager displayManager;
    private final ResolutionManager resolutionManager;
    private final MouseInput mouseInput;
    private final KeyBoard keyBoard;
    private final Runnable onExitCallback;
    
    private boolean leftButtonWasPressed = false;
    private boolean needsButtonRecreation = false;
    
    private int virtualWidth;
    private int virtualHeight;
    
    private ScalingMode currentScalingMode = ScalingMode.INTEGER_SCALE;
    private DisplayMode currentDisplayMode = DisplayMode.WINDOWED;
    
    // Selector de resolución con flechas
    private UIButton resolutionLeftArrow;
    private UIButton resolutionRightArrow;
    private int currentResolutionIndex = 0;
    
    // Dropdown para scaling mode
    private UIButton scalingModeButton;
    private boolean scalingDropdownOpen = false;
    private List<UIButton> scalingDropdownButtons = new ArrayList<>();
    
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
        
        this.currentDisplayMode = displayManager.getMode();
        this.currentScalingMode = displayManager.getScalingMode();
        
        createFonts();
        createButtons();
    }
    
    @Override
    public void onEnter() {
        virtualWidth = displayManager.getVirtualWidth();
        virtualHeight = displayManager.getVirtualHeight();
        currentDisplayMode = displayManager.getMode();
        currentScalingMode = displayManager.getScalingMode();
        createFonts();
        createButtons();
        leftButtonWasPressed = false;
    }
    
    @Override
    public void onExit() {
    }
    
    @Override
    public void update(Main.TemporalContext temporalContext) {
        keyBoard.update();
        mouseInput.flushEvents();
        
        // Actualizar dimensiones virtuales si cambiaron
        int newVirtualWidth = displayManager.getVirtualWidth();
        int newVirtualHeight = displayManager.getVirtualHeight();
        
        if (newVirtualWidth != virtualWidth || newVirtualHeight != virtualHeight) {
            virtualWidth = newVirtualWidth;
            virtualHeight = newVirtualHeight;
            createFonts();
            createButtons();
        }
        
        // Refrescar el estado actual para sincronizar con F11 y cambios externos
        DisplayMode newDisplayMode = displayManager.getMode();
        ScalingMode newScalingMode = displayManager.getScalingMode();
        
        if (newDisplayMode != currentDisplayMode || newScalingMode != currentScalingMode) {
            currentDisplayMode = newDisplayMode;
            currentScalingMode = newScalingMode;
            needsButtonRecreation = true;
        }
        
        // Mouse hover - coordenadas virtuales
        float mouseVirtualX = mouseInput.mouseVirtualX;
        float mouseVirtualY = mouseInput.mouseVirtualY;
        
        hoveredButton = null;
        
        // Check resolution arrows
        if (resolutionLeftArrow != null && resolutionLeftArrow.bounds.contains((int)mouseVirtualX, (int)mouseVirtualY)) {
            hoveredButton = resolutionLeftArrow;
        } else if (resolutionRightArrow != null && resolutionRightArrow.bounds.contains((int)mouseVirtualX, (int)mouseVirtualY)) {
            hoveredButton = resolutionRightArrow;
        }
        
        // Check scaling mode dropdown
        if (hoveredButton == null) {
            if (scalingModeButton != null && scalingModeButton.bounds.contains((int)mouseVirtualX, (int)mouseVirtualY)) {
                hoveredButton = scalingModeButton;
            } else if (scalingDropdownOpen) {
                for (UIButton btn : scalingDropdownButtons) {
                    if (btn.bounds.contains((int)mouseVirtualX, (int)mouseVirtualY)) {
                        hoveredButton = btn;
                        break;
                    }
                }
            }
        }
        
        // Check other buttons
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
            } else if (scalingDropdownOpen) {
                // Click fuera del dropdown lo cierra
                scalingDropdownOpen = false;
                createButtons();
            }
        }
        leftButtonWasPressed = leftButtonCurrentlyPressed;
        
        // Recrear botones si es necesario
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
        
        // Current mode indicator
        g.setFont(smallFont);
        g.setColor(new Color(150, 180, 220));
        String modeText = "Mode: " + (currentDisplayMode.isFullscreen() ? "FULLSCREEN" : "WINDOWED");
        Resolution currentRes = resolutionManager.getCurrentResolution();
        String resText = currentRes.width + " × " + currentRes.height;
        String fullText = modeText + "  |  " + resText + "  |  " + currentScalingMode.name();
        int textWidth = g.getFontMetrics().stringWidth(fullText);
        g.drawString(fullText, (virtualWidth - textWidth) / 2, (int)(virtualHeight * 0.12f));
        
        // Resolution section con selector de flechas
        renderSection(g, "RESOLUTION", (int)(virtualHeight * 0.20f));
        renderResolutionSelector(g);
        
        // Scaling mode section con dropdown
        renderSection(g, "SCALING MODE", (int)(virtualHeight * 0.40f));
        renderScalingDropdown(g);
        
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
        // Escalado completamente dinámico basado en 720p como referencia
        float baseScale = virtualHeight / 720f;
        
        int titleSize = Math.max(8, Math.round(32 * baseScale));
        int sectionSize = Math.max(7, Math.round(24 * baseScale));
        int normalSize = Math.max(6, Math.round(18 * baseScale));
        int smallSize = Math.max(5, Math.round(14 * baseScale));
        
        titleFont = new Font("Arial", Font.BOLD, titleSize);
        sectionFont = new Font("Arial", Font.BOLD, sectionSize);
        normalFont = new Font("Arial", Font.PLAIN, normalSize);
        smallFont = new Font("Arial", Font.PLAIN, smallSize);
    }
    
    private void createButtons() {
        scalingDropdownButtons.clear();
        
        List<Resolution> availableResolutions = currentDisplayMode.isFullscreen()
            ? resolutionManager.getAvailableFullscreenResolutions()
            : resolutionManager.getAvailableWindowedResolutions();
        
        Resolution currentRes = resolutionManager.getCurrentResolution();
        
        // Encontrar el índice de la resolución actual
        currentResolutionIndex = 0;
        for (int i = 0; i < availableResolutions.size(); i++) {
            if (availableResolutions.get(i).equals(currentRes)) {
                currentResolutionIndex = i;
                break;
            }
        }
        
        // Layout responsive basado en 720p
        float baseScale = virtualHeight / 720f;
        
        int buttonWidth = Math.round(400 * baseScale);
        int buttonHeight = Math.round(45 * baseScale);
        int centerX = virtualWidth / 2;
        
        // ===== RESOLUTION SELECTOR CON FLECHAS =====
        int resolutionY = Math.round(virtualHeight * 0.26f);
        
        int arrowSize = buttonHeight;
        int arrowSpacing = Math.round(12 * baseScale);
        int labelWidth = Math.round(buttonWidth * 0.60f);
        
        resolutionLeftArrow = new UIButton(
            new Rectangle(centerX - labelWidth/2 - arrowSize - arrowSpacing, resolutionY, arrowSize, buttonHeight),
            "<",
            this::previousResolution,
            false
        );
        
        resolutionRightArrow = new UIButton(
            new Rectangle(centerX + labelWidth/2 + arrowSpacing, resolutionY, arrowSize, buttonHeight),
            ">",
            this::nextResolution,
            false
        );
        
        // ===== SCALING MODE DROPDOWN =====
        int scalingY = Math.round(virtualHeight * 0.46f);
        
        scalingModeButton = new UIButton(
            new Rectangle(centerX - buttonWidth / 2, scalingY, buttonWidth, buttonHeight),
            getScalingModeLabel(currentScalingMode) + " ▼",
            this::toggleScalingDropdown,
            false
        );
        
        // Crear botones del dropdown
        ScalingMode[] modes = {ScalingMode.FIT, ScalingMode.INTEGER_SCALE, ScalingMode.PIXEL_PERFECT, ScalingMode.STRETCH};
        int dropdownItemHeight = Math.round(40 * baseScale);
        
        for (int i = 0; i < modes.length; i++) {
            ScalingMode mode = modes[i];
            int y = scalingY + buttonHeight + (i * dropdownItemHeight);
            
            boolean isActive = mode == currentScalingMode;
            
            UIButton btn = new UIButton(
                new Rectangle(centerX - buttonWidth / 2, y, buttonWidth, dropdownItemHeight),
                getScalingModeLabel(mode),
                () -> selectScalingMode(mode),
                isActive
            );
            scalingDropdownButtons.add(btn);
        }
        
        // Calcular espacio ocupado por dropdown
        int dropdownTotalHeight = buttonHeight + (modes.length * dropdownItemHeight);
        
        // ===== TOGGLE FULLSCREEN BUTTON =====
        int dropdownGap = Math.round(30 * baseScale);
        int toggleY = scalingDropdownOpen 
            ? (scalingY + dropdownTotalHeight + dropdownGap)
            : Math.round(virtualHeight * 0.60f);
            
        String toggleLabel = currentDisplayMode.isFullscreen() ? "SWITCH TO WINDOWED" : "SWITCH TO FULLSCREEN";
        toggleFullscreenButton = new UIButton(
            new Rectangle(centerX - buttonWidth / 2, toggleY, buttonWidth, buttonHeight),
            toggleLabel,
            this::toggleFullscreen,
            false
        );
        
        // ===== BACK BUTTON =====
        int buttonGap = Math.round(20 * baseScale);
        int backY = scalingDropdownOpen
            ? (toggleY + buttonHeight + buttonGap)
            : Math.round(virtualHeight * 0.78f);
            
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
    
    private void renderResolutionSelector(Graphics2D g) {
        List<Resolution> availableResolutions = currentDisplayMode.isFullscreen()
            ? resolutionManager.getAvailableFullscreenResolutions()
            : resolutionManager.getAvailableWindowedResolutions();
        
        if (currentResolutionIndex < 0 || currentResolutionIndex >= availableResolutions.size()) {
            currentResolutionIndex = 0;
        }
        
        Resolution currentRes = availableResolutions.get(currentResolutionIndex);
        
        // Renderizar las flechas
        renderButton(g, resolutionLeftArrow);
        renderButton(g, resolutionRightArrow);
        
        // Renderizar el texto central con la resolución
        g.setFont(normalFont);
        g.setColor(TEXT_COLOR);
        
        String resolutionText = currentRes.width + " × " + currentRes.height;
        
        // Añadir el label de calidad
        if (currentRes.width == 1280 && currentRes.height == 720) resolutionText += " (720p HD)";
        else if (currentRes.width == 1920 && currentRes.height == 1080) resolutionText += " (1080p Full HD)";
        else if (currentRes.width == 2560 && currentRes.height == 1440) resolutionText += " (1440p QHD)";
        else if (currentRes.width == 3840 && currentRes.height == 2160) resolutionText += " (4K UHD)";
        
        int textWidth = g.getFontMetrics().stringWidth(resolutionText);
        int textHeight = g.getFontMetrics().getAscent();
        int textX = virtualWidth / 2 - textWidth / 2;
        int textY = resolutionLeftArrow.bounds.y + (resolutionLeftArrow.bounds.height + textHeight) / 2 - 2;
        
        g.drawString(resolutionText, textX, textY);
    }
    
    private void renderScalingDropdown(Graphics2D g) {
        // Renderizar el botón principal
        renderButton(g, scalingModeButton);
        
        // Si el dropdown está abierto, renderizar las opciones
        if (scalingDropdownOpen) {
            for (UIButton btn : scalingDropdownButtons) {
                renderButton(g, btn);
            }
        }
    }
    
    private void renderButton(Graphics2D g, UIButton btn) {
        // Todo escala uniformemente usando 720p como base
        float baseScale = virtualHeight / 720f;
        
        int borderRadius = Math.max(4, Math.round(8 * baseScale));
        float strokeWidth = Math.max(1f, 2f * baseScale);
        
        // Background
        if (btn.isActive) {
            g.setColor(BUTTON_ACTIVE);
        } else if (btn == hoveredButton) {
            g.setColor(BUTTON_HOVER);
        } else {
            g.setColor(BUTTON_COLOR);
        }
        g.fillRoundRect(btn.bounds.x, btn.bounds.y, btn.bounds.width, btn.bounds.height, borderRadius, borderRadius);
        
        // Border
        if (btn.isActive) {
            g.setColor(ACCENT_COLOR);
            g.setStroke(new java.awt.BasicStroke(strokeWidth * 1.5f));
        } else {
            g.setColor(new Color(100, 100, 120));
            g.setStroke(new java.awt.BasicStroke(strokeWidth));
        }
        g.drawRoundRect(btn.bounds.x, btn.bounds.y, btn.bounds.width, btn.bounds.height, borderRadius, borderRadius);
        
        // Text
        g.setFont(normalFont);
        g.setColor(TEXT_COLOR);
        int textWidth = g.getFontMetrics().stringWidth(btn.label);
        int textHeight = g.getFontMetrics().getAscent();
        int textX = btn.bounds.x + (btn.bounds.width - textWidth) / 2;
        int textY = btn.bounds.y + (btn.bounds.height + textHeight) / 2 - Math.round(textHeight * 0.15f);
        g.drawString(btn.label, textX, textY);
    }
    
    private void changeResolution(Resolution newResolution) {
        DisplayMode actualMode = displayManager.getMode();
        resolutionManager.setResolution(actualMode, newResolution);
        displayManager.requestChangeResolution(newResolution);
    }
    
    private void previousResolution() {
        List<Resolution> availableResolutions = currentDisplayMode.isFullscreen()
            ? resolutionManager.getAvailableFullscreenResolutions()
            : resolutionManager.getAvailableWindowedResolutions();
        
        if (currentResolutionIndex > 0) {
            currentResolutionIndex--;
            changeResolution(availableResolutions.get(currentResolutionIndex));
        }
    }
    
    private void nextResolution() {
        List<Resolution> availableResolutions = currentDisplayMode.isFullscreen()
            ? resolutionManager.getAvailableFullscreenResolutions()
            : resolutionManager.getAvailableWindowedResolutions();
        
        if (currentResolutionIndex < availableResolutions.size() - 1) {
            currentResolutionIndex++;
            changeResolution(availableResolutions.get(currentResolutionIndex));
        }
    }
    
    private void toggleScalingDropdown() {
        scalingDropdownOpen = !scalingDropdownOpen;
        createButtons();
    }
    
    private void selectScalingMode(ScalingMode mode) {
        changeScalingMode(mode);
        scalingDropdownOpen = false;
        createButtons();
    }
    
    private String getScalingModeLabel(ScalingMode mode) {
        switch (mode) {
            case FIT: return "FIT";
            case INTEGER_SCALE: return "INTEGER SCALE";
            case PIXEL_PERFECT: return "PIXEL PERFECT";
            case STRETCH: return "STRETCH";
            default: return mode.name();
        }
    }
    
    private void changeScalingMode(ScalingMode newMode) {
        displayManager.requestChangeScalingMode(newMode);
    }
    
    private void toggleFullscreen() {
        displayManager.requestToggleFullscreen();
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
