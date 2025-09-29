package Game;

public class bulletType {
    private double direccionX;
    private double direccionY;
    private double velocidadX;
    private double velocidadY;
    
    private int tipo;
    
    public bulletType(int tipo, double directionY, double directionX) {

        direccionX = directionX;
        direccionY = directionY;

        switch (tipo) {
            case 1: // Bala normal
                velocidadY=1.57;
                velocidadX = 0.97;
                break;
            case 2: // Bala rápida
                velocidadY= 1;
                velocidadX = 1.69;
                break;
            case 3: // Bala lenta pero poderosa
                velocidadY= 0.01;
                velocidadX = 0.013;
                break;
            default:
                velocidadY = 2;
                velocidadX = 1.7;
                break;
        }
    }
    
    public double getVelocidadX() {
        return velocidadX*direccionX;
    }

    public double getVelocidadY() {
        return velocidadY*direccionY;
    }
}