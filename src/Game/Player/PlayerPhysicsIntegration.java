package Game.Player;

import Game.Engine.Entity.Components.ThermalComponent;
import Game.Engine.Physics.SimulaticWorld.WorldSimulation;

/**
 * Integración del Player con la simulación física genérica del Engine.
 *
 * ── HRFC — Player Reengineering v2 ────────────────────────────────────────
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * PlayerPhysicsIntegration conecta los componentes del Player con el sistema
 * de simulación física, permitiendo que participe automáticamente en fenómenos
 * como:
 *
 *   • Temperatura y transferencia térmica
 *   • Humedad y conductividad
 *   • Presión atmosférica
 *   • Electricidad y conductividad
 *   • Combustión
 *   • Fricción
 *   • Otros fenómenos físicos genéricos
 *
 * ── LO QUE NO HACE ────────────────────────────────────────────────────────
 *
 *   ✗ No implementa leyes físicas (Fourier, Ohm, Pascal, Newton)
 *   ✗ No calcula transferencias de calor, electricidad, etc.
 *   ✗ No duplica lógica que ya existe en WorldSimulation
 *
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 *
 *   Player
 *     │
 *     ├── ThermalComponent
 *     ├── HumidityComponent (futuro)
 *     ├── ConductivityComponent (futuro)
 *     └── Physical properties
 *             │
 *             ▼
 *   PlayerPhysicsIntegration
 *             │
 *             │ registra/conecta
 *             ▼
 *   WorldSimulation
 *             │
 *             ▼
 *   Physical Relations (Fourier, Ohm, etc.)
 *             │
 *             ▼
 *   Runtime / Components
 *
 * El Player simplemente proporciona las propiedades/componentes que
 * corresponden a la entidad. WorldSimulation maneja toda la física.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   // Durante construcción del Player en PlayerAssembler:
 *   PlayerPhysicsIntegration.integrate(player, worldSimulation);
 *
 *   // El Player ahora participa automáticamente en:
 *   // - Intercambio térmico con el ambiente
 *   // - Efectos de temperatura (congelación, quemaduras)
 *   // - Conducción eléctrica (si está en agua + hay electricidad)
 *   // - Presión atmosférica
 *   // - Etc.
 *
 * ── COMPONENTES SOPORTADOS ───────────────────────────────────────────────
 *
 * Actualmente:
 *   • ThermalComponent - temperatura del Player
 *
 * Futuro (según necesidades del juego):
 *   • HumidityComponent - nivel de humedad
 *   • ConductivityComponent - conductividad eléctrica
 *   • PressureComponent - resistencia a presión
 *   • CombustionComponent - inflamabilidad
 *
 * ── EJEMPLO DE INTEGRACIÓN TÉRMICA ───────────────────────────────────────
 *
 *   Player tiene ThermalComponent con temperatura inicial = 36.5°C
 *   Player entra en zona de lava (temperatura ambiente = 1000°C)
 *   
 *   WorldSimulation detecta:
 *     - Player tiene ThermalComponent
 *     - Ambiente tiene temperatura alta
 *     - Aplica ley de Fourier automáticamente
 *   
 *   Resultado:
 *     - Temperatura del Player aumenta gradualmente
 *     - StatusEffectSystem detecta temperatura > 50°C
 *     - Aplica efecto "Burning" al Player
 *     - HealthComponent recibe daño por segundo
 *
 * El Player NO necesita saber nada de esto - solo tiene el componente.
 */
public final class PlayerPhysicsIntegration {

    /**
     * Integra el Player con el sistema de simulación física genérico.
     *
     * Este método registra los componentes físicos del Player en la simulación,
     * permitiendo que participe en fenómenos físicos sin implementar las leyes.
     *
     * @param player         el Player a integrar
     * @param simulation     la simulación física del mundo (puede ser null si no hay simulación activa)
     */
    public static void integrate(Player player, WorldSimulation simulation) {
        if (player == null) {
            throw new IllegalArgumentException("player no puede ser null");
        }

        // Si no hay simulación activa, no hay nada que integrar
        if (simulation == null) {
            return;
        }

        // ── Integración Térmica ───────────────────────────────────────────
        integrateTemperature(player, simulation);

        // ── Futuras integraciones (placeholder) ───────────────────────────
        // integrateHumidity(player, simulation);
        // integrateConductivity(player, simulation);
        // integratePressure(player, simulation);
    }

    /**
     * Integra el componente térmico del Player con la simulación.
     *
     * Si el Player no tiene ThermalComponent, se añade uno con temperatura
     * corporal normal (36.5°C). Esto permite que el Player participe en
     * intercambio térmico con el ambiente.
     */
    private static void integrateTemperature(Player player, WorldSimulation simulation) {
        ThermalComponent thermal = player.getComponent(ThermalComponent.class);
        
        if (thermal == null) {
            // Crear componente con temperatura corporal normal
            thermal = new ThermalComponent(36.5);
            player.addComponent(thermal);
        }

        // WorldSimulation automáticamente detecta entidades con ThermalComponent
        // No necesitamos registro explícito - la simulación escanea componentes
    }

    /**
     * Desintegra el Player de la simulación física.
     *
     * Llamar cuando el Player es removido del mundo o la simulación se desactiva.
     * Limpia cualquier estado de simulación asociado al Player.
     *
     * @param player     el Player a desintegrar
     * @param simulation la simulación física (puede ser null)
     */
    public static void disintegrate(Player player, WorldSimulation simulation) {
        if (player == null || simulation == null) {
            return;
        }

        // La desintegración es automática - cuando el Player sale del mundo,
        // WorldSimulation deja de procesarlo automáticamente.
        // Este método existe para futura limpieza explícita si es necesaria.
    }

    /**
     * Verifica si el Player está integrado con una simulación física.
     *
     * @param player el Player a verificar
     * @return true si el Player tiene al menos un componente físico registrado
     */
    public static boolean isIntegrated(Player player) {
        if (player == null) {
            return false;
        }

        // Verificar si tiene componentes físicos
        return player.getComponent(ThermalComponent.class) != null;
        // Futuro: || player.getComponent(HumidityComponent.class) != null
        //        || player.getComponent(ConductivityComponent.class) != null
    }

    // ── Futuras integraciones (placeholder) ───────────────────────────────

    /**
     * Integra humedad del Player con la simulación.
     * TODO: Implementar cuando HumidityComponent exista.
     */
    @SuppressWarnings("unused")
    private static void integrateHumidity(Player player, WorldSimulation simulation) {
        // Futuro: Añadir HumidityComponent si no existe
        // El Player puede mojarse, secarse, etc.
    }

    /**
     * Integra conductividad eléctrica del Player con la simulación.
     * TODO: Implementar cuando ConductivityComponent exista.
     */
    @SuppressWarnings("unused")
    private static void integrateConductivity(Player player, WorldSimulation simulation) {
        // Futuro: Añadir ConductivityComponent si no existe
        // El Player húmedo conduce más electricidad
    }

    /**
     * Integra resistencia a presión del Player con la simulación.
     * TODO: Implementar cuando PressureComponent exista.
     */
    @SuppressWarnings("unused")
    private static void integratePressure(Player player, WorldSimulation simulation) {
        // Futuro: Añadir PressureComponent si no existe
        // Presión atmosférica, presión bajo agua, etc.
    }

    // Constructor privado — clase utilitaria pura
    private PlayerPhysicsIntegration() {}
}
