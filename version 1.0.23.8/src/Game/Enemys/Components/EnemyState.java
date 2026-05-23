package Game.Enemys.Components;

public class EnemyState {

    private boolean enElSuelo;
    private boolean mirandoDerecha = true;

    private boolean moving;
    private boolean attacking;
    private boolean jumping;
    private boolean flying;

    //-----------------
    // SUELO
    //-----------------

    public boolean isEnElSuelo() {
        return enElSuelo;
    }

    public void setEnElSuelo(boolean enElSuelo) {
        this.enElSuelo = enElSuelo;
    }

    //-----------------
    // DIRECCION
    //-----------------

    public boolean isMirandoDerecha() {
        return mirandoDerecha;
    }

    public void setMirandoDerecha(boolean mirandoDerecha) {
        this.mirandoDerecha = mirandoDerecha;
    }

    //-----------------
    // MOVIMIENTO
    //-----------------

    public boolean isMoving() {
        return moving;
    }

    public void setMoving(boolean moving) {
        this.moving = moving;
    }

    //-----------------
    // ATAQUE
    //-----------------

    public boolean isAttacking() {
        return attacking;
    }

    public void setAttacking(boolean attacking) {
        this.attacking = attacking;
    }

    //-----------------
    // SALTO
    //-----------------

    public boolean isJumping() {
        return jumping;
    }

    public void setJumping(boolean jumping) {
        this.jumping = jumping;
    }
    public boolean isFlying(){
        return flying;
    }

    public void setFlying(boolean f){
        flying = f;
    }
}