package Game.UI;

import java.awt.Color;
import java.awt.Graphics;

import Game.Player.PlayerStats;

public class LifeHUD implements UIElement {

    private int x;
    private int y;
    private final double originalXRatio;
    private final double originalYRatio;

    private final PlayerStats stats;

    public LifeHUD(PlayerStats stats, int screenWidth, int screenHeight) {
        this.stats = stats;
        this.originalXRatio = 0.02; // 2% desde la izquierda
        this.originalYRatio = 0.02; // 2% desde arriba

        this.x = (int)(screenWidth * originalXRatio);
        this.y = (int)(screenHeight * originalYRatio);
    }

    @Override
    public void update() {}

    @Override
    public void onResize(int screenWidth, int screenHeight) {
        x = (int)(screenWidth * originalXRatio);
        y = (int)(screenHeight * originalYRatio);
    }

    @Override
    public void draw(Graphics g) {
        int width = 200;
        int height = 20;

        g.setColor(Color.BLACK);
        g.drawRect(x, y, width, height);

        double healthPercent = stats.getLife() / (double)stats.getLifeMax();

        g.setColor(Color.RED);
        g.fillRect(x + 1, y + 1, (int)((width - 1) * healthPercent), height - 1);

        g.setColor(Color.BLACK);
        g.drawString("HP: " + stats.getLife() + "/" + stats.getLifeMax(), x, y + 35);
    }
}/* 
package Game.UI;

import java.awt.Color;
import java.awt.Graphics;

import Game.Player.PlayerStats;

public class LifeHUD implements UIElement{

    private final PlayerStats stats;

    private double displayedWidth = -1;
    private final double shrinkSpeed = 6.0;

    private final int barWidth = 200;
    private final int barHeight = 20;

    private final int x = 85;
    private final int y = 20;

    public LifeHUD(PlayerStats stats) {
        this.stats = stats;
    }
    
    @Override
    public void update() {

        int life = stats.getLife();
        int max = stats.getLifeMax();

        double targetWidth = ((double) life / max) * barWidth;

        if (displayedWidth < 0) {
            displayedWidth = targetWidth;
        }

        if (displayedWidth > targetWidth) {
            displayedWidth -= shrinkSpeed;
            if (displayedWidth < targetWidth) {
                displayedWidth = targetWidth;
            }
        } else if (displayedWidth < targetWidth) {
            displayedWidth += shrinkSpeed;
            if (displayedWidth > targetWidth) {
                displayedWidth = targetWidth;
            }
        }
    }

    @Override
    public void draw(Graphics g) {

        int life = stats.getLife();
        int max = stats.getLifeMax();

        double percent = max > 0 ? (life / (double) max) : 0.0;

        // Borde
        g.setColor(Color.BLACK);
        g.drawRect(x, y, barWidth, barHeight);

        // Color dinámico
        if (percent > 0.6) {
            g.setColor(Color.GREEN);
        } else if (percent > 0.3) {
            g.setColor(Color.ORANGE);
        } else {
            g.setColor(Color.RED);
        }

        g.fillRect(x + 1, y + 1,
                (int) displayedWidth,
                barHeight - 1);
    }
} */