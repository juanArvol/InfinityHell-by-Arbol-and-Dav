package Game.Enemys.Core.Contracts;

import Game.Enemys.Core.Enemy;

/**
 * Contrato para todos los componentes opcionales de un enemigo.
 *
 * Un EnemyComponent es cualquier capacidad adicional que puede
 * adjuntarse a un Enemy en tiempo de ensamblado: aura, regeneración,
 * inmunidades, invocaciones, explosión al morir, etc.
 *
 * No confundir con Game.Engine.Component — EnemyComponent es un contrato
 * específico del framework de enemigos, independiente del motor.
 *
 * ── Ciclo de vida ────────────────────────────────────────────────────────
 *   onAttach(enemy)  — llamado cuando el componente se registra en el Enemy.
 *   update(enemy)    — llamado cada frame durante el ciclo de vida del Enemy.
 *   onDetach(enemy)  — llamado cuando el componente se elimina del Enemy.
 */
public interface EnemyComponent {

    /**
     * Llamado una vez al registrar el componente en el Enemy.
     * Usar para inicialización, suscripción a eventos, etc.
     *
     * @param enemy el Enemy al que se adjunta este componente.
     */
    default void onAttach(Enemy enemy) {}

    /**
     * Actualización por frame.
     *
     * @param enemy el Enemy propietario.
     */
    void update(Enemy enemy);

    /**
     * Llamado cuando el componente se elimina del Enemy.
     * Usar para limpiar efectos, desuscribirse de eventos, etc.
     *
     * @param enemy el Enemy del que se desprende este componente.
     */
    default void onDetach(Enemy enemy) {}
}
