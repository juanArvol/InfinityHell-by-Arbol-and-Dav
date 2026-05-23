package Game.Player;

import GameMath.Vector2D;

public class PlayerState {
    
    private boolean congelado;
    private boolean enElSuelo;
    private boolean mirandoDerecha = true;
    private boolean mirandoArriba;
    private boolean mirandoAbajo;
    private boolean agachado;
    private boolean running;
    private boolean reloading;

    private Vector2D aimDirection = new Vector2D(1, 0);

    public boolean isReloading(){ return reloading; }
    public void setReloaging(boolean v) { reloading = v; }
    public boolean isCongelado() { return congelado; }
    public void setCongelado(boolean v) { congelado = v; }

    public boolean isEnElSuelo() { return enElSuelo; }
    public void setEnElSuelo(boolean v) { enElSuelo = v; }

    public boolean isDer() { return mirandoDerecha; }
    public void setDer(boolean v) { mirandoDerecha = v; }

    public boolean isMirandoArriba() { return mirandoArriba; }
    public void setMirandoArriba(boolean v) { mirandoArriba = v; }

    public boolean isMirandoAbajo() { return mirandoAbajo; }
    public void setMirandoAbajo(boolean v) { mirandoAbajo = v; }

    public boolean isAgachado() { return agachado; }
    public void setAgachado(boolean v) { agachado = v; }

    public boolean isRunning() { return running; }
    public void setRunning(boolean v) { running = v; }

    public boolean isMirandoArribaOAbajo() {
        return mirandoArriba || mirandoAbajo;
    }
    public Vector2D getAimDirection() {
        return aimDirection;
    }
    public void setAimDirection(Vector2D dir) {
        this.aimDirection = dir;
    }
} 