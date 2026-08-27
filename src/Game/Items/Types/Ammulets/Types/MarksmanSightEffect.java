package Game.Items.Types.Ammulets.Types;

import Game.Items.Types.Ammulets.Effects.UICapabilityEffect;
import Game.Gameplay.UI.Aim.TrajectoryVisualizationCapability;

import java.awt.Color;

/**
 * Marksman Sight — permite visualizar la trayectoria de las balas.
 * Proporciona capacidad UI para el sistema de apuntado.
 */
public final class MarksmanSightEffect extends UICapabilityEffect {

    public MarksmanSightEffect() {
        super(() -> new TrajectoryVisualizationCapability(
            TrajectoryVisualizationCapability.TrajectoryStyle.FADE,
            Color.CYAN
        ));
    }
}
