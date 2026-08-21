package Main;

/**
 * Contexto temporal unificado del simulation step.
 *
 * ── HRFC-DT-002 — Unified Temporal Context ───────────────────────────────
 *
 * CONTRATO:
 *
 * TemporalContext es la abstracción canónica del tiempo del simulation step.
 * Transporta el deltaTime calculado por GameLoop a través de toda la
 * jerarquía de sistemas sin que ningún componente tenga que obtenerlo,
 * reconstruirlo o solicitarlo desde fuentes diferentes.
 *
 * FLUJO:
 *
 *   GameLoop
 *     ↓ (produce)
 *   TemporalContext (instancia única por step)
 *     ↓ (propaga)
 *   State → Systems → Subsystems
 *
 * GARANTÍAS:
 *
 * 1. GameLoop es el ÚNICO PRODUCTOR del contexto temporal.
 * 2. El contexto NO mide ni recalcula tiempo internamente.
 * 3. Una sola instancia por simulation step representa el mismo tiempo
 *    para todos los sistemas ejecutados durante ese step.
 * 4. Los sistemas NO pueden consultar directamente el reloj ni calcular
 *    su propio deltaTime.
 *
 * IDENTIDAD:
 *
 * Cada TemporalContext tiene un stepId único que permite verificar
 * mediante instrumentación que todos los consumidores están utilizando
 * exactamente el mismo contexto temporal durante un simulation step.
 *
 * UNIDADES:
 *
 * simulationDeltaTime está expresado en SEGUNDOS (tiempo real transcurrido).
 *
 * INMUTABILIDAD:
 *
 * TemporalContext es inmutable. Una vez creado, su deltaTime no puede
 * ser modificado. Los sistemas lo reciben como parámetro de solo lectura.
 *
 * ── Verificación de identidad ─────────────────────────────────────────────
 *
 * El método verifyIdentity() permite instrumentar el flujo temporal y
 * demostrar que:
 *
 *   GameLoop.update(ctx)
 *     → GameState.update(ctx)      [mismo stepId]
 *       → WorldManager.update(ctx)  [mismo stepId]
 *         → System A.update(ctx)    [mismo stepId]
 *         → System B.update(ctx)    [mismo stepId]
 *
 * Ningún consumidor está generando un contexto alternativo.
 */
public final class TemporalContext {

    /**
     * Identificador único del simulation step.
     *
     * Permite verificar por identidad que múltiples consumidores
     * están utilizando exactamente el mismo contexto temporal.
     *
     * Incrementa monótonamente: step N, step N+1, step N+2, ...
     */
    private final long stepId;

    /**
     * Tiempo real del simulation step en SEGUNDOS.
     *
     * Calculado por GameLoop como:
     *   deltaTime = (System.nanoTime() - lastTime) / 1_000_000_000.0
     *
     * Representa el tiempo real transcurrido desde el simulation step
     * anterior. Puede variar entre frames (real deltaTime, no fixed timestep).
     *
     * EJEMPLOS (60 FPS target):
     *   Frame rápido:  0.008s (8ms)
     *   Frame normal:  0.016s (16ms)
     *   Frame lento:   0.050s (50ms)
     *   Lag spike:     0.083s (clamped a ~5 frames)
     *
     * UNIDADES CANÓNICAS DE INTEGRACIÓN:
     *   Δv = a × simulationDeltaTime
     *   Δx = v × simulationDeltaTime
     */
    private final double simulationDeltaTime;

    /**
     * Timestamp de creación del contexto (nanosegundos).
     *
     * Solo para diagnóstico y verificación. NO debe usarse para
     * cálculos de gameplay — todo el gameplay debe derivarse de
     * simulationDeltaTime.
     */
    private final long creationTimestamp;

    // ── Constructor privado ───────────────────────────────────────────────

    /**
     * Constructor privado — solo GameLoop puede crear instancias
     * a través del método estático of().
     */
    private TemporalContext(long stepId, double simulationDeltaTime) {
        this.stepId = stepId;
        this.simulationDeltaTime = simulationDeltaTime;
        this.creationTimestamp = System.nanoTime();
    }

    // ── Factory method (package-private + Debug access) ──────────────────

    /**
     * Crea un nuevo contexto temporal para el simulation step dado.
     *
     * RESTRICCIÓN:
     * Package-private — solo GameLoop (package Main) puede llamar a este método.
     * Los sistemas reciben el contexto como parámetro, no lo construyen.
     *
     * EXCEPCIÓN:
     * Main.Debug.TemporalContextVerifier necesita crear contextos para testing.
     * El acceso está permitido SOLO para testing/verificación.
     *
     * @param stepId identificador único del simulation step (monótono creciente)
     * @param simulationDeltaTime tiempo real del step en segundos
     * @return contexto temporal inmutable
     */
    public static TemporalContext of(long stepId, double simulationDeltaTime) {
        // Verificar que el caller es GameLoop o Debug
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        boolean authorized = false;
        
        for (StackTraceElement element : stack) {
            String className = element.getClassName();
            if (className.equals("Main.GameLoop") || 
                className.startsWith("Main.Debug.")) {
                authorized = true;
                break;
            }
        }
        
        if (!authorized) {
            throw new IllegalCallerException(
                "TemporalContext.of() solo puede ser llamado desde GameLoop o Main.Debug.*"
            );
        }

        if (simulationDeltaTime < 0.0) {
            throw new IllegalArgumentException(
                "simulationDeltaTime no puede ser negativo: " + simulationDeltaTime
            );
        }
        return new TemporalContext(stepId, simulationDeltaTime);
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    /**
     * Retorna el identificador único del simulation step.
     *
     * Permite verificar por identidad que múltiples consumidores
     * están usando el mismo contexto temporal.
     */
    public long getStepId() {
        return stepId;
    }

    /**
     * Retorna el tiempo del simulation step en SEGUNDOS.
     *
     * Este es el valor canónico que debe usarse para toda integración
     * temporal en física, movimiento, cooldowns, efectos temporales, etc.
     *
     * INVARIANTE:
     * Este valor NO debe ser recalculado, modificado ni sustituido por
     * ningún sistema. Representa la autoridad temporal única del step.
     */
    public double getDeltaTime() {
        return simulationDeltaTime;
    }

    /**
     * Retorna el timestamp de creación del contexto (nanosegundos).
     *
     * Solo para diagnóstico — NO usar para cálculos de gameplay.
     */
    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    // ── Verificación de identidad ─────────────────────────────────────────

    /**
     * Verifica que dos referencias de contexto sean LA MISMA INSTANCIA.
     *
     * Útil para instrumentación y debugging:
     *
     *   TemporalContext ctx = ...; // recibido en update()
     *   if (!ctx.verifyIdentity(globalContextSnapshot)) {
     *       log.warn("Sistema recibió contexto diferente al esperado");
     *   }
     *
     * @param other contexto a comparar
     * @return true si ambos son la misma instancia (this == other)
     */
    public boolean verifyIdentity(TemporalContext other) {
        return this == other;
    }

    /**
     * Verifica que el stepId coincida con el esperado.
     *
     * Útil para instrumentación menos estricta que verifyIdentity():
     *
     *   if (!ctx.verifyStepId(expectedStepId)) {
     *       log.warn("Sistema recibió contexto de step diferente");
     *   }
     *
     * @param expectedStepId stepId esperado
     * @return true si los stepIds coinciden
     */
    public boolean verifyStepId(long expectedStepId) {
        return this.stepId == expectedStepId;
    }

    // ── Debug string ──────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format(
            "TemporalContext[step=%d, dt=%.6fs, created=%d]",
            stepId, simulationDeltaTime, creationTimestamp
        );
    }

    // ── Equality (por identidad, no por valor) ────────────────────────────

    /**
     * TemporalContext usa identidad de referencia, no igualdad por valor.
     *
     * Dos contextos con el mismo stepId y deltaTime NO son iguales si
     * son instancias diferentes — esto es intencional para detectar
     * contextos duplicados incorrectamente construidos.
     */
    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
