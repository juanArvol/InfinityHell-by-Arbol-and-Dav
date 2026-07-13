package Game.Engine.Events;

import Game.Engine.GameObjects;

/**
 * Interfaz para componentes que necesitan saber cuándo ENTRA, CONTINÚA
 * o SALE una colisión — no solo el impacto puntual.
 *
 * ── ESTADO ACTUAL: NO ACTIVA ──────────────────────────────────────────────
 *
 * Esta interfaz está declarada pero el sistema de colisiones NO la invoca.
 * Implementarla en un componente no tendrá ningún efecto observable hasta
 * que se conecte la maquinaria enter/stay/exit en CollisionsSystem.
 *
 * ── QUÉ FALTA PARA ACTIVARLA ─────────────────────────────────────────────
 *
 * CollisionsSystem necesitaría mantener por cada objeto un Set<GameObjects>
 * con los contactos activos del frame anterior. En cada frame:
 *
 *   contactosActuales = detectarTodos()
 *   paraEntrar   = contactosActuales - contactosAnteriores  → onCollisionEnter
 *   paraPermanece = intersección                             → onCollisionStay
 *   paraSalir    = contactosAnteriores - contactosActuales  → onCollisionExit
 *   contactosAnteriores = contactosActuales
 *
 * Los métodos se llamarían solo en los componentes del objeto que implementen
 * CollisionListener, verificando con instanceof antes de invocar.
 *
 * ── CANDIDATO PARA RFC FUTURO ─────────────────────────────────────────────
 *
 * Este mecanismo es útil para:
 *   - Zonas de daño continuo (lava, gas, campo de fuerza)
 *   - Triggers de área que activan scripts al entrar/salir
 *   - Detectores de suelo más refinados que el groundCheck de FASE 0
 *
 * Hasta que se implemente, usar onCollisionWith(GameObjects) en GameObjects
 * para reacciones puntuales, o GameEventBus para eventos desacoplados.
 *
 * ── USO FUTURO (cuando esté activa) ──────────────────────────────────────
 *
 *   public class DamageTrigger extends Component implements CollisionListener {
 *       {@literal @}Override
 *       public void onTriggerStay(GameObjects other) {
 *           if (other instanceof Player p) p.damage(1);
 *       }
 *   }
 */
public interface CollisionListener {

    default void onCollisionEnter(GameObjects other) {}
    default void onCollisionStay(GameObjects other)  {}
    default void onCollisionExit(GameObjects other)  {}

    default void onTriggerEnter(GameObjects other) {}
    default void onTriggerStay(GameObjects other)  {}
    default void onTriggerExit(GameObjects other)  {}
}
