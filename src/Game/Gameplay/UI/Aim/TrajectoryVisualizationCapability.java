package Game.Gameplay.UI.Aim;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.BulletComport.TrajectoryProvider;
import Game.Items.Types.Bullets.Definition.BulletType;
import Game.Items.Types.Bullets.ProjectileTrajectoryPredictor;
import Game.Items.Types.Weapons.ModifiedWeapon;
import Game.Player.Player;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Capability de visualización de trayectoria — muestra la trayectoria proyectada
 * del próximo disparo consultando el TrajectoryProvider del BulletBehavior.
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 * La UI NO implementa la física del proyectil.
 * Esta capability CONSULTA la trayectoria al dominio de proyectiles:
 *
 *   TrajectoryVisualizationCapability (UI)
 *          │
 *          ├── obtiene ProjectilePreview desde PlayerCombat
 *          ├── obtiene BulletType actual
 *          ├── consulta behavior.getTrajectoryProvider()
 *          └── llama provider.predict(spawn, dir, speed, life)
 *          └── renderiza los puntos resultantes
 *
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 *
 * Esta capability representa la trayectoria REAL del proyectil, no una
 * aproximación genérica. Si MetheorBullet tiene gravedad extrema, la
 * trayectoria mostrada reflejará eso fielmente porque consulta directamente
 * al behavior del proyectil.
 *
 * ── ESTILOS DE RENDERIZADO ────────────────────────────────────────────────
 *
 *   DOTS       → puntos discretos (estilo clásico)
 *   LINE       → línea continua conectando puntos
 *   DASHED     → línea discontinua
 *   FADE       → puntos que se desvanecen con la distancia
 *   ARROW      → puntos con indicadores direccionales
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   // Concedido por amuleto:
 *   player.getAimCapabilities().add(
 *       new TrajectoryVisualizationCapability(
 *           virtualWidth, virtualHeight, 
 *           TrajectoryStyle.FADE, 
 *           Color.CYAN
 *       )
 *   );
 */
public class TrajectoryVisualizationCapability implements AimVisualizationCapability {

    private final TrajectoryStyle style;
    private final Color color;
    private final int maxPoints;
    private final int pointSize;

    /**
     * Estilo de renderizado de la trayectoria.
     */
    public enum TrajectoryStyle {
        /** Puntos discretos uniformes */
        DOTS,
        
        /** Línea continua conectando puntos */
        LINE,
        
        /** Línea discontinua (dash pattern) */
        DASHED,
        
        /** Puntos que se desvanecen con la distancia */
        FADE,
        
        /** Puntos con indicadores direccionales */
        ARROW
    }

    /**
     * @param style      estilo de renderizado
     * @param color      color base de la trayectoria
     * @param maxPoints  máximo de puntos a renderizar (para performance)
     * @param pointSize  tamaño de cada punto en píxeles
     */
    public TrajectoryVisualizationCapability(TrajectoryStyle style, Color color, 
                                             int maxPoints, int pointSize) {
        this.style = style;
        this.color = color;
        this.maxPoints = maxPoints;
        this.pointSize = pointSize;
    }

    /**
     * Constructor con valores por defecto.
     */
    public TrajectoryVisualizationCapability() {
        this(TrajectoryStyle.FADE, Color.CYAN, 100, 4);
    }

    /**
     * Constructor con estilo y color custom.
     */
    public TrajectoryVisualizationCapability(TrajectoryStyle style, Color color) {
        this(style, color, 100, 4);
    }

    @Override
    public void render(Graphics2D g, Player player, double camX, double camY) {
        ModifiedWeapon weapon = player.getRuntime().getCurrentWeapon();
        BulletType bulletType = player.getRuntime().getCurrentBullet();
        
        if (weapon == null || bulletType == null) return;

        // ── Obtener preview del proyectil ─────────────────────────────────
        ModifiedWeapon.ProjectilePreview preview = player.getCombat().getProjectilePreview();
        if (preview == null) return;

        // ── Consultar TrajectoryProvider del BulletBehavior ──────────────
        // La UI NO implementa física — delega al dominio de proyectiles
        BulletBehavior behavior = bulletType.create();
        TrajectoryProvider provider = behavior.getTrajectoryProvider();

        // ── Calcular spawn position en coordenadas de mundo ──────────────
        Vector2D playerPos = player.getPosition();
        Vector2D spawnPosition = playerPos.add(new Vector2D(20, 20)); // Offset del spawn

        // ── Predecir trayectoria ──────────────────────────────────────────
        ProjectileTrajectoryPredictor.TrajectoryPrediction prediction = 
                provider.predict(
                        spawnPosition,
                        player.getState().getAimDirection(),
                        preview.speed(),
                        Math.min(preview.lifeTime(), maxPoints)
                );

        // ── Renderizar trayectoria ────────────────────────────────────────
        Color prevColor = g.getColor();
        
        switch (style) {
            case DOTS -> renderDots(g, prediction, camX, camY);
            case LINE -> renderLine(g, prediction, camX, camY);
            case DASHED -> renderDashed(g, prediction, camX, camY);
            case FADE -> renderFade(g, prediction, camX, camY);
            case ARROW -> renderArrow(g, prediction, camX, camY);
        }
        
        g.setColor(prevColor);
    }

    private void renderDots(Graphics2D g, 
                           ProjectileTrajectoryPredictor.TrajectoryPrediction prediction,
                           double camX, double camY) {
        g.setColor(color);
        for (Vector2D point : prediction.points()) {
            int screenX = (int) (point.getX() - camX);
            int screenY = (int) (point.getY() - camY);
            g.fillOval(screenX - pointSize/2, screenY - pointSize/2, pointSize, pointSize);
        }
    }

    private void renderLine(Graphics2D g, 
                           ProjectileTrajectoryPredictor.TrajectoryPrediction prediction,
                           double camX, double camY) {
        g.setColor(color);
        Vector2D prev = null;
        for (Vector2D point : prediction.points()) {
            if (prev != null) {
                int x1 = (int) (prev.getX() - camX);
                int y1 = (int) (prev.getY() - camY);
                int x2 = (int) (point.getX() - camX);
                int y2 = (int) (point.getY() - camY);
                g.drawLine(x1, y1, x2, y2);
            }
            prev = point;
        }
    }

    private void renderDashed(Graphics2D g, 
                             ProjectileTrajectoryPredictor.TrajectoryPrediction prediction,
                             double camX, double camY) {
        g.setColor(color);
        Vector2D prev = null;
        int dashCounter = 0;
        for (Vector2D point : prediction.points()) {
            if (prev != null && dashCounter % 3 != 0) {
                int x1 = (int) (prev.getX() - camX);
                int y1 = (int) (prev.getY() - camY);
                int x2 = (int) (point.getX() - camX);
                int y2 = (int) (point.getY() - camY);
                g.drawLine(x1, y1, x2, y2);
            }
            prev = point;
            dashCounter++;
        }
    }

    private void renderFade(Graphics2D g, 
                           ProjectileTrajectoryPredictor.TrajectoryPrediction prediction,
                           double camX, double camY) {
        int total = prediction.size();
        for (int i = 0; i < total; i++) {
            Vector2D point = prediction.points().get(i);
            
            // Fade out: alpha decrece con la distancia
            float alpha = 1.0f - ((float) i / total);
            Color fadedColor = new Color(
                    color.getRed() / 255f,
                    color.getGreen() / 255f,
                    color.getBlue() / 255f,
                    alpha
            );
            g.setColor(fadedColor);
            
            int screenX = (int) (point.getX() - camX);
            int screenY = (int) (point.getY() - camY);
            g.fillOval(screenX - pointSize/2, screenY - pointSize/2, pointSize, pointSize);
        }
    }

    private void renderArrow(Graphics2D g, 
                            ProjectileTrajectoryPredictor.TrajectoryPrediction prediction,
                            double camX, double camY) {
        g.setColor(color);
        Vector2D prev = null;
        int arrowFrequency = 10; // Dibujar flecha cada 10 puntos
        int counter = 0;
        
        for (Vector2D point : prediction.points()) {
            int screenX = (int) (point.getX() - camX);
            int screenY = (int) (point.getY() - camY);
            
            // Punto base
            g.fillOval(screenX - pointSize/2, screenY - pointSize/2, pointSize, pointSize);
            
            // Flecha direccional cada N puntos
            if (prev != null && counter % arrowFrequency == 0) {
                double dx = point.getX() - prev.getX();
                double dy = point.getY() - prev.getY();
                double len = Math.sqrt(dx * dx + dy * dy);
                
                if (len > 0.1) {
                    dx /= len;
                    dy /= len;
                    
                    // Punta de flecha
                    int arrowSize = 6;
                    int tipX = screenX;
                    int tipY = screenY;
                    int baseX = (int) (screenX - dx * arrowSize);
                    int baseY = (int) (screenY - dy * arrowSize);
                    
                    // Aletas de la flecha
                    int leftX = (int) (baseX + dy * arrowSize/2);
                    int leftY = (int) (baseY - dx * arrowSize/2);
                    int rightX = (int) (baseX - dy * arrowSize/2);
                    int rightY = (int) (baseY + dx * arrowSize/2);
                    
                    g.drawLine(tipX, tipY, leftX, leftY);
                    g.drawLine(tipX, tipY, rightX, rightY);
                }
            }
            
            prev = point;
            counter++;
        }
    }

    @Override
    public int getRenderPriority() {
        return 50; // Renderiza después del crosshair (30), antes de info (70)
    }

    @Override
    public boolean requiresAiming() {
        return true; // Solo visible con botón derecho
    }

    @Override
    public String getName() {
        return "Trajectory Visualization (" + style.name() + ")";
    }
}
