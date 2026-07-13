package Game.Engine.Systems;

/**
 * Contrato mínimo de configuración de debug para el Engine.
 *
 * DebugRenderSystem solo necesita saber si el debug está habilitado.
 * Esta interfaz permite que el Engine no dependa de Main.Debug.DebugGameSettings
 * — cualquier implementación (singleton de producción, mock de test) puede
 * inyectarse sin cambiar el Engine.
 *
 * DebugGameSettings en Main.Debug implementa esta interfaz.
 */
public interface DebugSettings {
    boolean isDebugEnabled();
}
