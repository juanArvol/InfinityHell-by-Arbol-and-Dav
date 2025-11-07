package nueva;
import States.GameState;
import entradas.KeyBoard;
import graficos.Assets;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;
import javax.swing.*;

public class NuevaVercion extends JFrame implements Runnable{

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    public final int widthMax = screenSize.width;
    public final int heightMax = screenSize.height;
    public static final int width =1200, height = 600;

    private Canvas canvas;

    //lo siguiente es un hilo para tener un programa dentro de otro programa
    private Thread thread;
    private boolean runner=false;
    private BufferStrategy bs;
    private Graphics g;

    private final int FPS = 30;
    private double objetivo =  1000000000.0 / FPS; //tiempo requerido para pasar fotograma
    private double delta = 0; //almacena el tiempo temporal al tiempo-- deltarepresenta el tiempo respecto al cambio
    private  int fpsPorSegundo = 0; //nos permite sabe a cuanto esta correiendo un juegp

    private GameState gameState;
    public KeyBoard  Keyboard;

    public NuevaVercion (){
        setTitle("Infinity Hell");
        setSize(width, height);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true); //esta hace que la ventama se redimencione en tiempo de ejecucion
        setLocationRelativeTo(null); //evitamos que la ventana se despliege en el centro de la pantalla
        setVisible(true);//hace la ventana visible

        canvas = new Canvas();
        Keyboard = new KeyBoard();
    
        //se limita el canvas y se le pasa un objeto de tipo dimencion
    
        canvas.setPreferredSize(new Dimension(width,height));
        canvas.setMaximumSize(new Dimension(widthMax,heightMax));
        canvas.setMinimumSize(new Dimension(width,height));
        canvas.setFocusable(true);

        add(canvas);
        canvas.addKeyListener(Keyboard);

        addComponentListener(new java.awt.event.ComponentAdapter() {
        @Override
        public void componentResized(java.awt.event.ComponentEvent e) {
            canvas.requestFocus();
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new NuevaVercion().start();
        });
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
        bs = canvas.getBufferStrategy();
        if (bs == null) {
            canvas.createBufferStrategy(3);
            return;
        }
        g = bs.getDrawGraphics();

        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
        
            if (gameState != null) gameState.draw(g);
        
            g.setColor(Color.BLACK);
            g.drawString("" + fpsPorSegundo, 10, 10);
        } finally {
            g.dispose();
        }
        bs.show();
    }
        
    private void init() {
        Assets.init();
        gameState = new GameState();
        canvas.createBufferStrategy(3);
        canvas.requestFocus();
    }

    @Override
    public void run() {
        long now = 0; //para tener un registro del tiempo
        long lastTime = System.nanoTime(); //esto me da el tiempo actual en nanosegundos
        int frames = 0;
        long time = 0;
        init();

        while (runner) {
            now = System.nanoTime();
            delta += (now -lastTime)/objetivo; //esto es para sumar el tiempo que valla pasando
            time += (now - lastTime);
            lastTime = now;
            if (delta >= 1) {   //de esta manera para poder cronometrar el tiempo
                update();
                draw();
                delta--;
                frames ++;
            }

            long frameTime = System.nanoTime() - now;
            long sleepTime = (long)(objetivo - frameTime) / 1_000_000;

            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (time >= 1000000000.0) {
                fpsPorSegundo = frames;
                frames = 0;
                time = 0; // Reiniciar tiempo
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
    private void stop() {
        runner = false; // ← Detenemos el bucle
        try {
            thread.join(); // Espera a que termine correctamente
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
