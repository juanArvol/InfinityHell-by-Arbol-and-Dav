package nueva;
import States.GameState;
import graficos.Assets;
import imput.KeyBoard;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import javax.swing.*;

public class NuevaVercion extends JFrame implements Runnable{

public static final int width = 1200, height = 600;
private Canvas canvas;

//lo siguiente es un hilo para tener un programa dentro de otro programa
private Thread thread;
private boolean runner=false;
private BufferStrategy bs;
private Graphics g;

private final int FPS = 60;
private double objetivo =  1000000000.0 / FPS; //tiempo requerido para pasar fotograma
private double delta = 0; //almacena el tiempo temporal al tiempo-- deltarepresenta el tiempo respecto al cambio
private  int fpsPorSegundo = FPS; //nos permite sabe a cuanto esta correiendo un juegp

private GameState gameState;
public KeyBoard  Keyboard;

public NuevaVercion (){
    setTitle("Infinity Hell");
    setSize(width, height);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setResizable(false); //esta hace que la ventama se redimencione en tiempo de ejecucion
    setLocationRelativeTo(null); //evitamos que la ventana se despliege en el centro de la pantalla
    setVisible(true);//hace la ventana visible

    canvas = new Canvas();
    Keyboard = new KeyBoard();
    
    //se limita el canvas y se le pasa un objeto de tipo dimencion
    canvas.setPreferredSize(new Dimension(width,height));
    canvas.setMaximumSize(new Dimension(width,height));
    canvas.setMinimumSize(new Dimension(width,height));
    canvas.setFocusable(rootPaneCheckingEnabled); //recibe entradas por parte del teclado

    add(canvas);
    canvas.addKeyListener(Keyboard);

}

    public static void main(String[] args) {
        new NuevaVercion().start();;

    }

    double posicionY = 190; 
    double posicionX = 50.0;
    private void update(){
        Keyboard.update();
        if (gameState != null) {
        gameState.update();
    }

    }
    private void draw() {
        bs = canvas.getBufferStrategy();//
        if (bs == null) {
            canvas.createBufferStrategy(3);//simplemente lo utilza y ya
            return;
        }
    g = bs.getDrawGraphics();

    // Pinta el fondo primero
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, width, height);

    // Dibuja la imagen
   if (gameState != null) {
        gameState.draw(g);
    }
    // Dibuja el texto de FPS
    g.setColor(Color.BLACK);
    g.drawString("" + fpsPorSegundo, 10, 10);

        g.dispose();
        bs.show();
    }
        
private void init() {
Assets.init();
gameState = new GameState();
}

    @Override
    public void run() {
        long now = 0; //para tener un registro del tiempo
        long lastTime = System.nanoTime(); //esto me da el tiempo actual en nanosegundos
        int frames = 0;
        int time = 0;
        init();



    while (runner) {
        now = System.nanoTime();
        delta += (now -lastTime)/objetivo; //esto es para sumar el tiempo que valla pasando
        time += (now - lastTime);
        lastTime = now;
        if (delta >= 1) {
            update();
            draw();
            delta--;
            frames ++;
            //de esta manera para poder cronometrar el tiempo
        }
        if (time >= 1000000000.0) {
           fpsPorSegundo = frames;
            frames = 0;
        }
        
    }
     stop();
    }

//metodo para iniciar como detener el hilo principal
private void start(){
    runner = true;
    thread = new Thread(this);//esta clase resive como parametro la interfaz runneblo
    thread.start();// esto me llama el metodo run
     
}
private void stop(){
try {
    thread.join();
    runner = false;
} catch (InterruptedException e) {
    e.printStackTrace();/// esto simplemente no c que hace pero supongo que toca profundizar en try catch 
}
}
}
