package Game.Engine;

/**
 * Pieza de comportamiento que se agrega a un GameObjects.
 *
 * Ciclo de vida:
 *   start()  → se llama una vez cuando se agrega al objeto (addComponent)
 *   update() → se llama cada frame desde GameObjects.update()
 */
public abstract class Component {

    protected GameObjects gameObject;

    /** Llamado internamente por GameObjects.addComponent(). No llamar manualmente. */
    final void setGameObject(GameObjects obj) {
        this.gameObject = obj;
    }

    /** Inicialización. Se llama una vez al agregarse al objeto. */
    public void start() {}

    /** Lógica por frame. */
    public void update() {}
}
