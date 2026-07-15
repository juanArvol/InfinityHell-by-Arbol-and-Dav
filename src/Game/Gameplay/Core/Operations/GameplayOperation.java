package Game.Gameplay.Core.Operations;

/**
 * Consecuencia ejecutable que ocurre en el mundo cuando una condición se cumple.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * GameplayOperation responde a una sola pregunta:
 *
 *   "¿Qué acción ocurre en el mundo cuando una condición se cumple?"
 *
 * NO modifica números directamente.
 * NO resuelve propiedades.
 * NO reemplaza PropertyModifier — los modificadores calculan valores.
 * NO reemplaza GameplayEvent — los eventos son interceptables antes de ocurrir.
 *
 * GameplayOperation representa las CONSECUENCIAS del mundo: lo que ocurre
 * DESPUÉS de que un valor fue resuelto y una condición fue evaluada.
 *
 * ── DIFERENCIAS CLAVE ────────────────────────────────────────────────────
 *
 *   PropertyModifier   → calcula un valor numérico (+15 daño, ×1.5 velocidad)
 *   GameplayEvent      → interceptable antes de ocurrir (puede cancelarse)
 *   GameplayOperation  → consecuencia que SE EJECUTA (no se cancela)
 *
 * Una operación puede INTERNAMENTE disparar nuevos GameplayEvent si necesita
 * ser interceptable, pero la operación en sí no es cancelable desde fuera.
 *
 * ── DESACOPLAMIENTO TOTAL ─────────────────────────────────────────────────
 * GameplayOperation:
 *
 *   - NO conoce Player.
 *   - NO conoce Enemy.
 *   - NO conoce Bullet.
 *   - NO conoce Weapon.
 *   - Solo puede acceder a lo que OperationContext provee:
 *     GameObjects, Tags, Capabilities, Properties, Events, ModifierContext,
 *     ResolutionContext, CausalNode.
 *
 * ── EJEMPLOS CONCEPTUALES (NO IMPLEMENTADOS AQUÍ) ────────────────────────
 * Los siguientes son ejemplos de operaciones concretas que los sistemas
 * futuros implementarán. NINGUNO se implementa en el núcleo:
 *
 *   FreezeMovementOperation    → aplica PropertyModifier a Speed
 *   IgniteOperation            → añade tag STATUS_BURNING + modificador de daño
 *   DestroyEntityOperation     → elimina la entidad del mundo
 *   SpawnProjectileOperation   → crea un proyectil en la posición del origen
 *   CreateExplosionOperation   → genera un área de efecto
 *   PlayAnimationOperation     → señala al sistema de animación
 *   ApplyStatusOperation       → añade un estado alterado temporal
 *   EmitParticlesOperation     → señala al sistema de partículas
 *   PlaySoundOperation         → señala al sistema de sonido
 *
 * ── IMPLEMENTACIÓN ────────────────────────────────────────────────────────
 * GameplayOperation es una interfaz funcional. Cualquier lambda que tome
 * un OperationContext es una operación válida:
 *
 *   GameplayOperation logTemperature = ctx ->
 *       System.out.println("Temperature changed: " + ctx.getDelta());
 *
 * Para operaciones con estado o configuración, implementar como clase:
 *
 *   public final class ApplyStatusOperation implements GameplayOperation {
 *       private final GameplayTag status;
 *       public ApplyStatusOperation(GameplayTag status) { this.status = status; }
 *
 *       @Override
 *       public void execute(OperationContext ctx) {
 *           if (ctx.getTarget() == null) return;
 *           TagComponent tags = ctx.getTarget().getComponent(TagComponent.class);
 *           if (tags != null) tags.add(status);
 *       }
 *   }
 *
 * ── COMPOSICIÓN ──────────────────────────────────────────────────────────
 * Las operaciones pueden componerse con andThen() para crear secuencias:
 *
 *   GameplayOperation combined = freezeOp.andThen(playFreezeSound).andThen(emitIceParticles);
 *   combined.execute(ctx);
 *
 * @see OperationContext
 * @see OperationPredicate
 * @see OperationRegistry
 */
@FunctionalInterface
public interface GameplayOperation {

    /**
     * Ejecuta la consecuencia de mundo descrita por esta operación.
     *
     * La implementación puede:
     *   - Leer Tags y Capabilities de source/target para decidir cómo actuar.
     *   - Añadir/quitar Tags al target.
     *   - Añadir PropertyModifier al ModifierComponent del target.
     *   - Disparar nuevos GameplayEvent a través de canales propios.
     *   - Notificar a sistemas de partículas, sonido, animación, etc.
     *   - Crear nuevas entidades (proyectiles, explosiones, etc.).
     *   - Registrar CausalNode en el grafo causal del frame.
     *
     * La implementación NO debe:
     *   - Acceder a clases concretas mediante casting o instanceof.
     *   - Modificar campos internos de Player, Enemy, Bullet directamente.
     *   - Producir efectos con estado global sin pasar por los sistemas registrados.
     *
     * @param context toda la información disponible en el momento de la ejecución
     */
    void execute(OperationContext context);

    // ── Composición ───────────────────────────────────────────────────────

    /**
     * Retorna una operación compuesta: ejecuta esta operación primero,
     * luego la operación {@code next} sobre el mismo contexto.
     *
     * Si esta operación lanza una excepción, {@code next} no se ejecuta.
     * La composición no altera el contexto entre ejecuciones — ambas
     * operaciones reciben exactamente el mismo OperationContext.
     *
     * @param next operación a ejecutar después de esta
     * @return operación compuesta
     */
    default GameplayOperation andThen(GameplayOperation next) {
        if (next == null) return this;
        return ctx -> {
            this.execute(ctx);
            next.execute(ctx);
        };
    }

    // ── Operaciones predefinidas ──────────────────────────────────────────

    /**
     * Operación nula: no hace nada.
     * Útil como valor por defecto y en composiciones condicionales.
     */
    GameplayOperation NO_OP = ctx -> {};
}
