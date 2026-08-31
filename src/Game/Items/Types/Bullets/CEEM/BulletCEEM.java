package Game.Items.Types.Bullets.CEEM;

import Game.Engine.Camera.GameCamera;
import Game.Items.Creation.ItemRarity;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.OffScreenTracker;

/**
 * CEEM especializado para el ciclo de vida espacial de proyectiles.
 * 
 * ── HRFC — CEEM Especializado para Bullets ───────────────────────────────
 * 
 * OBJETIVO:
 *   Determinar cuándo un proyectil fuera del área activa deja de justificar
 *   su permanencia en runtime, permitiendo devolverlo oportunamente al
 *   ProjectilePool y reducir el coste de simulación/renderizado durante
 *   escenarios de alta densidad de proyectiles.
 * 
 * RESTRICCIÓN FUNDAMENTAL:
 *   El CEEM NO gestiona ni duplica bulletLifeTime.
 *   bulletLifeTime (BulletLife) continúa siendo la autoridad absoluta del
 *   tiempo de vida intrínseco del proyectil.
 * 
 * RESPONSABILIDAD:
 *   Responde exclusivamente a:
 *     "La bullet está fuera del área activa. ¿Tiene sentido mantenerla
 *      viva y simulándose durante más tiempo?"
 * 
 * NO DECIDE:
 *   × cómo se mueve la bullet
 *   × cómo colisiona
 *   × cómo aplica daño
 *   × cómo funciona su comportamiento
 *   × cuándo termina su bulletLifeTime natural
 *   × cómo se instancia
 *   × cómo se recicla físicamente el objeto
 * 
 * ENTRADAS DEL MODELO:
 * 
 *   velocityFinal (runtime)
 *     Velocidad efectiva que posee el proyectil durante runtime.
 *     Usa la velocidad final/runtime real, no una velocidad estática.
 *     Las bullets pueden recibir modificaciones durante construcción/runtime.
 * 
 *   rarityWeight
 *     Peso numérico asociado a la rareza del bullet (ItemRarity.weight).
 *     Obtenido desde BulletRegistry vía BulletID del behavior.
 *     Funciona como factor de prioridad/persistencia base.
 *     Proyectiles de mayor relevancia toleran permanencia exterior mayor.
 * 
 *   Información espacial
 *     Relación espacial respecto al área activa (viewport).
 *     Usa OffScreenTracker existente: isOffScreen(), offScreenTime.
 * 
 *   timeOutsideActiveRegion
 *     Tiempo acumulado en segundos durante el cual la bullet permanece
 *     fuera del área activa. Provisto por OffScreenTracker.
 * 
 *   timeAlive
 *     Tiempo real que la bullet lleva activa.
 *     Usado como contexto para evitar decisiones prematuras.
 * 
 *   distanceFromViewport (NUEVO)
 *     Distancia aproximada desde el edge del viewport.
 *     Bullets muy lejos tienen menor prioridad de persistencia.
 * 
 *   bulletSize (NUEVO)
 *     Tamaño del collider (width × height).
 *     Bullets grandes son más relevantes visualmente.
 * 
 * PERSISTENCE BUDGET:
 * 
 *   El mecanismo central es un presupuesto dinámico de persistencia espacial:
 * 
 *     persistenceBudget = baseBudget × velocityFactor × rarityFactor × sizeFactor
 * 
 *   Los factores están diseñados para evitar comportamientos extremos:
 * 
 *   velocityFactor
 *     La velocidad final influye en cuánto tiempo tiene sentido conservar
 *     una bullet fuera del área activa. Una bullet extremadamente rápida
 *     puede recorrer distancia considerable en poco tiempo.
 *     Usa función amortiguada (sqrt) para evitar crecimiento ilimitado.
 * 
 *   rarityFactor
 *     Usa directamente ItemRarity.weight (50-1 para COMMON-LEGENDARY).
 *     Obtenido desde BulletRegistry mediante BulletID del behavior.
 *     Incrementa tolerancia/persistencia de forma controlada.
 *     Normalizado respecto al rango real de ItemRarity.
 * 
 *   sizeFactor
 *     Bullets grandes (proyectiles masivos, meteoritos) son más relevantes
 *     visualmente y mecánicamente. Reciben un pequeño bonus de persistencia.
 *     Normalizado respecto a tamaño estándar (8×8px).
 * 
 * SPATIAL PRESSURE:
 * 
 *   La permanencia exterior no depende únicamente del tiempo.
 *   Incorpora la distancia de la bullet respecto al área activa:
 * 
 *     distanceOutside + velocityFinal → spatialPressure
 * 
 *   Una bullet apenas fuera del área activa vs extremadamente lejos
 *   no reciben el mismo tratamiento.
 * 
 *   Métrica heurística:
 *     spatialPressure = (distanceOutside / referenceDistance) × distanceWeight
 * 
 *   Si la bullet está muy lejos (>2× referenceDistance), se aplica penalización
 *   adicional al presupuesto para acelerar su expiración.
 * 
 *   Heurística de persistencia, NO predicción exacta de trayectoria.
 *   No asume movimiento lineal.
 * 
 * GRACE STATE:
 * 
 *   Fase de tolerancia cuando una bullet abandona el área activa.
 * 
 *   Estados conceptuales:
 *     ACTIVE  → dentro del área activa, sin presión de expiración espacial
 *     GRACE   → fuera del área activa, continúa viva mientras sea razonable
 *     EXPIRE  → excedió presupuesto espacial, debe cesar persistencia
 * 
 * MINIMUM LIFETIME:
 * 
 *   Protección contra reciclaje prematuro:
 * 
 *     spawn → sale inmediatamente → NO expire inmediato
 * 
 *   Especialmente importante en proyectiles extremadamente rápidos.
 *   minimumLifetime es parte de la política del CEEM, NO un segundo lifetime.
 *   Derivado/configurado mediante el modelo del CEEM.
 * 
 *   GARANTÍA: minimumLifetime < bulletLifeTime
 * 
 * INTEGRACIÓN CON bulletLifeTime:
 * 
 *   El CEEM respeta completamente bulletLifeTime:
 * 
 *     bulletLifeTime        → límite natural de existencia
 *     Bullet CEEM           → límite de persistencia espacial
 * 
 *   Son responsabilidades DIFERENTES.
 * 
 *   Ejemplo:
 *     Bullet dentro de ActiveRegion → vive normalmente
 *     bulletLifeTime alcanza límite → fin normal de vida
 * 
 *   vs:
 *     Bullet sale de ActiveRegion → entra en GRACE
 *     CEEM evalúa persistencia espacial
 *     presupuesto excedido → decisión EXPIRE
 * 
 *   El CEEM NO modifica ni redefine bulletLifeTime.
 * 
 * DECISIÓN, NO EJECUCIÓN:
 * 
 *   El CEEM devuelve una decisión (BulletCEEMDecision), no ejecuta pooling:
 * 
 *     BulletCEEM → BulletCEEMDecision → Bullet lifecycle → ProjectilePool
 * 
 *   Evita acoplar el modelo de decisión al mecanismo de reciclaje.
 * 
 * OPTIMIZACIÓN DE RUNTIME:
 * 
 *   Objetivo: optimización real bajo alta densidad de proyectiles.
 *   Evitar mantener indefinidamente proyectiles que:
 *     - están muy lejos del área activa
 *     - llevan demasiado tiempo fuera
 *     - tienen baja prioridad/persistencia
 *     - tienen pocas posibilidades razonables de volver a ser relevantes
 * 
 *   Política especialmente efectiva en escenarios Bullet Hell (cientos/miles
 *   de proyectiles simultáneos).
 * 
 *   NO optimización destructiva indiscriminada.
 *   Prioriza: relevancia espacial + coste de persistencia + características
 *   del proyectil antes de decidir expiración espacial.
 * 
 * CONFIGURACIÓN:
 * 
 *   Valores base del CEEM centralizados y configurables.
 *   NO hardcoding por BulletType.
 *   NO valores dispersos dentro de las bullets.
 *   Política derivada del modelo común.
 */
public final class BulletCEEM {
    
    // ── Configuración del modelo ──────────────────────────────────────────
    
    /**
     * Presupuesto base de persistencia fuera del área activa (segundos).
     * 
     * Este es el tiempo base que una bullet COMMON con velocidad moderada
     * y tamaño estándar puede permanecer fuera del viewport antes de ser
     * considerada para expiración espacial.
     * 
     * Los factores de velocidad, rareza y tamaño modulan este valor.
     * 
     * Valor por defecto: 3.0 segundos (razonable para bullets típicas).
     */
    private final double baseBudget;
    
    /**
     * Velocidad de referencia para normalización (units/s).
     * 
     * Velocidad típica de una bullet estándar. Usada para normalizar el
     * factor de velocidad y evitar que bullets muy rápidas obtengan
     * presupuestos absurdos.
     * 
     * Valor por defecto: 300.0 units/s (velocidad moderada).
     */
    private final double referenceVelocity;
    
    /**
     * Distancia de referencia desde viewport (píxeles).
     * 
     * Distancia típica fuera del viewport. Usada para calcular presión espacial.
     * Bullets más lejos que esta distancia reciben penalización.
     * 
     * Valor por defecto: 200.0 píxeles (viewport típico ~640×480).
     */
    private final double referenceDistance;
    
    /**
     * Lifetime mínimo absoluto antes de permitir expiración espacial (segundos).
     * 
     * Protección contra spawn → sale viewport → expire inmediato.
     * Una bullet debe vivir al menos este tiempo antes de que el CEEM
     * pueda decidir EXPIRE, independientemente de su posición.
     * 
     * Valor por defecto: 0.1 segundos (protección contra expiración instantánea).
     */
    private final double minimumLifetimeProtection;
    
    /**
     * Peso máximo de rareza para normalización.
     * 
     * Usamos COMMON.weight como referencia alta (50).
     * Rarezas mayores (menor weight numérico) obtienen bonus de persistencia.
     */
    private static final double MAX_RARITY_WEIGHT = 50.0; // ItemRarity.COMMON
    
    /**
     * Tamaño de referencia para normalización (píxeles).
     * 
     * Tamaño estándar de una bullet típica (8×8px).
     */
    private static final double REFERENCE_SIZE = 64.0; // 8×8 = 64 px²
    
    // ── Constructor ───────────────────────────────────────────────────────
    
    /**
     * Crea un BulletCEEM con configuración por defecto.
     * 
     * Configuración por defecto:
     *   - baseBudget: 3.0 segundos
     *   - referenceVelocity: 300.0 units/s
     *   - referenceDistance: 200.0 píxeles
     *   - minimumLifetimeProtection: 0.1 segundos
     */
    public BulletCEEM() {
        this(3.0, 300.0, 200.0, 0.1);
    }
    
    /**
     * Crea un BulletCEEM con configuración personalizada.
     * 
     * @param baseBudget presupuesto base en segundos (debe ser > 0)
     * @param referenceVelocity velocidad de referencia en units/s (debe ser > 0)
     * @param referenceDistance distancia de referencia en píxeles (debe ser > 0)
     * @param minimumLifetimeProtection lifetime mínimo en segundos (debe ser >= 0)
     */
    public BulletCEEM(double baseBudget,
                      double referenceVelocity,
                      double referenceDistance,
                      double minimumLifetimeProtection) {
        if (baseBudget <= 0.0) {
            throw new IllegalArgumentException("baseBudget must be positive");
        }
        if (referenceVelocity <= 0.0) {
            throw new IllegalArgumentException("referenceVelocity must be positive");
        }
        if (referenceDistance <= 0.0) {
            throw new IllegalArgumentException("referenceDistance must be positive");
        }
        if (minimumLifetimeProtection < 0.0) {
            throw new IllegalArgumentException("minimumLifetimeProtection cannot be negative");
        }
        
        this.baseBudget = baseBudget;
        this.referenceVelocity = referenceVelocity;
        this.referenceDistance = referenceDistance;
        this.minimumLifetimeProtection = minimumLifetimeProtection;
    }
    
    // ── Evaluación ────────────────────────────────────────────────────────
    
    /**
     * Evalúa la persistencia espacial de un proyectil.
     * 
     * PRECONDICIÓN:
     *   - bullet debe tener OffScreenTracker configurado (non-null)
     *   - bullet debe tener flyweight con rareza válida
     * 
     * POSTCONDICIÓN:
     *   - Retorna BulletCEEMDecision con estado ACTIVE, GRACE, o EXPIRE
     *   - NO modifica el estado del bullet
     *   - NO invoca bullet.getBulletLife().kill()
     * 
     * @param bullet el proyectil a evaluar
     * @param camera cámara activa del juego
     * @param deltaTime tiempo transcurrido (usado para contexto, no para mutación)
     * @return decisión inmutable sobre persistencia espacial
     */
    public BulletCEEMDecision evaluate(Bullet bullet, GameCamera camera, double deltaTime) {
        OffScreenTracker tracker = bullet.getOffScreenTracker();
        
        // Si no hay tracker configurado, el bullet no participa en CEEM espacial
        if (tracker == null) {
            return new BulletCEEMDecision(
                BulletSpatialState.ACTIVE,
                "No spatial tracking configured"
            );
        }
        
        // Si está dentro del área activa → ACTIVE
        if (!tracker.isOffScreen()) {
            return new BulletCEEMDecision(
                BulletSpatialState.ACTIVE,
                "Inside active region"
            );
        }
        
        // ── Protección de lifetime mínimo ─────────────────────────────────
        
        // timeAlive = tiempo total desde spawn
        // Calculamos cuánto tiempo ha vivido basándonos en el BulletLife
        double timeAlive = calculateTimeAlive(bullet);
        
        if (timeAlive < minimumLifetimeProtection) {
            return new BulletCEEMDecision(
                BulletSpatialState.GRACE,
                String.format("Minimum lifetime protection (alive=%.2fs < min=%.2fs)",
                             timeAlive, minimumLifetimeProtection)
            );
        }
        
        // ── Extraer entradas del modelo ───────────────────────────────────
        
        // Velocidad final runtime
        double vx = bullet.getPhysics().getXspeed();
        double vy = bullet.getPhysics().getYspeed();
        double velocityFinal = Math.sqrt(vx * vx + vy * vy);
        
        // Rareza del tipo de bullet — ahora expuesta directamente por Bullet
        ItemRarity rarity = bullet.getRarity();
        int rarityWeight = rarity.weight;
        
        // Tamaño del bullet (área del collider)
        int width = bullet.getFlyweight().width();
        int height = bullet.getFlyweight().height();
        double bulletSize = width * height;
        
        // Tiempo fuera del área activa
        double timeOutside = tracker.getOffScreenTime();
        
        // Distancia aproximada desde viewport (usando posición del bullet)
        double distanceOutside = estimateDistanceFromViewport(bullet, camera);
        
        // ── Calcular presupuesto de persistencia ──────────────────────────
        
        double persistenceBudget = calculatePersistenceBudget(
            velocityFinal,
            rarityWeight,
            bulletSize
        );
        
        // ── Aplicar presión espacial ──────────────────────────────────────
        
        double spatialPressure = calculateSpatialPressure(distanceOutside);
        persistenceBudget *= spatialPressure; // Reducir presupuesto si está muy lejos
        
        // ── Evaluar presión espacial ──────────────────────────────────────
        
        // Si el tiempo fuera excede el presupuesto → EXPIRE
        if (timeOutside > persistenceBudget) {
            String diagnostic = String.format(
                "Exceeded spatial budget: outside=%.2fs > budget=%.2fs " +
                "(velocity=%.1f u/s, rarity=%s[%d], size=%.0fpx², distance=%.0fpx)",
                timeOutside, persistenceBudget,
                velocityFinal, rarity.name(), rarityWeight, bulletSize, distanceOutside
            );
            return new BulletCEEMDecision(BulletSpatialState.EXPIRE, diagnostic);
        }
        
        // ── Aún dentro del presupuesto → GRACE ────────────────────────────
        
        String diagnostic = String.format(
            "Outside %.2fs, budget %.2fs (velocity=%.1f u/s, rarity=%s[%d], size=%.0fpx², distance=%.0fpx)",
            timeOutside, persistenceBudget,
            velocityFinal, rarity.name(), rarityWeight, bulletSize, distanceOutside
        );
        return new BulletCEEMDecision(BulletSpatialState.GRACE, diagnostic);
    }
    
    // ── Cálculo de presupuesto ────────────────────────────────────────────
    
    /**
     * Calcula el presupuesto de persistencia espacial dinámicamente.
     * 
     * Fórmula:
     *   persistenceBudget = baseBudget × velocityFactor × rarityFactor × sizeFactor
     * 
     * velocityFactor:
     *   Usa función amortiguada (sqrt) para evitar crecimiento ilimitado.
     *   Bullets rápidas obtienen presupuesto mayor, pero no lineal.
     * 
     * rarityFactor:
     *   COMMON (weight=50) → factor ≈ 1.0 (base)
     *   UNCOMMON (30)      → factor ≈ 1.67
     *   RARE (15)          → factor ≈ 3.33
     *   EPIC (4)           → factor ≈ 12.5
     *   LEGENDARY (1)      → factor ≈ 50.0
     * 
     * sizeFactor:
     *   Bullets grandes reciben pequeño bonus.
     *   8×8px (64px²) → factor = 1.0 (base)
     *   16×16px (256px²) → factor ≈ 1.2
     * 
     * @param velocityFinal velocidad runtime en units/s
     * @param rarityWeight peso de ItemRarity (50-1)
     * @param bulletSize área del collider (width × height)
     * @return presupuesto en segundos
     */
    private double calculatePersistenceBudget(double velocityFinal, int rarityWeight, double bulletSize) {
        // ── Velocity Factor (amortiguado) ─────────────────────────────────
        
        // Normalizar velocidad respecto a referencia
        double velocityRatio = velocityFinal / referenceVelocity;
        
        // Usar sqrt para amortiguación: velocity × 4 → factor × 2
        // Evita que bullets muy rápidas obtengan presupuestos absurdos
        double velocityFactor = Math.sqrt(velocityRatio);
        
        // Clampear a un rango razonable [0.5, 3.0]
        velocityFactor = Math.max(0.5, Math.min(3.0, velocityFactor));
        
        // ── Rarity Factor (normalizado) ───────────────────────────────────
        
        // Invertir el peso: COMMON=50 es base, LEGENDARY=1 es máximo bonus
        // rarityFactor = MAX_RARITY_WEIGHT / rarityWeight
        double rarityFactor = MAX_RARITY_WEIGHT / Math.max(1.0, rarityWeight);
        
        // Clampear rarity factor para evitar extremos con rarezas custom
        rarityFactor = Math.min(50.0, rarityFactor);
        
        // ── Size Factor (normalizado) ─────────────────────────────────────
        
        // Bullets grandes (meteoritos, proyectiles masivos) reciben bonus
        double sizeRatio = bulletSize / REFERENCE_SIZE;
        double sizeFactor = 1.0 + (Math.sqrt(sizeRatio) - 1.0) * 0.2; // bonus moderado
        
        // Clampear a rango razonable [1.0, 1.5]
        sizeFactor = Math.max(1.0, Math.min(1.5, sizeFactor));
        
        // ── Presupuesto final ─────────────────────────────────────────────
        
        return baseBudget * velocityFactor * rarityFactor * sizeFactor;
    }
    
    /**
     * Calcula la presión espacial basada en distancia desde viewport.
     * 
     * Bullets muy lejos reciben penalización para acelerar su expiración.
     * 
     * @param distanceOutside distancia desde viewport en píxeles
     * @return factor multiplicador del presupuesto [0.5, 1.0]
     */
    private double calculateSpatialPressure(double distanceOutside) {
        // Normalizar distancia
        double distanceRatio = distanceOutside / referenceDistance;
        
        if (distanceRatio < 1.0) {
            // Cerca del viewport: sin penalización
            return 1.0;
        } else if (distanceRatio < 2.0) {
            // Moderadamente lejos: penalización suave
            return 1.0 - (distanceRatio - 1.0) * 0.25; // [1.0, 0.75]
        } else {
            // Muy lejos: penalización fuerte
            return 0.5; // reducir presupuesto a la mitad
        }
    }
    
    /**
     * Estima la distancia aproximada desde el edge del viewport.
     * 
     * Si el bullet está dentro: retorna 0
     * Si está fuera: retorna distancia aproximada al edge más cercano
     * 
     * @param bullet el proyectil
     * @param camera cámara activa
     * @return distancia en píxeles
     */
    private double estimateDistanceFromViewport(Bullet bullet, GameCamera camera) {
        // Usar getPositionX/Y() directo — sin allocation
        double bulletX = bullet.getPositionX();
        double bulletY = bullet.getPositionY();
        
        double cameraX = camera.getX();
        double cameraY = camera.getY();
        double cameraWidth = camera.getVirtualWidth();
        double cameraHeight = camera.getVirtualHeight();
        
        // Calcular distancia a cada edge
        double distanceLeft = cameraX - bulletX;
        double distanceRight = bulletX - (cameraX + cameraWidth);
        double distanceTop = cameraY - bulletY;
        double distanceBottom = bulletY - (cameraY + cameraHeight);
        
        // Si está dentro en ambos ejes: distancia = 0
        boolean insideX = (distanceLeft < 0 && distanceRight < 0);
        boolean insideY = (distanceTop < 0 && distanceBottom < 0);
        
        if (insideX && insideY) {
            return 0.0;
        }
        
        // Calcular distancia al edge más cercano
        double distanceX = 0.0;
        if (distanceLeft > 0) distanceX = distanceLeft;
        else if (distanceRight > 0) distanceX = distanceRight;
        
        double distanceY = 0.0;
        if (distanceTop > 0) distanceY = distanceTop;
        else if (distanceBottom > 0) distanceY = distanceBottom;
        
        // Distancia euclidiana si está fuera en ambos ejes, lineal si solo en uno
        if (distanceX > 0 && distanceY > 0) {
            return Math.sqrt(distanceX * distanceX + distanceY * distanceY);
        } else {
            return Math.max(distanceX, distanceY);
        }
    }
    
    // ── Helpers privados ──────────────────────────────────────────────────
    
    /**
     * Calcula el tiempo que lleva vivo el bullet.
     * 
     * ARQUITECTURA:
     *   No podemos acceder directamente al lifeTime original porque no está
     *   almacenado en Bullet (solo remaining está en BulletLife).
     *   
     *   Usamos una heurística: si el bullet está vivo y no ha sido recién
     *   spawneado, asumimos que ha vivido un tiempo mínimo razonable.
     *   
     *   Esta heurística es conservadora: evita marcar bullets recién spawneados
     *   como EXPIRE, que es el propósito del minimumLifetimeProtection.
     * 
     * @param bullet el proyectil
     * @return tiempo estimado vivo en segundos
     */
    private double calculateTimeAlive(Bullet bullet) {
        // Heurística conservadora: usar el tiempo fuera + un epsilon
        // para evitar que bullets recién spawneados fuera de viewport
        // sean marcados EXPIRE inmediatamente.
        //
        // Si OffScreenTracker.offScreenTime > 0, el bullet ha estado
        // vivo al menos ese tiempo. Si es 0, asumimos que acaba de spawnear.
        OffScreenTracker tracker = bullet.getOffScreenTracker();
        if (tracker != null && tracker.isOffScreen()) {
            return tracker.getOffScreenTime();
        }
        
        // Fallback: retornar 0 para bullets dentro de viewport o sin tracker
        // El minimumLifetimeProtection protegerá contra expiración inmediata
        return 0.0;
    }
    
    // ── Getters de configuración ──────────────────────────────────────────
    
    public double getBaseBudget() {
        return baseBudget;
    }
    
    public double getReferenceVelocity() {
        return referenceVelocity;
    }
    
    public double getReferenceDistance() {
        return referenceDistance;
    }
    
    public double getMinimumLifetimeProtection() {
        return minimumLifetimeProtection;
    }
}
