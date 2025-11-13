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
import java.util.Scanner;
public class NuevaVersion extends JFrame implements Runnable{

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

    private static Scanner scanner = new Scanner(System.in);
    
    public NuevaVersion (){
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
        /* SwingUtilities.invokeLater(() -> {
            new NuevaVersion().start();
        }); */
        // Llamada a los ejercicios del taller
        ejercicio8();
    }
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
    // ejercicio taller

    /* 1. Numeros primos en un rango
        Solicita al usuario dos números (inicio y fin) y muestra todos los números primos dentro de ese rango.
        Estructuras: condicionales, bucles anidados, funciones. */
    private static void ejercicio1() {
        System.out.print("Ingrese el número de inicio: ");
        int inicio = scanner.nextInt();
        System.out.print("Ingrese el número de fin: ");
        int fin = scanner.nextInt();

        System.out.println("Números primos entre " + inicio + " y " + fin + ":");
        for (int num = inicio; num <= fin; num++) {
            if (esPrimo(num)) {
                System.out.print(num + " ");
            }
        }
        System.out.println();
          
    }
    private static boolean esPrimo(int numero) {
            if (Math.abs(numero) < 2) return false;
            for (int i = 2; i <= Math.sqrt(numero); i++) {
                if (numero % i == 0) return false;
            }
            return true;
    }
    /* 2. Serie de Fibonacci con límite
        Genera los primeros n números de la serie de Fibonacci, donde n es ingresado por el usuario.
        Estructuras: bucle for o while, variables acumuladoras. */
    private static void ejercicio2() {
        System.out.print("Ingrese la cantidad de números de Fibonacci a generar: ");
        int n = scanner.nextInt();

        int a = 0, b = 1;
        long aa= 0, bb= 1;

        System.out.print("Serie de Fibonacci: ");
            for (int i = 0; i < n; i++) {
                System.out.print(a + " ");
                int siguiente = a + b;
                a = b;
                b = siguiente;
            }

        System.out.println();
        System.out.println("explicacion: " );
            for (int i = 1; i <= n; i++) {
        if (i == 1) {
            System.out.println("Paso 1: 0 (primer número de la serie)");
        } else if (i == 2) {
            System.out.println("Paso 2: 1 (segundo número de la serie)");
        } else {
            long siguiente = aa + bb;
            System.out.println("Paso " + i + ": " + siguiente + 
                " (se obtiene sumando " + aa + " + " + bb + ")");
            aa = bb;
            bb = siguiente;
        }
    }
    }
    /* 3. Promedio de calificaciones
        Pide al usuario las calificaciones de varios estudiantes (cantidad determinada por el usuario). Calcula y muestra el promedio general y cuántos están por encima del promedio.
        Estructuras: bucle for, listas, condicionales. */
    private static void ejercicio3() {
        System.out.print("Ingrese la cantidad de los estudiantes: ");
        int cantidadE = scanner.nextInt();
        if(cantidadE <=0){
            System.out.println("Cantidad invalida");
            return;
        }
        double[] calificaciones = new double[cantidadE];
        double suma = 0;

        for (int i = 0; i < cantidadE; i++) {
            System.out.print("Ingrese la calificación del estudiante " + (i + 1) + ": ");
            for(int j = 0; j<cantidadE; j++){
                calificaciones[j] = scanner.nextDouble();
            }
            suma += calificaciones[i];
        }

        double promedio = suma / cantidadE;
        System.out.printf("Promedio general: %.2f%n", promedio);

        int countAboveAverage = 0;
        for (double calificacion : calificaciones) {
            if (calificacion > promedio) {
                countAboveAverage++;
            }
        }
        System.out.println("Número de estudiantes por encima del promedio: " + countAboveAverage);
    }
    /* 4. Tabla de multiplicar con formato
        Solicita un número y muestra su tabla de multiplicar del 1 al 10, formateada en columnas.
        Estructuras: bucle for, operaciones aritméticas, formato de salida. */
        private static void ejercicio4(){
            System.out.print("Ingrese un número para ver su tabla de multiplicar: ");
            double numero = scanner.nextDouble();
            System.out.println("Tabla de multiplicar del " + numero + ":");
            
            for (int i = 1; i <= 10; i++) {
                System.out.printf("%2f x %2d = %3f%n", numero, i, numero * i);
            }
        }

    /* 5. Adivina el número
        Genera un número aleatorio del 1 al 50. El usuario debe adivinarlo, recibiendo mensajes como 'más alto' o 'más bajo' hasta acertar.
        Estructuras: bucle while, condicionales. */
        private static void ejercicio5(){
            int numeroSecreto = (int)(Math.random() * 50) + 1;
            int intento = 0;
            System.out.println("Adivina el número entre 1 y 50:");

            while (intento != numeroSecreto) {
                System.out.print("Ingrese su numero de intento: ");
                intento = scanner.nextInt();

                if (intento < numeroSecreto) {
                    System.out.println("Más grande.");
                } else if (intento > numeroSecreto) {
                    System.out.println("Más pequeño.");
                } else {
                    System.out.println("¡Correcto! Has adivinado el número.");
                }
            }
        }
    /* 6. Calculadora de edad exacta
        Pide al usuario su fecha de nacimiento (día, mes, año) y calcula su edad actual exacta (años, meses y días).
        Estructuras: manejo de fechas, condicionales, operaciones aritméticas. */
        private static void ejercicio6(){
            System.out.print("Ingrese su año de nacimiento (YYYY): "); 
            int anioNacimiento = scanner.nextInt();
            System.out.print("Ingrese su mes de nacimiento (1-12): ");
            int mesNacimiento = scanner.nextInt();
            System.out.print("Ingrese su día de nacimiento (1-31): ");
            int diaNacimiento = scanner.nextInt();

            java.time.LocalDate fechaNacimiento = java.time.LocalDate.of(anioNacimiento, mesNacimiento, diaNacimiento);
            java.time.LocalDate fechaActual = java.time.LocalDate.now();

            java.time.Period edad = java.time.Period.between(fechaNacimiento, fechaActual);

            System.out.println("Su edad exacta es: " + edad.getYears() + " años, " + edad.getMonths() + " meses y " + edad.getDays() + " días.");
        }

    /* 7. Cajero automático simplificado
        Simula un cajero: el usuario tiene un saldo inicial. Puede elegir entre depositar, retirar o consultar saldo hasta que elija salir.
        Estructuras: menú con bucle do-while o while, condicionales. */
        private static void ejercicio7(){
            double saldo = 1000.0; // Saldo inicial
            int opcion;

            do {
                System.out.println("\n--- Cajero Automático ---");
                System.out.println("1. Consultar saldo");
                System.out.println("2. Depositar dinero");
                System.out.println("3. Retirar dinero");
                System.out.println("4. Salir");
                System.out.print("Seleccione una opción: ");
                opcion = scanner.nextInt();

                switch (opcion) {
                    case 1:
                        System.out.printf("Su saldo actual es: %.2f%n", saldo);
                        break;
                    case 2:
                        System.out.print("Ingrese la cantidad a depositar: ");
                        double deposito = scanner.nextDouble();
                        if (deposito > 0) {
                            saldo += deposito;
                            System.out.printf("Ha depositado: %.2f%n", deposito);
                        } else {
                            System.out.println("Cantidad inválida.");
                        }
                        break;
                    case 3:
                        System.out.print("Ingrese la cantidad a retirar: ");
                        double retiro = scanner.nextDouble();
                        if (retiro > 0 && retiro <= saldo) {
                            saldo -= retiro;
                            System.out.printf("Ha retirado: %.2f%n", retiro);
                        } else {
                            System.out.println("Cantidad inválida o saldo insuficiente.");
                        }
                        break;
                    case 4:
                        System.out.println("Gracias por usar el cajero automático.");
                        break;
                    default:
                        System.out.println("Opción inválida. Intente de nuevo.");
                }
            } while (opcion != 4);
        }

    /* 8. Buscar el valor máximo y mínimo
        Pide n números al usuario y determina el mayor y el menor de todos.
        Estructuras: bucle for, comparaciones, acumuladores. */
        private static void ejercicio8(){
            System.out.print("¿Cuántos números desea ingresar? ");
            int n = scanner.nextInt();

            if (n <= 0) {
                System.out.println("Cantidad inválida.");
                return;
            }

            double maximo = Double.NEGATIVE_INFINITY;
            double minimo = Double.POSITIVE_INFINITY;

            for (int i = 0; i < n; i++) {
                System.out.print("Ingrese el número " + (i + 1) + ": ");
                double numero = scanner.nextDouble();

                if (numero > maximo) {
                    maximo = numero;
                }
                if (numero < minimo) {
                    minimo = numero;
                }
            }

            System.out.printf("El número máximo es: %.2f%n", maximo);
            System.out.printf("El número mínimo es: %.2f%n", minimo);
        }
    /* 9. Contar palabras en una oración
        Solicita al usuario una oración y muestra cuántas palabras contiene y la longitud promedio de las palabras.
        Estructuras: cadenas, bucle, condicionales. */

    /* 10. Validar contraseña segura
        Pide una contraseña y valida que cumpla con los siguientes requisitos:
        - Al menos 8 caracteres
        - Contenga mayúscula, minúscula y número
        Estructuras: condicionales, bucle sobre cadena, funciones. */

/* Ejercicios con Programación Orientada a Objetos (POO) */

    /* 11. Clase Persona
        Crea una clase Persona con atributos: nombre, edad y documento. Implementa un método que indique si la persona es mayor de edad.
        Conceptos: encapsulamiento, métodos. */

    /* 12. Clase Vehículo
        Crea una clase Vehículo con atributos: marca, modelo y velocidad. Agrega métodos acelerar() y frenar() que modifiquen la velocidad.
        Conceptos: atributos, encapsulamiento. */

    /* 13. Cuenta Bancaria
        Define una clase CuentaBancaria con saldo, titular y número de cuenta. Implementa métodos depositar(), retirar() y mostrarSaldo(). Evita que el saldo sea negativo.
        Conceptos: encapsulamiento, validaciones. */

    /* 14. Clase Estudiante
        Crea una clase Estudiante con nombre, programa y lista de calificaciones. Incluye un método para calcular el promedio y otro para mostrar si aprobó.
        Conceptos: listas, métodos, condicionales. */

    /* 15. Herencia – Animales
        Crea una clase base Animal con el método hacerSonido(). Subclases Perro y Gato sobrescriben el método.
        Conceptos: herencia, polimorfismo. */

    /* 16. Inventario de Productos
        Crea una clase Producto (nombre, precio, cantidad). Crea una clase Inventario que almacene productos y permita agregar, buscar y calcular el valor total del inventario.
        Conceptos: composición, colecciones. */

    /* 17. Sistema Académico
        Crea clases Curso, Docente y Estudiante. Cada curso tiene un docente y varios estudiantes. Implementa un método que muestre el listado de estudiantes por curso.
        Conceptos: relaciones uno a muchos. */

    /* 18. Polimorfismo – Figuras Geométricas
        Crea una clase base Figura con el método calcularArea(). Subclases: Cuadrado, Círculo y Triángulo, cada una con su propio cálculo.
        Conceptos: herencia, polimorfismo. */

    /* 19. Juego de Personajes
        Crea una clase Personaje con atributos: nombre, vida y poder. Implementa un método atacar() que descuente puntos de vida a otro personaje. Simula una batalla entre dos personajes.
        Conceptos: encapsulamiento, interacción entre objetos. */


    /* 20. Reloj Digital
        Crea una clase Reloj con atributos hora, minuto y segundo. Incluye métodos para incrementar segundos y mostrar la hora formateada correctamente.
        Conceptos: encapsulamiento, validación de datos. */


/* Ejercicios de Razonamiento y Lógica Algorítmica */

    /* 21. Números amigos
        Dos números son amigos si la suma de los divisores propios del primero es igual al segundo, y viceversa (por ejemplo, 220 y 284). Solicita dos números y determina si son amigos.
        Conceptos: bucles, funciones, análisis de divisores. */

    /* 22. Caminos posibles en una cuadrícula
        Dada una cuadrícula de tamaño N x M, calcula cuántos caminos diferentes hay para ir desde la esquina superior izquierda hasta la inferior derecha, moviéndose solo hacia abajo o hacia la derecha.
        Conceptos: recursividad o combinatoria, razonamiento lógico. */

    /* 23. Simulación de un sistema de colas
        Simula una fila en una tienda o banco. Cada cliente tiene un tiempo de atención aleatorio. Muestra el tiempo total de espera y el tiempo promedio por cliente.
        Conceptos: colas, bucles, acumuladores, aleatoriedad. */

    /* 24. Ahorcado básico (juego de texto)
        Crea un programa que seleccione una palabra secreta al azar y permita al usuario adivinarla letra por letra. Finaliza cuando adivine toda la palabra o se quede sin intentos.
        Conceptos: cadenas, bucles, condicionales, control de estados. */

    /* 25. Planificador de tareas
        Crea una clase Tarea (nombre, prioridad, estado) y una clase Planificador que gestione una lista de tareas. Permite agregar, completar y listar tareas pendientes o finalizadas.
        Conceptos: POO, listas, filtrado y lógica condicional. */

}