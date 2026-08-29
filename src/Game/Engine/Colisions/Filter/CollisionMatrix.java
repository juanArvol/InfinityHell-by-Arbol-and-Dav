package Game.Engine.Colisions.Filter;

/**
 * CollisionMatrix — define qué categorías de objetos pueden colisionar entre sí.
 *
 * ── HRFC — Deep Optimization: Collision Filtering ─────────────────────────
 *
 * PROPÓSITO:
 *   Evitar procesar pares de colisión que no son relevantes para gameplay.
 *   Por ejemplo: Bullet ↔ Bullet típicamente no necesita procesarse.
 *
 * ARQUITECTURA:
 *   Matriz simétrica de bits que indica si dos categorías pueden colisionar.
 *   
 *   Ejemplo:
 *                PLAYER  ENEMY  BULLET  SOLID  ITEM
 *     PLAYER       0       1      1       1      1
 *     ENEMY        1       0      1       1      0
 *     BULLET       1       1      0       1      0
 *     SOLID        1       1      1       0      0
 *     ITEM         1       0      0       0      0
 *
 * CONFIGURACIÓN:
 *   La matriz se configura una vez al inicio del juego.
 *   Puede modificarse en runtime para debugging/testing.
 *
 * INTEGRACIÓN:
 *   CollisionsSystem consulta la matriz antes de procesar cada par:
 *   
 *   if (!CollisionMatrix.canCollide(categoryA, categoryB)) {
 *       continue;  // Skip este par
 *   }
 *
 * BENEFICIOS:
 *   - Bullets no colisionan entre sí → ahorro ~N²/2 checks
 *   - Items no colisionan con enemigos → ahorro adicional
 *   - Filtrado O(1) con bitwise operations
 */
public class CollisionMatrix {

    /**
     * Categorías de colisión disponibles.
     * Cada categoría tiene un bit único para operaciones eficientes.
     */
    public enum Category {
        PLAYER(0),
        ENEMY(1),
        BULLET(2),
        SOLID(3),
        ITEM(4),
        TRIGGER(5),
        PROJECTILE(6),
        HAZARD(7);

        public final int bit;

        Category(int bit) {
            this.bit = bit;
        }

        public int mask() {
            return 1 << bit;
        }
    }

    /**
     * Matriz de colisiones representada como array de bitmasks.
     * matrix[categoryA.bit] contiene los bits de todas las categorías
     * con las que categoryA puede colisionar.
     */
    private static final int[] matrix = new int[8];

    // Flag para activar/desactivar el filtrado
    private static boolean enabled = true;

    static {
        // Configuración default de la matriz
        configureDefault();
    }

    /**
     * Configuración default de colisiones.
     * 
     * Reglas:
     *   - PLAYER colisiona con: ENEMY, BULLET, SOLID, ITEM, HAZARD
     *   - ENEMY colisiona con: PLAYER, BULLET, SOLID, HAZARD
     *   - BULLET colisiona con: PLAYER, ENEMY, SOLID, HAZARD
     *   - SOLID colisiona con: TODO (paredes, pisos)
     *   - ITEM colisiona con: PLAYER
     *   - TRIGGER colisiona con: PLAYER
     *   - PROJECTILE colisiona con: PLAYER, ENEMY, SOLID, HAZARD
     *   - HAZARD colisiona con: TODO
     *
     * IMPORTANTE: BULLET NO colisiona con BULLET
     */
    public static void configureDefault() {
        // Clear matrix
        for (int i = 0; i < matrix.length; i++) {
            matrix[i] = 0;
        }

        // PLAYER collisions
        setCollision(Category.PLAYER, Category.ENEMY, true);
        setCollision(Category.PLAYER, Category.BULLET, true);
        setCollision(Category.PLAYER, Category.SOLID, true);
        setCollision(Category.PLAYER, Category.ITEM, true);
        setCollision(Category.PLAYER, Category.TRIGGER, true);
        setCollision(Category.PLAYER, Category.PROJECTILE, true);
        setCollision(Category.PLAYER, Category.HAZARD, true);

        // ENEMY collisions
        setCollision(Category.ENEMY, Category.PLAYER, true);
        setCollision(Category.ENEMY, Category.BULLET, true);
        setCollision(Category.ENEMY, Category.SOLID, true);
        setCollision(Category.ENEMY, Category.PROJECTILE, true);
        setCollision(Category.ENEMY, Category.HAZARD, true);
        // ENEMY NO colisiona con ENEMY (optimization)
        // ENEMY NO colisiona con ITEM
        // ENEMY NO colisiona con TRIGGER

        // BULLET collisions
        setCollision(Category.BULLET, Category.PLAYER, true);
        setCollision(Category.BULLET, Category.ENEMY, true);
        setCollision(Category.BULLET, Category.SOLID, true);
        setCollision(Category.BULLET, Category.HAZARD, true);
        // BULLET NO colisiona con BULLET (OPTIMIZATION KEY)
        // BULLET NO colisiona con ITEM
        // BULLET NO colisiona con TRIGGER
        // BULLET NO colisiona con PROJECTILE

        // SOLID collisions (walls, floors)
        setCollision(Category.SOLID, Category.PLAYER, true);
        setCollision(Category.SOLID, Category.ENEMY, true);
        setCollision(Category.SOLID, Category.BULLET, true);
        setCollision(Category.SOLID, Category.PROJECTILE, true);
        setCollision(Category.SOLID, Category.HAZARD, true);
        // SOLID NO colisiona con SOLID
        // SOLID NO colisiona con ITEM
        // SOLID NO colisiona con TRIGGER

        // ITEM collisions
        setCollision(Category.ITEM, Category.PLAYER, true);
        // ITEM NO colisiona con nada más

        // TRIGGER collisions
        setCollision(Category.TRIGGER, Category.PLAYER, true);
        // TRIGGER NO colisiona con nada más

        // PROJECTILE collisions (similar a BULLET pero puede tener reglas diferentes)
        setCollision(Category.PROJECTILE, Category.PLAYER, true);
        setCollision(Category.PROJECTILE, Category.ENEMY, true);
        setCollision(Category.PROJECTILE, Category.SOLID, true);
        setCollision(Category.PROJECTILE, Category.HAZARD, true);
        // PROJECTILE NO colisiona con PROJECTILE
        // PROJECTILE NO colisiona con BULLET

        // HAZARD collisions (traps, spikes, etc.)
        setCollision(Category.HAZARD, Category.PLAYER, true);
        setCollision(Category.HAZARD, Category.ENEMY, true);
        setCollision(Category.HAZARD, Category.BULLET, true);
        setCollision(Category.HAZARD, Category.SOLID, true);
        setCollision(Category.HAZARD, Category.PROJECTILE, true);
        // HAZARD NO colisiona con HAZARD
        // HAZARD NO colisiona con ITEM
        // HAZARD NO colisiona con TRIGGER
    }

    /**
     * Configura si dos categorías pueden colisionar.
     * La matriz es simétrica: setCollision(A, B) también setea collision(B, A).
     *
     * @param a categoría A
     * @param b categoría B
     * @param canCollide true si pueden colisionar
     */
    public static void setCollision(Category a, Category b, boolean canCollide) {
        if (canCollide) {
            matrix[a.bit] |= b.mask();
            matrix[b.bit] |= a.mask();
        } else {
            matrix[a.bit] &= ~b.mask();
            matrix[b.bit] &= ~a.mask();
        }
    }

    /**
     * Verifica si dos categorías pueden colisionar según la matriz.
     *
     * @param a categoría A
     * @param b categoría B
     * @return true si pueden colisionar
     */
    public static boolean canCollide(Category a, Category b) {
        if (!enabled) {
            return true;  // Si está desactivado, permitir todas las colisiones
        }
        return (matrix[a.bit] & b.mask()) != 0;
    }

    /**
     * Activa o desactiva el filtrado de colisiones.
     * Si está desactivado, canCollide() siempre retorna true.
     *
     * @param enabled true para activar filtrado
     */
    public static void setEnabled(boolean enabled) {
        CollisionMatrix.enabled = enabled;
    }

    /**
     * Verifica si el filtrado está activo.
     *
     * @return true si está activo
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Imprime la matriz de colisiones en formato legible.
     * Útil para debugging y verificar configuración.
     */
    public static void printMatrix() {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  COLLISION MATRIX");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("Enabled: " + enabled);
        System.out.println();

        Category[] categories = Category.values();
        
        // Header
        System.out.print("           ");
        for (Category cat : categories) {
            System.out.printf("%-8s ", cat.name().substring(0, Math.min(7, cat.name().length())));
        }
        System.out.println();
        System.out.println("─────────────────────────────────────────────────────────");

        // Rows
        for (Category a : categories) {
            System.out.printf("%-10s ", a.name());
            for (Category b : categories) {
                boolean canCollide = canCollide(a, b);
                System.out.printf("%-8s ", canCollide ? "✓" : "✗");
            }
            System.out.println();
        }
        System.out.println("═══════════════════════════════════════════════════════════");
    }

    /**
     * Retorna estadísticas de filtrado.
     *
     * @return array con: [total_pairs, filtered_pairs, percentage_filtered]
     */
    public static double[] getFilteringStats() {
        if (!enabled) {
            return new double[]{0, 0, 0};
        }

        Category[] categories = Category.values();
        int totalPairs = categories.length * categories.length;
        int filteredPairs = 0;

        for (Category a : categories) {
            for (Category b : categories) {
                if (!canCollide(a, b)) {
                    filteredPairs++;
                }
            }
        }

        double percentage = (filteredPairs * 100.0) / totalPairs;
        return new double[]{totalPairs, filteredPairs, percentage};
    }

    /**
     * Imprime estadísticas de filtrado.
     */
    public static void printStats() {
        double[] stats = getFilteringStats();
        System.out.printf("[CollisionMatrix] Enabled=%s, Filtered=%d/%d pairs (%.1f%%)%n",
            enabled, (int)stats[1], (int)stats[0], stats[2]);
    }
}
