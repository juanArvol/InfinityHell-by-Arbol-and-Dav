package Game.Engine;

/**
 * Pieza de comportamiento que se agrega a un GameObjects.
 *
 * ── HRFC — Unified DeltaTime Migration & Temporal Model Completion ────────
 *
 * Ciclo de vida:
 *   start()  → se llama una vez cuando se agrega al objeto (addComponent)
 *   update(deltaTime) → se llama cada frame desde GameObjects.update(deltaTime)
 *
 * MIGRACIÓN TEMPORAL:
 *   update() ahora recibe deltaTime para permitir comportamientos independientes
 *   del framerate (duraciones, cooldowns, efectos temporales).
 */
public abstract class Component {

    protected GameObjects gameObject;

    /** Llamado internamente por GameObjects.addComponent(). No llamar manualmente. */
    final void setGameObject(GameObjects obj) {
        this.gameObject = obj;
    }

    /**
     * Retorna el GameObject propietario de este componente.
     * @return el GameObject al que pertenece este componente
     */
    public GameObjects getOwner() {
        return gameObject;
    }

    /** Inicialización. Se llama una vez al agregarse al objeto. */
    public void start() {}

    /**
     * Lógica por frame.
     *
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * @param deltaTime tiempo del simulation step en segundos
     */
    public void update(double deltaTime) {}
}
