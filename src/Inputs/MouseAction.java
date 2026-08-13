package Inputs;

/**
 * Representación tipada de acciones del mouse.
 *
 * ─── MINI-HRFC — TYPED MOUSE ACTIONS ──────────────────────────────────────────
 *
 * Elimina el uso de String como representación semántica de acciones del mouse
 * fuera de la capa de Input. Los String de acciones ("leftClick", "middleClick", 
 * etc.) pertenecen exclusivamente a la configuración/infraestructura de MouseInput.
 *
 * Al ser despachada una acción hacia consumidores del juego, se convierte a esta
 * representación tipada cerrada y conocida.
 *
 * ── FLUJO DE TRADUCCIÓN ───────────────────────────────────────────────────────
 *
 * AWT BUTTON2 → "middleClick" (configuración) → MouseAction.MIDDLE_CLICK (semántica)
 *
 * MouseInput mantiene la responsabilidad de traducción:
 *   - String actions permanecen en MouseButton (configuración interna)
 *   - MouseAction se despacha a listeners (frontera tipada)
 *   - PlayerCombat compara contra enums (sin strings)
 *
 * ── VALORES ───────────────────────────────────────────────────────────────────
 *
 * Mapeo directo con las acciones actuales en MouseButton.BUTTONS:
 *   - LEFT_CLICK     ← "leftClick"
 *   - LEFT_RELEASE   ← "leftRelease"  
 *   - RIGHT_CLICK    ← "rightClick"
 *   - RIGHT_RELEASE  ← "rightRelease"
 *   - MIDDLE_CLICK   ← "middleClick"
 */
public enum MouseAction {
    LEFT_CLICK,
    LEFT_RELEASE,
    RIGHT_CLICK,
    RIGHT_RELEASE,
    MIDDLE_CLICK
}